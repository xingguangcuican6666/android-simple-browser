package com.example.simplebrowser;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import android.graphics.Color;

/**
 * 使用Chrome Custom Tabs打开网页
 * 完全支持Cloudflare验证和WebAuthn/通行密钥
 */
public class ChromeTabActivity extends AppCompatActivity {
    
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
        
        if (url != null && !url.isEmpty()) {
            openUrlInCustomTab(url);
        }
        
        // Chrome Custom Tabs会在外部打开，关闭此Activity
        finish();
    }
    
    /**
     * 使用Chrome Custom Tabs打开URL
     * 优势：
     * - 完全支持Cloudflare验证
     * - 完全支持WebAuthn/通行密钥
     * - 使用真实Chrome浏览器内核
     * - 共享Chrome的Cookie和登录状态
     * - 更好的性能和兼容性
     */
    private void openUrlInCustomTab(String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        
        // 自定义工具栏颜色
        builder.setToolbarColor(Color.parseColor("#2196F3"));
        
        // 显示网页标题
        builder.setShowTitle(true);
        
        // 启用URL栏隐藏（向下滚动时）
        builder.setUrlBarHidingEnabled(true);
        
        // 添加默认分享按钮
        builder.setShareState(CustomTabsIntent.SHARE_STATE_ON);
        
        // 设置启动动画
        builder.setStartAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        builder.setExitAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        
        CustomTabsIntent customTabsIntent = builder.build();
        
        // 启动Chrome Custom Tab
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }
}
