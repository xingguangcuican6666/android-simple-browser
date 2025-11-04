# 保留主入口Activity，防止被混淆删除
-keep class com.example.simplebrowser.MainActivity { *; }
-keep class com.example.simplebrowser.WebViewActivity { *; }
# 保持默认配置，无需额外混淆规则