package com.example.simplebrowser;

import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import android.util.Base64;
import android.util.Log;
import java.util.Collections;
import com.google.android.gms.fido.Fido;
import android.app.DownloadManager;
import android.os.Environment;
import android.webkit.URLUtil;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.Toast;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
// Note: avoid direct compile-time dependency on Fido2PendingIntent type to improve
// compatibility across Play Services versions — we'll handle the returned object
// at runtime (instanceof or reflection) to extract an IntentSender.
import android.app.PendingIntent;
import androidx.activity.result.IntentSenderRequest;

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
    // ActivityResult launcher placeholder for FIDO2 / WebAuthn IntentSender
    private ActivityResultLauncher<IntentSenderRequest> fido2Launcher;
    private static final String TAG = "GeckoViewActivity";
    private volatile boolean mCanGoBack = false;
    
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

        // 内容委托：下载与长按上下文菜单
        geckoSession.setContentDelegate(new GeckoSession.ContentDelegate() {
            // 外部下载响应API当前Gecko版本不可用，使用长按“下载链接”手动触发。
            @Override
            public void onContextMenu(GeckoSession session, int screenX, int screenY,
                                      GeckoSession.ContentDelegate.ContextElement element) {
                if (element == null) return;
                java.util.ArrayList<String> options = new java.util.ArrayList<>();
                final String link = element.linkUri;
                final String src = element.srcUri;
                if (link != null && !link.isEmpty()) { options.add("复制链接"); options.add("下载链接"); }
                if (src != null && !src.isEmpty()) options.add("保存图片");
                if (options.isEmpty()) return;
                new AlertDialog.Builder(GeckoViewActivity.this)
                        .setTitle("操作")
                        .setItems(options.toArray(new String[0]), (dialog, which) -> {
                            String choice = options.get(which);
                            if ("复制链接".equals(choice) && link != null) {
                                try {
                                    ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    cb.setPrimaryClip(ClipData.newPlainText("link", link));
                                    Toast.makeText(GeckoViewActivity.this, "已复制", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) { Toast.makeText(GeckoViewActivity.this, "复制失败", Toast.LENGTH_SHORT).show(); }
                            } else if ("下载链接".equals(choice) && link != null) {
                                try {
                                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                    android.net.Uri uriL = android.net.Uri.parse(link);
                                    DownloadManager.Request reqL = new DownloadManager.Request(uriL);
                                    String fileNameL = URLUtil.guessFileName(link, null, null);
                                    reqL.setTitle(fileNameL);
                                    reqL.setDescription("正在下载");
                                    reqL.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    reqL.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileNameL);
                                    long idL = dm.enqueue(reqL);
                                    startActivity(new android.content.Intent(GeckoViewActivity.this, DownloadActivity.class).putExtra("downloadId", idL));
                                } catch (Exception e) { Toast.makeText(GeckoViewActivity.this, "下载失败", Toast.LENGTH_SHORT).show(); }
                            } else if ("保存图片".equals(choice) && src != null) {
                                try {
                                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                    android.net.Uri uri2 = android.net.Uri.parse(src);
                                    DownloadManager.Request req2 = new DownloadManager.Request(uri2);
                                    String imgName = URLUtil.guessFileName(src, null, null);
                                    req2.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    req2.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, imgName);
                                    long id2 = dm.enqueue(req2);
                                    startActivity(new android.content.Intent(GeckoViewActivity.this, DownloadActivity.class).putExtra("downloadId", id2));
                                } catch (Exception e) { Toast.makeText(GeckoViewActivity.this, "保存失败", Toast.LENGTH_SHORT).show(); }
                            }
                        })
                        .show();
            }
        });
        
        // 监听导航能力变化，更新是否可后退
        geckoSession.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onCanGoBack(GeckoSession session, boolean canGoBack) {
                mCanGoBack = canGoBack;
            }
            @Override
            public void onCanGoForward(GeckoSession session, boolean canGoForward) { /* no-op */ }
        });
        
        // 初始化 FIDO2 / WebAuthn 的 ActivityResultLauncher（使用 IntentSender）
        fido2Launcher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    // 这里处理 FIDO2 IntentSender 返回的数据
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
                        java.lang.reflect.Method eval = geckoSession.getClass().getMethod("evaluateJS", String.class);
                        eval.invoke(geckoSession, desktopSpoofScript);
                    } catch (NoSuchMethodException nsme) {
                        // evaluateJS 不存在则跳过，避免影响历史
                    } catch (Throwable ignored) {}
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
        // 占位：当前环境下不启动任何 IntentSender（避免类型不匹配）；
        // 真实环境可调用 startFido2Register/startFido2Sign
        android.util.Log.w(TAG, "startFido2FlowPlaceholder: FIDO2 launcher not invoked in placeholder mode");
    }

    /**
     * 使用 Play Services FIDO2 发起注册（Create）流程。
     * 参数说明：
     * - challengeB64: 来自服务端的 challenge（Base64 编码）
     * - rpId: relying party id（通常为域名）
     * - rpName: 显示名
     * - userIdB64: 用户 id（Base64 编码）
     * - userName: 用户名
     * 注意：页面应将上述参数通过消息桥（或其他方式）传递给原生层。
     */
    public void startFido2Register(String challengeB64, String rpId, String rpName, String userIdB64, String userName) {
        try {
            byte[] challenge = Base64.decode(challengeB64, Base64.DEFAULT);
            byte[] userId = Base64.decode(userIdB64, Base64.DEFAULT);

            PublicKeyCredentialRpEntity rp = new PublicKeyCredentialRpEntity(rpId, rpName, null);
            PublicKeyCredentialUserEntity user = new PublicKeyCredentialUserEntity(userId, userName, userName, null);
            PublicKeyCredentialParameters params = new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY.toString(), -7); // ES256

            PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions.Builder()
                    .setRp(rp)
                    .setUser(user)
                    .setChallenge(challenge)
                    .setParameters(Collections.singletonList(params))
                    .build();

            Fido.getFido2ApiClient(this)
                    .getRegisterIntent(options)
                    .addOnSuccessListener(new OnSuccessListener<Object>() {
                        @Override
                        public void onSuccess(Object pending) {
                            try {
                                android.content.IntentSender sender = null;
                                if (pending == null) {
                                    Log.w(TAG, "FIDO2 pending is null");
                                } else if (pending instanceof PendingIntent) {
                                    sender = ((PendingIntent) pending).getIntentSender();
                                } else {
                                    // Try reflection: some Play Services versions return Fido2PendingIntent
                                    try {
                                        java.lang.reflect.Method m = pending.getClass().getMethod("getIntentSender");
                                        Object o = m.invoke(pending);
                                        if (o instanceof android.content.IntentSender) {
                                            sender = (android.content.IntentSender) o;
                                        }
                                    } catch (NoSuchMethodException nsme) {
                                        Log.w(TAG, "pending object has no getIntentSender method");
                                    }
                                }
                                if (sender != null) {
                                    IntentSenderRequest req = new IntentSenderRequest.Builder(sender).build();
                                    fido2Launcher.launch(req);
                                } else {
                                    Log.w(TAG, "FIDO2 pendingIntent has no IntentSender");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to launch FIDO2 pending intent", e);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "getRegisterIntent failed", e);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "startFido2Register error", e);
        }
    }

    /**
     * 使用 Play Services FIDO2 发起认证（Get/Sign）流程。
     * - challengeB64: Base64 编码的 challenge
     * - rpId: relying party id
     */
    public void startFido2Sign(String challengeB64, String rpId) {
        try {
            byte[] challenge = Base64.decode(challengeB64, Base64.DEFAULT);

            PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions.Builder()
                    .setChallenge(challenge)
                    .setRpId(rpId)
                    .build();

            Fido.getFido2ApiClient(this)
                    .getSignIntent(options)
                    .addOnSuccessListener(new OnSuccessListener<Object>() {
                        @Override
                        public void onSuccess(Object pending) {
                            try {
                                android.content.IntentSender sender = null;
                                if (pending == null) {
                                    Log.w(TAG, "FIDO2 pending is null");
                                } else if (pending instanceof PendingIntent) {
                                    sender = ((PendingIntent) pending).getIntentSender();
                                } else {
                                    try {
                                        java.lang.reflect.Method m = pending.getClass().getMethod("getIntentSender");
                                        Object o = m.invoke(pending);
                                        if (o instanceof android.content.IntentSender) {
                                            sender = (android.content.IntentSender) o;
                                        }
                                    } catch (NoSuchMethodException nsme) {
                                        Log.w(TAG, "pending object has no getIntentSender method");
                                    }
                                }
                                if (sender != null) {
                                    IntentSenderRequest req = new IntentSenderRequest.Builder(sender).build();
                                    fido2Launcher.launch(req);
                                } else {
                                    Log.w(TAG, "FIDO2 sign pendingIntent has no IntentSender");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to launch FIDO2 sign pending intent", e);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "getSignIntent failed", e);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "startFido2Sign error", e);
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 处理返回键：优先让 GeckoSession 后退（如果可行），否则最小化应用
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (geckoSession != null && mCanGoBack) {
                geckoSession.goBack();
            } else {
                moveTaskToBack(true);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // 与 onKeyDown 一致：可后退则后退，否则最小化
        if (geckoSession != null && mCanGoBack) {
            geckoSession.goBack();
        } else {
            moveTaskToBack(true);
        }
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
