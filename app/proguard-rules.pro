# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools/proguard/proguard-android.txt file.

# Keep Device Admin receiver
-keep class com.bfoxnet.dashboard.admin.** { *; }

# Keep Accessibility service
-keep class com.bfoxnet.dashboard.services.ProtectionAccessibilityService { *; }
