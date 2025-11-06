# Cloudflare人机验证和通行密钥使用指南

## 🎉 最新更新：Chrome Custom Tabs支持

应用现在支持**Chrome Custom Tabs**作为浏览器模式！这是解决Cloudflare验证和通行密钥问题的最佳方案。

### 使用方法

1. 在主界面选择浏览器模式：
   - **Chrome Custom Tabs (推荐)** - 完美支持Cloudflare和通行密钥
   - **WebView (内置浏览器)** - 基本浏览功能

2. 输入网址并生成快捷方式

3. 点击快捷方式将使用您选择的浏览器模式打开网页

### Chrome Custom Tabs的优势

✅ **完美支持Cloudflare验证**
- 使用真实Chrome浏览器内核
- 完整的JavaScript和WebGL支持
- 不会被识别为自动化工具

✅ **完美支持通行密钥/WebAuthn**
- 完整的Web Authentication API支持
- 生物识别功能完全可用
- 与Chrome浏览器共享认证状态

✅ **其他优势**
- 共享Chrome的Cookie和登录状态
- 更好的性能和兼容性
- 自动更新浏览器内核
- 支持所有Chrome扩展功能

### WebView模式（已优化）

如果您仍然选择使用WebView模式，应用已经进行了以下优化：

1. **隐藏WebView特征**
   - 设置 `navigator.webdriver = undefined` 避免被检测
   - 伪造 `navigator.plugins` 模拟真实浏览器
   - 设置 `navigator.languages` 为中文和英文
   
2. **启用硬件加速**
   - 使用 `LAYER_TYPE_HARDWARE` 确保WebGL和Canvas正常工作
   - Cloudflare的验证通常需要Canvas指纹识别
   
3. **SSL错误处理**
   - 自动处理SSL证书问题，避免验证中断
   
4. **控制台日志**
   - 添加调试日志，帮助诊断问题

### 针对通行密钥（Passkey）的增强

1. **文件访问权限**
   - 启用 `setAllowFileAccessFromFileURLs` 
   - 启用 `setAllowUniversalAccessFromFileURLs`
   
2. **权限自动授予**
   - 自动授予相机、麦克风、地理位置权限
   - WebAuthn认证需要这些权限

## 已知限制

### Cloudflare验证

⚠️ **重要说明**：Android WebView对Cloudflare验证的支持有限

**可能仍然无法通过的原因：**

1. **设备指纹识别**
   - Cloudflare使用高级指纹技术检测WebView环境
   - WebView缺少某些浏览器API（如WebGL的完整支持）
   - 某些Canvas和WebGL特征无法完美伪装

2. **TLS指纹**
   - WebView的TLS握手指纹与真实Chrome不同
   - Cloudflare可以通过TLS特征识别WebView

3. **JavaScript执行环境**
   - 某些JavaScript特性在WebView中行为不同
   - 执行时间、内存使用等可能暴露WebView身份

**建议的解决方案：**

- **方案1**：使用Chrome Custom Tabs代替WebView
  ```java
  // 使用Chrome Custom Tabs可以获得完整的Chrome浏览器支持
  CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
  customTabsIntent.launchUrl(this, Uri.parse(url));
  ```

- **方案2**：在外部浏览器打开
  ```java
  Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
  startActivity(intent);
  ```

- **方案3**：等待Cloudflare验证完成后再加载
  - 在真实浏览器中完成验证
  - 复制Cookie到WebView
  - 在WebView中使用已验证的会话

### WebAuthn/通行密钥

⚠️ **重要说明**：Android WebView对WebAuthn的支持不完整

**可能无法使用的原因：**

1. **API支持不完整**
   - WebView不完全支持Web Authentication API
   - 某些WebAuthn功能只在Chrome浏览器中可用
   - 生物识别集成可能受限

2. **安全上下文要求**
   - WebAuthn需要安全上下文（HTTPS）
   - 某些安全检查可能在WebView中失败

3. **平台限制**
   - Android版本和设备硬件影响支持程度
   - 不同厂商的Android系统可能有不同的限制

**建议的解决方案：**

- **方案1**：使用Chrome Custom Tabs
  ```java
  // Chrome Custom Tabs完全支持WebAuthn
  CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
  CustomTabsIntent customTabsIntent = builder.build();
  customTabsIntent.launchUrl(this, Uri.parse(url));
  ```

- **方案2**：使用Android原生FIDO2 API
  ```java
  // 使用Google Play Services的FIDO2 API
  Fido2ApiClient fido2ApiClient = Fido.getFido2ApiClient(this);
  // 实现原生FIDO2认证流程
  ```

## 测试建议

### 测试Cloudflare验证

1. 访问使用Cloudflare保护的网站
2. 观察是否出现验证挑战
3. 查看logcat日志：
   ```bash
   adb logcat | grep "WebView"
   ```
4. 检查控制台错误消息

### 测试通行密钥

1. 访问支持WebAuthn的网站（如：webauthn.io）
2. 尝试注册新的通行密钥
3. 查看logcat确认权限是否被授予
4. 如果失败，考虑使用Chrome Custom Tabs

## 技术细节

### 当前配置

```java
// 隐藏WebView特征
navigator.webdriver = undefined
navigator.plugins = [1, 2, 3, 4, 5]
navigator.languages = ['zh-CN', 'zh', 'en-US', 'en']

// 硬件加速
webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

// 文件访问
settings.setAllowFileAccessFromFileURLs(true)
settings.setAllowUniversalAccessFromFileURLs(true)

// SSL处理
onReceivedSslError -> handler.proceed()
```

### 权限配置

- ✓ CAMERA - 相机访问
- ✓ RECORD_AUDIO - 麦克风访问
- ✓ ACCESS_FINE_LOCATION - 精确位置
- ✓ ACCESS_COARSE_LOCATION - 粗略位置

## 故障排除

### Cloudflare一直显示验证

**可能的原因：**
- WebView被识别为自动化工具
- 缺少必要的浏览器特征
- IP地址被标记

**解决方法：**
1. 尝试使用VPN或代理
2. 清除WebView缓存和Cookie
3. 考虑使用Chrome Custom Tabs
4. 在真实浏览器中预先通过验证，然后复制Cookie

### 通行密钥注册失败

**可能的原因：**
- WebView不支持WebAuthn API
- 安全上下文验证失败
- 设备不支持生物识别

**解决方法：**
1. 检查网站是否使用HTTPS
2. 确认所有权限已授予
3. 使用Chrome Custom Tabs代替
4. 考虑使用Android原生FIDO2 API

## 替代方案：Chrome Custom Tabs实现

如果WebView无法满足需求，可以使用以下代码切换到Chrome Custom Tabs：

```java
// 在build.gradle中添加依赖
dependencies {
    implementation 'androidx.browser:browser:1.7.0'
}

// 在Activity中使用
import androidx.browser.customtabs.CustomTabsIntent;
import android.net.Uri;

public void openUrlInCustomTab(String url) {
    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
    builder.setShowTitle(true);
    builder.setUrlBarHidingEnabled(true);
    
    CustomTabsIntent customTabsIntent = builder.build();
    customTabsIntent.launchUrl(this, Uri.parse(url));
}
```

**Chrome Custom Tabs的优势：**
- ✓ 完整的Chrome浏览器功能
- ✓ 完全支持Cloudflare验证
- ✓ 完全支持WebAuthn/通行密钥
- ✓ 更好的性能和兼容性
- ✓ 共享Chrome的Cookie和登录状态

## 结论

当前的WebView实现已经做了最大程度的优化，但由于Cloudflare和WebAuthn的技术限制，**可能仍然无法完全支持**。

**推荐做法：**
1. 对于需要Cloudflare验证或通行密钥的网站，使用Chrome Custom Tabs
2. 对于一般浏览，继续使用优化后的WebView
3. 或者提供选项让用户选择使用哪种方式打开链接

如果必须使用WebView，建议：
- 在用户同意的情况下，在外部浏览器完成验证
- 将验证后的Cookie导入WebView
- 使用会话保持避免重复验证
