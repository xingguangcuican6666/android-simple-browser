package com.example.simplebrowser;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("url");
        
        // 如果从Intent data URI中获取URL
        if (getIntent().getData() != null) {
            android.net.Uri data = getIntent().getData();
            String uriString = data.toString();
            // 移除可能的fragment标记
            url = uriString.split("#")[0];
        }
        
        WebView webView = new WebView(this);
        
        // 自定义WebViewClient来注入JavaScript
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                
                // 注入JavaScript代码，伪造桌面环境
                String jsCode = 
                    "(function() {" +
                    "  Object.defineProperty(window.screen, 'width', {get: function() { return 1920; }});" +
                    "  Object.defineProperty(window.screen, 'height', {get: function() { return 1080; }});" +
                    "  Object.defineProperty(window.screen, 'availWidth', {get: function() { return 1920; }});" +
                    "  Object.defineProperty(window.screen, 'availHeight', {get: function() { return 1080; }});" +
                    "  Object.defineProperty(window, 'innerWidth', {get: function() { return 1920; }});" +
                    "  Object.defineProperty(window, 'innerHeight', {get: function() { return 1080; }});" +
                    "  Object.defineProperty(window, 'outerWidth', {get: function() { return 1920; }});" +
                    "  Object.defineProperty(window, 'outerHeight', {get: function() { return 1080; }});" +
                    "  if ('ontouchstart' in window) {" +
                    "    delete window.ontouchstart;" +
                    "  }" +
                    "  Object.defineProperty(navigator, 'maxTouchPoints', {get: function() { return 0; }});" +
                    "})();";
                
                view.evaluateJavascript(jsCode, null);
            }
        });
        
        // 强制启用桌面版模式 - 使用Windows Chrome User-Agent以获得更好的兼容性
        String desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        webView.getSettings().setUserAgentString(desktopUserAgent);
        
        // 启用所有必要的WebView设置
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        // 设置初始缩放
        webView.setInitialScale(1);
        
        if (url != null && !url.isEmpty()) {
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
}