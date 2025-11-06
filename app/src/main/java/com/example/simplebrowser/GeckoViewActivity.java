package com.example.simplebrowser;

import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import androidx.appcompat.app.AppCompatActivity;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoSession.WebAuthnDelegate;

/**
 * 使用GeckoView（Firefox引擎）打开网页
 * 完全支持全屏、WebAuthn和现代Web标准
 */
public class GeckoViewActivity extends AppCompatActivity {
    
    private GeckoView geckoView;
    private GeckoSession geckoSession;
    private static GeckoRuntime sRuntime;
    
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
        GeckoSessionSettings settings = new GeckoSessionSettings.Builder()
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP) // 设置为桌面版User Agent
                .build();
        geckoSession = new GeckoSession(settings);
        geckoSession.open(sRuntime);
        geckoView.setSession(geckoSession);
        geckoSession.setWebAuthnDelegate(new MyWebAuthnDelegate());
        
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
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 处理返回键
        if (keyCode == KeyEvent.KEYCODE_BACK && geckoSession.getNavigationDelegate() != null) {
            // 尝试后退
            if (geckoSession.canGoBack()) {
                geckoSession.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geckoSession != null) {
            geckoSession.close();
        }
    }
}

    private class MyWebAuthnDelegate implements GeckoSession.WebAuthnDelegate {
        @Override
        public void onFido2Register(GeckoSession session, GeckoSession.WebAuthnRequest request) {
            // Use Google Play Services FIDO2 API to handle registration
            com.google.android.gms.fido.fido2.Fido2ApiClient fido2ApiClient = com.google.android.gms.fido.fido2.Fido2.getFido2ApiClient(GeckoViewActivity.this);
            com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions options = com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions.deserializeFromBytes(request.getPublicKeyCredentialCreationOptions());
            com.google.android.gms.tasks.Task<com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse> task = fido2ApiClient.register(options);
            task.addOnSuccessListener(response -> {
                request.complete(response.serializeToBytes());
            });
            task.addOnFailureListener(e -> {
                request.cancel();
            });
        }

        @Override
        public void onFido2Sign(GeckoSession session, GeckoSession.WebAuthnRequest request) {
            // Use Google Play Services FIDO2 API to handle signing
            com.google.android.gms.fido.fido2.Fido2ApiClient fido2ApiClient = com.google.android.gms.fido.fido2.Fido2.getFido2ApiClient(GeckoViewActivity.this);
            com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions options = com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions.deserializeFromBytes(request.getPublicKeyCredentialRequestOptions());
            com.google.android.gms.tasks.Task<com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse> task = fido2ApiClient.sign(options);
            task.addOnSuccessListener(response -> {
                request.complete(response.serializeToBytes());
            });
            task.addOnFailureListener(e -> {
                request.cancel();
            });
        }
    }
