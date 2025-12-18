# How to Build the APK File

## Method 1: Using Android Studio (Easiest)

1. **Open Android Studio** with your project
2. Go to: **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
3. Wait for the build to complete (you'll see progress in the bottom status bar)
4. When done, you'll see a notification: **"APK(s) generated successfully"**
5. Click **"locate"** in the notification
6. The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Method 2: Using Terminal (if gradle wrapper exists)

If you have the gradle wrapper, you can run:
```bash
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## After Building

Once you have the APK:
1. **Copy it to your phone** (via USB, email, or cloud storage)
2. **On your phone**: Enable "Install from unknown sources" in Settings → Security
3. **Tap the APK file** on your phone to install




