package com.example.simplebrowser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebSettings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class WebViewActivity extends AppCompatActivity {
    
    private WebView webView;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    // 需要的权限列表
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    private String getDesktopSpoofScript() {
        return 
            "(function() {" +
            "  try {" +
            "    Object.defineProperty(window.screen, 'width', {get: function() { return 1920; }, configurable: true});" +
            "    Object.defineProperty(window.screen, 'height', {get: function() { return 1080; }, configurable: true});" +
            "    Object.defineProperty(window.screen, 'availWidth', {get: function() { return 1920; }, configurable: true});" +
            "    Object.defineProperty(window.screen, 'availHeight', {get: function() { return 1080; }, configurable: true});" +
            "    Object.defineProperty(window, 'innerWidth', {get: function() { return 1920; }, configurable: true});" +
            "    Object.defineProperty(window, 'innerHeight', {get: function() { return 1080; }, configurable: true});" +
            "    Object.defineProperty(window, 'outerWidth', {get: function() { return 1920; }, configurable: true});" +
            "    Object.defineProperty(window, 'outerHeight', {get: function() { return 1080; }, configurable: true});" +
            "    Object.defineProperty(navigator, 'maxTouchPoints', {get: function() { return 0; }, configurable: true});" +
            "    Object.defineProperty(navigator, 'platform', {get: function() { return 'Win32'; }, configurable: true});" +
            "    if ('ontouchstart' in window) { delete window.ontouchstart; }" +
            // 增强Cloudflare兼容性 - 隐藏WebView特征
            "    Object.defineProperty(navigator, 'webdriver', {get: function() { return undefined; }, configurable: true});" +
            "    Object.defineProperty(navigator, 'plugins', {get: function() { return [1, 2, 3, 4, 5]; }, configurable: true});" +
            "    Object.defineProperty(navigator, 'languages', {get: function() { return ['zh-CN', 'zh', 'en-US', 'en']; }, configurable: true});" +
            // 修改viewport meta标签
            "    var viewport = document.querySelector('meta[name=viewport]');" +
            "    if (viewport) {" +
            "      viewport.setAttribute('content', 'width=1920, initial-scale=1.0');" +
            "    } else {" +
            "      var meta = document.createElement('meta');" +
            "      meta.name = 'viewport';" +
            "      meta.content = 'width=1920, initial-scale=1.0';" +
            "      document.getElementsByTagName('head')[0].appendChild(meta);" +
            "    }" +
            "  } catch(e) {}" +
            "})();";
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 请求必要的运行时权限
        requestNecessaryPermissions();
        
        webView = new WebView(this);
        
        String url = getIntent().getStringExtra("url");
        
        // 如果从Intent data URI中获取URL
        if (getIntent().getData() != null) {
            android.net.Uri data = getIntent().getData();
            String uriString = data.toString();
            // 移除可能的fragment标记
            url = uriString.split("#")[0];
        }
        
        // 强制启用桌面版模式 - 使用Windows Chrome User-Agent以获得更好的兼容性
        String desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        webView.getSettings().setUserAgentString(desktopUserAgent);
        
        // 启用所有必要的WebView设置
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // 设置布局算法以更好地处理桌面网页
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        
        // 缩放设置
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        // 强制设置默认缩放比例，提供更好的可读性
        webView.setInitialScale(100);  // 设置为100%，提供最佳可读性
        
        // ========== 增强CAPTCHA和人机验证支持 ==========
        
        // 启用Cookie支持 - CAPTCHA通常需要cookies来跟踪验证状态
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        
        // 启用缓存 - 帮助加载CAPTCHA资源
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 启用数据库存储 - 某些验证机制需要
        settings.setDatabaseEnabled(true);
        
        // 允许混合内容 - 某些CAPTCHA可能从HTTP加载资源
        // 注意：这会降低安全性，但某些CAPTCHA服务需要此设置
        // 如需更高安全性，可改为MIXED_CONTENT_COMPATIBILITY_MODE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        // 启用文件访问 - 某些验证可能需要访问本地资源
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // 启用地理位置 - 某些验证使用位置信息
        settings.setGeolocationEnabled(true);
        settings.setGeolocationDatabasePath(getApplicationContext().getFilesDir().getPath());
        
        // 启用媒体播放 - 某些CAPTCHA包含音频/视频
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // 启用JavaScript可以打开新窗口
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        
        // ========== Passkey/WebAuthn支持设置 ==========
        
        // WebAuthn需要安全上下文，确保支持
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        
        // 启用硬件加速 - Cloudflare和WebAuthn需要
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // 允许文件访问来自文件URL - 某些验证需要
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
        
        // 自定义WebViewClient来注入JavaScript
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // 在页面开始加载时注入
                view.evaluateJavascript(getDesktopSpoofScript(), null);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后再次注入，确保覆盖任何后加载的检测
                view.evaluateJavascript(getDesktopSpoofScript(), null);
            }
            
            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                // 注意：在生产环境中应谨慎处理SSL错误
                // 这里为了兼容性自动继续，但建议在生产中添加用户确认
                handler.proceed();
            }
        });
        
        // 设置WebChromeClient以获得更早的注入时机和处理权限请求
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // 在页面加载过程中持续注入
                if (newProgress < 100) {
                    view.evaluateJavascript(getDesktopSpoofScript(), null);
                }
            }
            
            // 处理地理位置权限请求 - 某些验证使用位置信息
            // 注意：为了支持位置验证，自动授予权限
            // 在生产环境中，建议添加origin白名单验证
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // 自动授予地理位置权限以支持位置验证
                callback.invoke(origin, true, false);
            }
            
            // 处理权限请求 - 支持相机、麦克风等（用于生物识别和passkey）
            // 注意：为了支持WebAuthn和验证，自动授予权限
            // 在生产环境中，建议添加origin白名单或用户确认
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    // 授予所有请求的权限以支持WebAuthn和验证
                    // WebAuthn可能需要访问设备的生物识别传感器
                    request.grant(request.getResources());
                }
            }
            
            // 处理控制台消息 - 帮助调试
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("WebView", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return true;
            }
        });
        
        // 恢复保存的状态（如果有）
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        }
        
        setContentView(webView);

        // 全屏设置，忽略刘海屏
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
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存WebView状态
        webView.saveState(outState);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 处理返回键：如果WebView可以后退，则后退，否则关闭Activity
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    /**
     * 请求必要的权限以支持人机验证和passkey功能
     */
    private void requestNecessaryPermissions() {
        // 检查是否需要请求权限
        java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        // 如果有需要请求的权限，则请求
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                permissionsToRequest.toArray(new String[0]),
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 权限结果处理 - 即使拒绝也继续运行，只是某些功能可能不可用
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 可以在这里添加日志或提示，但不阻止应用运行
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理WebView资源
        if (webView != null) {
            webView.destroy();
        }
    }
}