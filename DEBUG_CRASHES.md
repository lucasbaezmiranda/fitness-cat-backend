# How to Debug Crashes in Android Studio

## Viewing Crash Logs (Logcat)

1. **Open Logcat**:
   - In Android Studio, look at the **bottom panel**
   - Click the **"Logcat"** tab
   - If you don't see it: **View > Tool Windows > Logcat** (or press `Alt+6`)

2. **Filter Logs**:
   - In the Logcat search box, type: `FATAL` or `AndroidRuntime`
   - This shows only crash/error messages
   - Or filter by package: `com.fitnesscat.stepstracker`

3. **Run the App**:
   - Click the green **Run** button (▶️)
   - Watch Logcat for errors

4. **Common Crash Indicators**:
   - Look for lines starting with `FATAL EXCEPTION`
   - Look for `java.lang.RuntimeException` or `SecurityException`
   - The error message will tell you what went wrong

## Example: What a Crash Looks Like

```
FATAL EXCEPTION: main
Process: com.fitnesscat.stepstracker, PID: 12345
java.lang.SecurityException: Starting FGS with type health requires permission...
```

## Common Crash Causes & Fixes

### 1. Permission Not Granted
**Error**: `SecurityException: Permission denied`
**Fix**: The app now checks permissions before starting the service

### 2. Foreground Service Type Error
**Error**: `ForegroundServiceType not allowed`
**Fix**: Added fallback for older Android versions

### 3. Sensor Not Available
**Error**: `Sensor not found` or `NullPointerException`
**Fix**: Added null checks and error handling

## Testing the Fixes

The updated code now:
- ✅ Checks permissions before starting service
- ✅ Handles errors gracefully (won't crash)
- ✅ Shows error messages in Toast notifications
- ✅ Logs errors to Logcat for debugging

## Steps to Test

1. **Clean Build**:
   - **Build > Clean Project**
   - **Build > Rebuild Project**

2. **Run on Device**:
   - Connect your phone
   - Click **Run** (▶️)
   - Grant permissions when asked
   - Check Logcat for any errors

3. **If It Still Crashes**:
   - Copy the error from Logcat
   - The error message will tell us exactly what's wrong

## Quick Logcat Commands

You can also use terminal commands:

```bash
# View all logs
adb logcat

# Filter for errors only
adb logcat *:E

# Filter for our app only
adb logcat | grep fitnesscat

# Clear logs
adb logcat -c
```

## What to Look For

When the app crashes, look for:
1. **Exception type**: `SecurityException`, `NullPointerException`, etc.
2. **Error message**: What it says went wrong
3. **Stack trace**: Which file/line caused the crash

The updated code should prevent most crashes by:
- Checking permissions first
- Handling null values
- Catching exceptions
- Showing user-friendly error messages





