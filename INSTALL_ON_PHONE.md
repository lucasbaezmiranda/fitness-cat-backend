# How to Install the App on Your Phone

## Method 1: Direct Install from Android Studio (Recommended)

### Step 1: Enable USB Debugging on Your Phone

1. **Go to Settings** on your Android phone
2. **About Phone** → Tap **"Build Number"** 7 times (you'll see "You are now a developer!")
3. **Go back** → **Developer Options** (now visible)
4. **Enable "USB Debugging"**

### Step 2: Connect Your Phone

1. Connect your phone to your computer via USB cable
2. On your phone, you'll see a popup: **"Allow USB debugging?"** → Tap **"Allow"** (check "Always allow" if you want)

### Step 3: Build and Install from Android Studio

1. **Open Android Studio** with your project
2. **Wait for device detection**: Your phone should appear in the device dropdown (top toolbar, next to the green play button)
3. **Select your phone** from the dropdown
4. **Click the green Run button** (▶️) or press `Shift+F10`
5. Android Studio will:
   - Build the app
   - Install it on your phone
   - Launch it automatically

**That's it!** The app will be installed and running on your phone.

---

## Method 2: Build APK and Install Manually

If you want to build an APK file to install manually:

### Step 1: Build APK in Android Studio

1. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for build to complete
3. Click **"locate"** in the notification when it says "APK(s) generated successfully"

### Step 2: Find the APK

The APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Transfer to Phone

**Option A: USB Transfer**
- Copy `app-debug.apk` to your phone via USB
- On your phone, open the file manager and tap the APK to install

**Option B: Email/Cloud**
- Email the APK to yourself or upload to Google Drive
- Download on your phone and tap to install

### Step 4: Install on Phone

1. **Enable "Install from Unknown Sources"**:
   - Settings → Security → Enable "Install unknown apps" (or similar, varies by phone)
2. **Open the APK file** on your phone
3. **Tap "Install"**

---

## Troubleshooting

### Phone Not Detected?

1. **Check USB connection**: Try a different USB cable/port
2. **Check USB mode**: On phone, when connected, change USB mode to "File Transfer" or "MTP"
3. **Restart ADB**: In Android Studio terminal, run:
   ```bash
   adb kill-server
   adb start-server
   ```
4. **Check device**: Run `adb devices` to see if phone is listed

### Build Fails?

- Make sure you've synced Gradle: **File → Sync Project with Gradle Files**
- Check that all dependencies downloaded successfully

### Permission Denied?

- Make sure USB Debugging is enabled
- On phone, check the USB debugging authorization popup

---

## Quick Test

After installation, the app should:
1. Show your **User ID**
2. Display your **step count** (may be 0 if you haven't walked yet)
3. Have a **"Sync to Cloud"** button

Try walking around and opening the app again - the step count should update!




