# Android Simple Browser

A simple Android browser application with enhanced support for CAPTCHA verification and passkey authentication. **Now with Chrome Custom Tabs support for perfect Cloudflare and WebAuthn compatibility!**

## 🎉 New Feature: Chrome Custom Tabs

The app now supports **Chrome Custom Tabs** as the recommended browsing mode for:
- ✅ Perfect Cloudflare verification support
- ✅ Full WebAuthn/passkey functionality
- ✅ Real Chrome browser experience
- ✅ Shared cookies and login state with Chrome

## Features

### Dual Browser Modes

**1. Chrome Custom Tabs (Recommended)**
- Uses the actual Chrome browser
- Perfect support for all modern web features
- Cloudflare verification works flawlessly
- WebAuthn/passkeys fully functional
- Shares Chrome's cookies and sessions

**2. WebView Mode (Enhanced)**
- Built-in browser view
- Desktop mode spoofing
- Enhanced CAPTCHA support
- Basic passkey support (with limitations)

### Desktop Mode (WebView)
- Spoofs Windows Chrome user agent
- Simulates 1920x1080 desktop resolution
- Hides mobile touch features

### Enhanced CAPTCHA Support
- Full cookie support including third-party cookies
- Application cache and database storage
- Mixed content mode for loading resources
- Automatic media playback for audio/video CAPTCHAs
- Geolocation support for location-based verification

### Passkey/WebAuthn Support
- WebAuthn protocol support
- Biometric authentication (fingerprint, face recognition)
- Safe browsing enabled for secure context
- Automatic permission granting for WebView

## Permissions

The app requests the following runtime permissions:
- **Camera**: For QR code scanning and biometric verification
- **Microphone**: For voice verification
- **Fine Location**: For location-based verification
- **Coarse Location**: For location-based verification

All permissions are optional - the app will continue to work even if some are denied, but certain features may not be available.

## Usage

1. Launch the app
2. Enter a URL in the input field
3. **Choose your browser mode:**
   - **Chrome Custom Tabs (Recommended)**: For perfect Cloudflare and passkey support
   - **WebView**: For basic browsing with desktop mode
4. Tap "生成快捷方式" (Create Shortcut) to create a home screen shortcut
5. The shortcut will open the website using your selected browser mode

### When to Use Each Mode

**Use Chrome Custom Tabs when:**
- Website requires Cloudflare verification
- Using passkeys/WebAuthn authentication
- Need full modern web feature support
- Want to share login state with Chrome

**Use WebView when:**
- Need a completely embedded browser experience
- Want desktop mode spoofing
- Don't need advanced verification features

## Technical Details

### Enhanced WebView Settings
- JavaScript enabled
- DOM storage enabled
- Cookies (including third-party) enabled
- Application cache enabled
- Mixed content mode allowed
- Geolocation enabled
- Media autoplay enabled
- Safe browsing enabled

### Permission Handling
The app automatically grants WebView permission requests for:
- Geolocation
- Camera
- Microphone
- Other resources required for WebAuthn

## Building

```bash
./gradlew build
```

## Requirements
- Android SDK 24 (Android 7.0) or higher
- Target SDK: 34 (Android 14)

## Privacy

- Permissions are only used to support web page functionality
- No data collection or transmission to third-party servers
- All data is stored locally on the device

## Security Considerations

**Important Security Notes:**

To support CAPTCHA and passkey functionality, this app implements the following permission policies:

- **Automatic WebView Permission Granting**: The app automatically grants camera, microphone, and geolocation permissions requested by web pages
- **Mixed Content Mode**: Allows HTTPS pages to load HTTP resources, which may reduce security
- **Recommendations**:
  - Only visit trusted websites
  - For production use, consider implementing an origin whitelist mechanism
  - Regularly review app permission usage

## License

This project is open source.
