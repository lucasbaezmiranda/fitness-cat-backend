# APK Installation Troubleshooting Guide

## Quick Fix: Standard APK Location

The APK should be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

If you see a file named `steps-debug-v2.apk`, it's the same file - just renamed.

## Method 1: Install via Script (Recommended)

I've created a helper script that will:
- Check if your phone is connected
- Uninstall old version if needed
- Install the new APK

```bash
cd /home/luke/Desktop/repositorios/fitness-cat-backend
./install_apk.sh
```

## Method 2: Manual Installation via ADB

```bash
cd /home/luke/Desktop/repositorios/fitness-cat-backend

# 1. Uninstall old version (if exists)
adb uninstall com.fitnesscat.stepstracker

# 2. Install new APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Method 3: Manual Installation on Phone

1. **Copy APK to phone**:
   - Via USB: Copy `app/build/outputs/apk/debug/app-debug.apk` to your phone
   - Via cloud: Upload to Google Drive/Dropbox and download on phone

2. **Enable installation from unknown sources**:
   - Settings → Security → Install unknown apps
   - Enable for your file manager/browser

3. **Install**:
   - Open the APK file on your phone
   - Tap "Install"

## Common Issues & Solutions

### Issue: "App not installed" or "Package appears to be corrupt"

**Solution:**
1. Uninstall the existing app from your phone first
2. Rebuild the APK in Android Studio: **Build → Clean Project**, then **Build → Rebuild Project**
3. Try installing again

### Issue: "Installation failed" or nothing happens

**Possible causes:**
1. **Old version with different signature**: Uninstall the old app first
2. **Insufficient storage**: Free up space on your phone
3. **Security settings**: Enable "Install from unknown sources"

**Fix:**
```bash
# Uninstall via ADB
adb uninstall com.fitnesscat.stepstracker

# Then install again
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Issue: "Device not found" (ADB)

**Solution:**
1. Enable USB Debugging on your phone:
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → Developer Options → Enable USB Debugging
2. Connect phone via USB
3. Allow USB debugging when prompted on phone
4. Check connection: `adb devices`

### Issue: Build fails in Android Studio

**Solution:**
1. **Clean and rebuild**:
   - Build → Clean Project
   - Build → Rebuild Project

2. **Check for errors**:
   - Look at the "Build" tab at the bottom
   - Check "Problems" tab for compilation errors

3. **Sync Gradle**:
   - File → Sync Project with Gradle Files

## Verify APK is Valid

The APK should be:
- **Size**: ~6-7 MB (not 0 bytes)
- **Type**: Android package (APK)
- **Location**: `app/build/outputs/apk/debug/app-debug.apk`

Check with:
```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
file app/build/outputs/apk/debug/app-debug.apk
```

## Rebuild APK in Android Studio

If you need to rebuild:

1. **Clean build**:
   - Build → Clean Project
   - Wait for completion

2. **Build APK**:
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Wait for "APK(s) generated successfully" notification

3. **Find APK**:
   - Click "locate" in the notification, or
   - Navigate to: `app/build/outputs/apk/debug/app-debug.apk`

## Still Having Issues?

1. **Check Logcat** in Android Studio for installation errors
2. **Try installing on a different device** to isolate the issue
3. **Check Android version**: App requires Android 7.0+ (API 24+)
4. **Verify permissions**: Make sure all required permissions are in AndroidManifest.xml



