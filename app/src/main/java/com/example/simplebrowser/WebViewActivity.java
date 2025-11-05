package com.example.simplebrowser;

import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {
    
    private WebView webView;
    
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
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        
        // 设置布局算法以更好地处理桌面网页
        webView.getSettings().setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.NORMAL);
        
        // 缩放设置
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        // 强制设置默认缩放比例，提供更好的可读性
        webView.setInitialScale(100);  // 设置为100%，提供最佳可读性
        
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
        });
        
        // 设置WebChromeClient以获得更早的注入时机
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // 在页面加载过程中持续注入
                if (newProgress < 100) {
                    view.evaluateJavascript(getDesktopSpoofScript(), null);
                }
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理WebView资源
        if (webView != null) {
            webView.destroy();
        }
    }
}