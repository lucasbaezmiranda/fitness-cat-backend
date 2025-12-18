# Android Studio Setup Guide (for VS Code Users)

This guide will help you set up and run the Android app if you're coming from VS Code.

## Step 1: Install Android Studio

### On Linux (Ubuntu/Debian):

```bash
# Download Android Studio
# Option 1: Using snap (easiest)
sudo snap install android-studio --classic

# Option 2: Download from website
# Visit: https://developer.android.com/studio
# Download the .tar.gz file, extract, and run bin/studio.sh
```

### Alternative: Command Line Installation

```bash
# Add repository
sudo add-apt-repository ppa:maarten-fonville/android-studio
sudo apt update

# Install
sudo apt install android-studio
```

## Step 2: First Launch Setup

1. **Launch Android Studio** (from Applications menu or terminal: `android-studio`)

2. **Welcome Screen**: Choose "New Project" or "Open" (we'll open existing)

3. **Setup Wizard** will appear:
   - Choose "Standard" installation
   - Let it download Android SDK components (this takes 10-30 minutes)
   - Accept licenses when prompted
   - Wait for download to complete

## Step 3: Open Your Project

1. **From Welcome Screen**:
   - Click "Open" or "Open an Existing Project"
   - Navigate to: `/home/luke/Desktop/repositorios/fitness-cat-backend`
   - Click "OK"

2. **Gradle Sync**:
   - Android Studio will detect it's an Android project
   - A popup will say "Gradle files have changed since last project sync"
   - Click "Sync Now" (or it will auto-sync)
   - Wait for Gradle to download dependencies (first time: 5-15 minutes)

## Step 4: Set Up Android SDK (if needed)

If prompted about missing SDK components:

1. Go to: **File > Settings** (or `Ctrl+Alt+S`)
2. Navigate to: **Appearance & Behavior > System Settings > Android SDK**
3. In **SDK Platforms** tab:
   - Check "Android 14.0 (API 34)" or latest
   - Check "Android 7.0 (API 24)" - minimum for this app
4. In **SDK Tools** tab, ensure these are checked:
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
   - Android SDK Command-line Tools
5. Click **Apply** and wait for installation

## Step 5: Set Up Device/Emulator

### Option A: Use Physical Device (Recommended for step tracking)

1. **Enable Developer Options** on your Android phone:
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings > Developer Options
   - Enable "USB Debugging"

2. **Connect Phone**:
   - Connect phone via USB
   - On phone, allow USB debugging when prompted
   - In Android Studio, your device should appear in the device dropdown

### Option B: Create Android Emulator

1. Click **Device Manager** icon (phone icon in toolbar) or **Tools > Device Manager**

2. Click **Create Device**

3. Choose a device (e.g., "Pixel 5")

4. Click **Next**, then choose a system image:
   - Select "API 34" or latest (download if needed)
   - Click **Next** and **Finish**

5. Click the **Play** button next to your emulator to start it

## Step 6: Build and Run

1. **Select Device**: 
   - In the toolbar, click the device dropdown (next to the green play button)
   - Select your phone or emulator

2. **Build Project**:
   - Click **Build > Make Project** (or `Ctrl+F9`)
   - Wait for build to complete (check bottom status bar)

3. **Run App**:
   - Click the green **Run** button (▶️) in toolbar
   - Or press `Shift+F10`
   - Or right-click `app` folder > **Run 'app'**

4. **First Launch**:
   - App will install on device
   - Grant "Activity Recognition" permission when prompted
   - App should display your step count!

## Common Issues & Solutions

### "SDK location not found"
- Go to **File > Settings > Appearance & Behavior > System Settings > Android SDK**
- Note the "Android SDK Location" path
- Create `local.properties` file in project root:
  ```properties
  sdk.dir=/path/to/Android/Sdk
  ```

### "Gradle sync failed"
- Click **File > Invalidate Caches / Restart**
- Choose "Invalidate and Restart"
- Wait for re-indexing

### "Build failed" or dependency errors
- Click **File > Sync Project with Gradle Files**
- Or: **Build > Clean Project**, then **Build > Rebuild Project**

### Device not detected
- For physical device: Check USB debugging is enabled
- For emulator: Make sure it's running (you'll see it in Device Manager)

### "Step counter not working"
- Ensure device has step counter sensor (most modern phones do)
- Check permission was granted: Settings > Apps > Fitness Cat > Permissions

## Android Studio vs VS Code - Key Differences

| VS Code | Android Studio |
|---------|----------------|
| Extensions | Built-in Android tools |
| Terminal | Built-in terminal (bottom panel) |
| File Explorer | Project view (left sidebar) |
| Run button | Green play button (top toolbar) |
| Settings | File > Settings (not JSON) |
| Command Palette | Double Shift (search everywhere) |

## Useful Android Studio Shortcuts

- `Ctrl+Alt+S` - Settings
- `Shift+F10` - Run app
- `Ctrl+F9` - Build project
- `Ctrl+Shift+F` - Search in files
- `Alt+1` - Show/hide Project view
- `Alt+6` - Show/hide Logcat (for debugging)
- `Double Shift` - Search everywhere

## Project Structure in Android Studio

- **Left sidebar**: Project view (similar to VS Code explorer)
- **Center**: Code editor (same as VS Code)
- **Bottom**: 
  - **Build** tab: Build output
  - **Logcat** tab: App logs (like console.log)
  - **Terminal** tab: Command line
- **Top toolbar**: Run, debug, device selector

## Next Steps After App Runs

1. **View Logs**: Bottom panel > **Logcat** tab (filter by "DynamoDBHelper" to see sync logs)
2. **Debug**: Set breakpoints, click debug button (bug icon) instead of run
3. **Modify Code**: Edit Kotlin files, click run again to see changes
4. **Check DynamoDB**: Go to AWS Console to see your step records

## Tips

- **Gradle sync** happens automatically when you change `build.gradle`
- **Auto-import** is enabled by default (like VS Code)
- **Code completion** works similarly to VS Code (Ctrl+Space)
- **Git integration** is built-in (VCS menu)

