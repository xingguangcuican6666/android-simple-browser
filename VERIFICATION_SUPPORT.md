# CAPTCHA和通行密钥支持说明

## 概述

本应用已增强对人机验证（CAPTCHA）和通行密钥（Passkey/WebAuthn）的支持。以下是实施的改进措施：

## 1. 人机验证（CAPTCHA）支持增强

### Cookie和会话管理
- **第三方Cookie支持**：启用第三方cookie，CAPTCHA服务通常需要跨域cookie来跟踪验证状态
- **Cookie持久化**：确保验证状态在页面刷新后保持

### 缓存和存储
- **应用缓存**：启用应用缓存以加快CAPTCHA资源加载
- **数据库存储**：支持本地数据库存储，某些验证机制需要本地存储
- **DOM存储**：完整支持localStorage和sessionStorage

### 内容加载
- **混合内容模式**：允许HTTPS页面加载HTTP资源，某些CAPTCHA可能需要
- **文件访问**：启用文件和内容访问，支持本地CAPTCHA资源加载

### 多媒体支持
- **自动播放媒体**：禁用媒体播放的用户手势要求，支持音频CAPTCHA
- **音频/视频支持**：完整支持音频和视频CAPTCHA

### 地理位置
- **位置验证**：某些安全验证使用地理位置信息
- **自动授权**：自动授予地理位置权限（在用户授予应用权限后）

## 2. 通行密钥（Passkey/WebAuthn）支持

### 安全上下文
- **安全浏览**：启用安全浏览功能，WebAuthn需要安全上下文
- **HTTPS支持**：确保在安全连接下正常工作

### 权限支持
- **生物识别**：支持访问设备生物识别传感器（指纹、面部识别）
- **相机权限**：支持二维码扫描等功能
- **自动权限授予**：自动授予WebView所需的权限

### 运行时权限
应用会在启动时请求以下权限：
- **相机**（CAMERA）：用于二维码扫描和生物识别
- **麦克风**（RECORD_AUDIO）：用于语音验证
- **精确位置**（ACCESS_FINE_LOCATION）：用于位置验证
- **粗略位置**（ACCESS_COARSE_LOCATION）：用于位置验证

## 3. 用户代理和设备伪装

- **桌面模式**：使用Windows Chrome用户代理
- **屏幕尺寸伪装**：模拟1920x1080桌面分辨率
- **触摸检测隐藏**：隐藏移动设备的触摸特征

## 4. 使用说明

### 首次使用
1. 应用启动时会请求必要的权限
2. 建议授予所有权限以获得最佳体验
3. 即使拒绝某些权限，应用仍可继续运行

### CAPTCHA处理
- 大多数CAPTCHA（包括reCAPTCHA、hCaptcha等）应该能正常工作
- 如果遇到CAPTCHA问题，请确保已授予所有请求的权限
- 某些CAPTCHA可能需要多次尝试

### Passkey使用
- 支持标准的WebAuthn协议
- 可以使用设备的生物识别功能进行身份验证
- 确保访问的网站支持WebAuthn/FIDO2

## 5. 技术细节

### WebView设置
```java
// Cookie支持
CookieManager.setAcceptCookie(true)
CookieManager.setAcceptThirdPartyCookies(webView, true)

// 缓存和存储
setCacheMode(LOAD_DEFAULT)
setDatabaseEnabled(true)
setDomStorageEnabled(true)

// 多媒体和内容
setMixedContentMode(MIXED_CONTENT_ALWAYS_ALLOW)
setMediaPlaybackRequiresUserGesture(false)
setGeolocationEnabled(true)

// 安全和权限
setSafeBrowsingEnabled(true)
```

### 权限处理
```java
// WebChromeClient中的权限自动授予
onGeolocationPermissionsShowPrompt() -> callback.invoke(origin, true, false)
onPermissionRequest() -> request.grant(request.getResources())
```

## 6. 已知限制

- WebView对某些高级WebAuthn功能的支持可能有限
- 某些网站可能检测到WebView环境并限制功能
- 生物识别支持取决于设备硬件和Android版本

## 7. 故障排除

### CAPTCHA无法显示
- 检查网络连接
- 确保已授予所有权限
- 尝试刷新页面

### Passkey无法使用
- 确保网站支持WebAuthn
- 检查是否已授予相机权限
- 确认设备支持生物识别

### 权限被拒绝
- 进入系统设置 > 应用 > SimpleBrowser > 权限
- 手动授予所需的权限

## 8. 安全注意事项

**重要安全说明：**

本应用为了支持CAPTCHA和Passkey功能，采用了以下权限策略：

- **自动授予WebView权限**：应用会自动授予网页请求的相机、麦克风和地理位置权限
- **混合内容模式**：允许HTTPS页面加载HTTP资源，这可能降低安全性
- **建议措施**：
  - 仅访问信任的网站
  - 在生产环境中，建议实施origin白名单机制
  - 定期检查应用权限使用情况

## 9. 隐私说明

- 应用仅在用户授权后才访问相机、麦克风和位置
- 权限仅用于支持网页功能，不会后台收集数据
- 所有数据存储在本地，不会发送到第三方服务器
