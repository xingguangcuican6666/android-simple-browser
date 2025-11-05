package com.example.simplebrowser;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private EditText urlEdit;
    private Button shortcutBtn;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        urlEdit = new EditText(this);
        urlEdit.setHint("输入网址");
        
        shortcutBtn = new Button(this);
        shortcutBtn.setText("生成快捷方式");

        shortcutBtn.setOnClickListener(v -> {
            String url = urlEdit.getText().toString().trim();
            if (!url.isEmpty()) {
                // 确保URL有协议前缀
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                
                // 默认使用桌面版模式
                boolean desktopMode = true;
                String finalUrl = url;
                
                // 显示进度提示
                Toast.makeText(this, "正在获取网站图标...", Toast.LENGTH_SHORT).show();
                
                // 异步获取网站图标
                fetchFaviconAndCreateShortcut(finalUrl, desktopMode);
            } else {
                Toast.makeText(this, "请输入网址", Toast.LENGTH_SHORT).show();
            }
        });

        // 简单线性布局
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        layout.addView(urlEdit, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        
        layout.addView(shortcutBtn, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        
        setContentView(layout);
    }

    private void fetchFaviconAndCreateShortcut(String url, boolean desktopMode) {
        executorService.execute(() -> {
            Bitmap favicon = null;
            
            try {
                // 尝试多个常见的favicon位置
                String[] faviconUrls = {
                    getFaviconUrl(url, "/favicon.ico"),
                    getFaviconUrl(url, "/apple-touch-icon.png"),
                    getFaviconUrl(url, "/apple-touch-icon-precomposed.png")
                };
                
                for (String faviconUrl : faviconUrls) {
                    favicon = downloadFavicon(faviconUrl);
                    if (favicon != null) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            Bitmap finalFavicon = favicon;
            mainHandler.post(() -> createShortcut(url, desktopMode, finalFavicon));
        });
    }
    
    private String getFaviconUrl(String siteUrl, String faviconPath) {
        try {
            URL url = new URL(siteUrl);
            return url.getProtocol() + "://" + url.getHost() + faviconPath;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Bitmap downloadFavicon(String faviconUrl) {
        if (faviconUrl == null) return null;
        
        try {
            URL url = new URL(faviconUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private void createShortcut(String url, boolean desktopMode, Bitmap favicon) {
        android.content.pm.ShortcutManager shortcutManager =
                (android.content.pm.ShortcutManager) getSystemService(Context.SHORTCUT_SERVICE);
        
        // 使用Intent data来保存desktopMode，确保在快捷方式中被保留
        String uriString = url + (desktopMode ? "#desktopMode=true" : "#desktopMode=false");
        
        Intent intent = new Intent(this, WebViewActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(android.net.Uri.parse(uriString))
                .putExtra("url", url)
                .putExtra("desktopMode", desktopMode);
        
        android.content.pm.ShortcutInfo.Builder builder = 
                new android.content.pm.ShortcutInfo.Builder(this, url + "_" + System.currentTimeMillis())
                .setShortLabel("网页快捷方式")
                .setLongLabel(url)
                .setIntent(intent);
        
        // 如果成功获取了favicon，使用它作为图标
        if (favicon != null) {
            builder.setIcon(android.graphics.drawable.Icon.createWithBitmap(favicon));
            Toast.makeText(this, "已获取网站图标", Toast.LENGTH_SHORT).show();
        } else {
            builder.setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_view));
            Toast.makeText(this, "使用默认图标", Toast.LENGTH_SHORT).show();
        }
        
        android.content.pm.ShortcutInfo shortcut = builder.build();
        shortcutManager.requestPinShortcut(shortcut, null);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}