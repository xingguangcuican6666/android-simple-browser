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
        boolean desktopMode = getIntent().getBooleanExtra("desktopMode", false);
        
        // 如果extras中没有desktopMode，尝试从Intent data URI中读取
        if (!desktopMode && getIntent().getData() != null) {
            android.net.Uri data = getIntent().getData();
            String uriString = data.toString();
            
            // 从URI fragment中提取desktopMode参数
            if (uriString.contains("#desktopMode=true")) {
                desktopMode = true;
                // 移除fragment，获取真实URL
                url = uriString.substring(0, uriString.indexOf("#desktopMode="));
            } else if (uriString.contains("#desktopMode=false")) {
                desktopMode = false;
                url = uriString.substring(0, uriString.indexOf("#desktopMode="));
            } else if (url == null || url.isEmpty()) {
                // 如果没有url extra，直接使用URI（去掉可能的fragment）
                url = uriString.split("#")[0];
            }
        }
        
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        
        // 设置桌面版模式
        if (desktopMode) {
            String desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            webView.getSettings().setUserAgentString(desktopUserAgent);
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setLoadWithOverviewMode(true);
        }
        
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