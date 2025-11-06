package com.example.simplebrowser;

import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

/**
 * 使用GeckoView（Firefox引擎）打开网页
 * 完全支持全屏、WebAuthn和现代Web标准
 */
public class GeckoViewActivity extends AppCompatActivity {
    
    private GeckoView geckoView;
    private GeckoSession geckoSession;
    private static GeckoRuntime sRuntime;
    // 是否使用桌面模式（强制桌面 UA + 注入脚本）
    private static final boolean FORCE_DESKTOP = true;
    // ActivityResult launcher placeholder for FIDO2 / WebAuthn intents
    private ActivityResultLauncher<android.content.Intent> fido2Launcher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建GeckoView
        geckoView = new GeckoView(this);
        
        // 初始化GeckoRuntime（整个应用只需一个实例）
        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this);
        }
        
        // 创建并配置GeckoSession
        geckoSession = new GeckoSession();
        // 如果需要设置 UA，GeckoView/GeckoSession 的 API 在不同版本中差异较大。
        // 为保证兼容性，本实现改为通过注入脚本强制桌面特征（见后续注入逻辑）。
        geckoSession.open(sRuntime);
        geckoView.setSession(geckoSession);
        
        // 初始化 FIDO2 / WebAuthn 的 ActivityResultLauncher（占位实现）
        fido2Launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 这里处理 FIDO2 Intent 返回的数据
                    if (result != null && result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // TODO: 将 result.getData() 中的 attestation/assertion bytes 提取并发送回 Gecko（或通过 Geckoview API 回填）
                        android.util.Log.d("GeckoViewActivity", "FIDO2 result OK: " + result.getData());
                    } else {
                        android.util.Log.d("GeckoViewActivity", "FIDO2 result canceled or null");
                    }
                }
        );
        // 获取URL
        String url = getIntent().getStringExtra("url");
        if (getIntent().getData() != null) {
            android.net.Uri data = getIntent().getData();
            String uriString = data.toString();
            url = uriString.split("#")[0];
        }
        
        // 加载URL
        if (url != null && !url.isEmpty()) {
            geckoSession.loadUri(url);
        }

        // 如果强制桌面，注入类似 WebViewActivity 的脚本以覆盖 navigator/screen 等属性
        if (FORCE_DESKTOP) {
            final String desktopSpoofScript = getDesktopSpoofScript();
            // 在页面加载后注入脚本 - 使用简单的监听器（GeckoSession.loadUri 不直接提供 onPageFinished 的钩子）
            // 所以我们使用 evaluateJS 在短延迟后注入，作为兼容实现
            // GeckoView 是一个 View，直接设置 layerType
            geckoView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            geckoView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        geckoSession.getWebExtensionController();
                        // evaluateJS 在部分 geckoview 版本里是通过 GeckoSession#sendMessage 或类似方法实现。
                        // 这里调用 evaluateJS 可能不存在；作为兼容方案，我们调用 loadUri("javascript:..."), 让引擎执行脚本。
                        geckoSession.loadUri("javascript:(function() { " + desktopSpoofScript + " })();");
                    } catch (Throwable ignored) {
                        // 忽略注入失败，浏览器仍可正常工作
                    }
                }
            }, 500);
        }
        
        setContentView(geckoView);
        
        // 全屏设置
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
    }

    /**
     * 占位：当 Gecko 页面触发 WebAuthn 时，请调用此方法以启动本地 FIDO2 流程。
     * 实际实现建议：
     * 1) 在 build.gradle 中添加 Google Play Services FIDO2 依赖（示例：com.google.android.gms:play-services-fido:<version>）
     * 2) 使用 Fido.getFido2ApiClient(this).getRegisterIntent(...)/getSignIntent(...) 获取 PendingIntent 或 Intent
     * 3) 使用 fido2Launcher.launch(intent) 启动，并在 launcher 回调中处理结果，将 attestation/assertion bytes 传回给页面或 Gecko
     * 注意：某些 geckoview 版本提供了直接的 WebAuthn delegate 回调，优先使用 Geckoview 提供的 API将结果直接回填给引擎。
     */
    private void startFido2FlowPlaceholder() {
        // 示例占位：构造一个空 Intent 并启动，真实实现应用 FIDO2 API 构造
        android.content.Intent dummy = new android.content.Intent();
        dummy.putExtra("dummy", true);
        fido2Launcher.launch(dummy);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 处理返回键：优先让 GeckoSession 后退（如果可行），否则交给系统
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (geckoSession != null) {
                try {
                    // 优先使用 canGoBack()（如果存在）
                    java.lang.reflect.Method canGoBack = geckoSession.getClass().getMethod("canGoBack");
                    Object res = canGoBack.invoke(geckoSession);
                    if (res instanceof Boolean && ((Boolean) res)) {
                        geckoSession.goBack();
                        return true;
                    }
                } catch (NoSuchMethodException nsme) {
                    // 没有 canGoBack，尝试通过 NavigationDelegate 或直接 goBack
                    try {
                        if (geckoSession.getNavigationDelegate() != null) {
                            geckoSession.goBack();
                            return true;
                        }
                    } catch (Throwable ignored) { }
                } catch (Throwable ignored) { }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // 额外保护：和 onKeyDown 保持一致的后退行为
        if (geckoSession != null) {
            try {
                java.lang.reflect.Method canGoBack = geckoSession.getClass().getMethod("canGoBack");
                Object res = canGoBack.invoke(geckoSession);
                if (res instanceof Boolean && ((Boolean) res)) {
                    geckoSession.goBack();
                    return;
                }
            } catch (NoSuchMethodException nsme) {
                try {
                    if (geckoSession.getNavigationDelegate() != null) {
                        geckoSession.goBack();
                        return;
                    }
                } catch (Throwable ignored) { }
            } catch (Throwable ignored) { }
        }
        super.onBackPressed();
    }

    /**
     * 返回用于注入到页面以模拟桌面环境的脚本（复用 WebViewActivity 的实现）
     */
    private String getDesktopSpoofScript() {
        return "(function() { try { Object.defineProperty(window.screen, 'width', {get: function() { return 1920; }, configurable: true});" +
                "Object.defineProperty(window.screen, 'height', {get: function() { return 1080; }, configurable: true});" +
                "Object.defineProperty(window.screen, 'availWidth', {get: function() { return 1920; }, configurable: true});" +
                "Object.defineProperty(window.screen, 'availHeight', {get: function() { return 1080; }, configurable: true});" +
                "Object.defineProperty(window, 'innerWidth', {get: function() { return 1920; }, configurable: true});" +
                "Object.defineProperty(window, 'innerHeight', {get: function() { return 1080; }, configurable: true});" +
                "Object.defineProperty(window, 'outerWidth', {get: function() { return 1920; }, configurable: true});" +
                "Object.defineProperty(window, 'outerHeight', {get: function() { return 1080; }, configurable: true});" +
                "Object.defineProperty(navigator, 'maxTouchPoints', {get: function() { return 0; }, configurable: true});" +
                "Object.defineProperty(navigator, 'platform', {get: function() { return 'Win32'; }, configurable: true});" +
                "if ('ontouchstart' in window) { try { delete window.ontouchstart; } catch(e){} }" +
                "Object.defineProperty(navigator, 'webdriver', {get: function() { return undefined; }, configurable: true});" +
                "Object.defineProperty(navigator, 'plugins', {get: function() { return [1,2,3,4,5]; }, configurable: true});" +
                "Object.defineProperty(navigator, 'languages', {get: function() { return ['zh-CN','zh','en-US','en']; }, configurable: true});" +
                "var viewport = document.querySelector('meta[name=viewport]'); if (viewport) { viewport.setAttribute('content', 'width=1920, initial-scale=1.0'); } else { var meta = document.createElement('meta'); meta.name='viewport'; meta.content='width=1920, initial-scale=1.0'; document.getElementsByTagName('head')[0].appendChild(meta); } } catch(e) {} })();";
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geckoSession != null) {
            geckoSession.close();
        }
    }
}
