#!/bin/bash
# Helper script to install the APK on your phone via ADB

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "=== FitnessCat APK Installation Helper ==="
echo ""

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at: $APK_PATH"
    echo "Please build the APK first in Android Studio:"
    echo "  Build → Build Bundle(s) / APK(s) → Build APK(s)"
    exit 1
fi

echo "✅ APK found: $APK_PATH"
echo "   Size: $(ls -lh "$APK_PATH" | awk '{print $5}')"
echo ""

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "⚠️  ADB not found in PATH"
    echo ""
    echo "To install via ADB:"
    echo "1. Install Android SDK Platform Tools"
    echo "2. Or use Android Studio's built-in ADB"
    echo ""
    echo "Alternative: Copy the APK to your phone and install manually"
    echo "  APK location: $(pwd)/$APK_PATH"
    exit 0
fi

# Check if device is connected
echo "Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ No Android device connected"
    echo ""
    echo "Please:"
    echo "1. Connect your phone via USB"
    echo "2. Enable USB Debugging on your phone"
    echo "3. Allow USB debugging when prompted"
    echo ""
    echo "To enable USB Debugging:"
    echo "  Settings → About Phone → Tap 'Build Number' 7 times"
    echo "  Then: Settings → Developer Options → Enable USB Debugging"
    exit 1
fi

echo "✅ Device connected"
echo ""

# Uninstall old version if exists
echo "Checking for existing installation..."
if adb shell pm list packages | grep -q "com.fitnesscat.stepstracker"; then
    echo "⚠️  App already installed. Uninstalling old version..."
    adb uninstall com.fitnesscat.stepstracker
    if [ $? -eq 0 ]; then
        echo "✅ Old version uninstalled"
    else
        echo "⚠️  Could not uninstall (might need to uninstall manually from phone)"
    fi
    echo ""
else
    echo "✅ No existing installation found"
    echo ""
fi

# Install APK
echo "Installing APK..."
adb install "$APK_PATH"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Installation successful!"
    echo ""
    echo "You can now:"
    echo "  - Open the app from your phone's app drawer"
    echo "  - Or run: adb shell am start -n com.fitnesscat.stepstracker/.MainActivity"
else
    echo ""
    echo "❌ Installation failed"
    echo ""
    echo "Common issues:"
    echo "1. App already installed with different signature - uninstall manually first"
    echo "2. Insufficient storage on device"
    echo "3. Installation from unknown sources not enabled"
    echo ""
    echo "Try:"
    echo "  - Uninstall the app manually from your phone"
    echo "  - Enable 'Install from Unknown Sources' in phone settings"
    echo "  - Or copy APK to phone and install manually"
fi


