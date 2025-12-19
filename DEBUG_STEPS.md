# Debugging Step Tracking Issues

## Changes Made

I've added comprehensive logging to help diagnose why steps aren't being recorded. The logs will show:

1. **Service startup**: Whether the service starts, permissions, sensor availability
2. **Sensor registration**: Whether the sensor listener is registered successfully
3. **Sensor events**: Every time the sensor fires (with detailed information)
4. **Step calculations**: How steps are being calculated and added

## How to Debug

### 1. Check Logcat

Run this command to see all step tracking logs:

```bash
adb logcat | grep -E "(StepTrackingService|MainActivity)"
```

Or use Android Studio's Logcat with filter: `StepTrackingService|MainActivity`

### 2. What to Look For

#### Service Starting
Look for:
```
StepTrackingService: Starting step tracking - permission: granted, sensor: available
StepTrackingService: ✓ Successfully registered step counter sensor listener
```

If you see:
```
StepTrackingService: ✗ Activity Recognition permission not granted
```
→ **Fix**: Grant Activity Recognition permission in app settings

If you see:
```
StepTrackingService: ✗ Step counter sensor not available
```
→ **Fix**: Your device may not have a step counter sensor (hardware limitation)

#### Sensor Events
When you walk, you should see:
```
StepTrackingService: === SENSOR EVENT ===
StepTrackingService: Current sensor value: 1234.0
StepTrackingService: Steps since last reading: 5
StepTrackingService: ✓ Added 5 new steps!
```

If you **never** see "SENSOR EVENT", the sensor isn't firing. This could mean:
- The sensor isn't working
- You need to actually walk (TYPE_STEP_COUNTER only fires when steps change)
- The device doesn't have a step counter sensor

### 3. Important Notes

**TYPE_STEP_COUNTER Behavior:**
- This sensor only fires events when the step count **changes**
- If you're not walking, no events will fire
- When you first register the listener, it should fire once with the current cumulative value
- The sensor provides cumulative steps since device boot, not daily steps

**First Reading:**
- When the app first starts, if the sensor already has steps (e.g., 1000 steps since boot), those steps are **not counted**
- Only steps taken **after** the app starts tracking will be counted
- This is intentional - we only want to track steps after the app is installed

### 4. Testing Steps

1. **Check if service is running:**
   - Look for the notification "Step Tracker Running"
   - If no notification, the service isn't running

2. **Check permissions:**
   - Go to Settings > Apps > Fitness Cat > Permissions
   - Ensure "Physical activity" (Activity Recognition) is granted

3. **Test sensor:**
   - Walk around for a few steps
   - Check logcat for "SENSOR EVENT" messages
   - If no events appear, the sensor might not be working

4. **Check stored values:**
   - The logs will show: `Current state - totalSteps: X, lastSensorValue: Y`
   - If `lastSensorValue` is 0, the sensor hasn't fired yet
   - If `totalSteps` is 0, no steps have been recorded

### 5. Common Issues

**Issue: Steps show 0 and never increase**
- **Cause**: Sensor isn't firing events
- **Check**: Look for "SENSOR EVENT" in logs
- **Fix**: Walk around, ensure sensor is available, check permissions

**Issue: Service stops immediately**
- **Cause**: Permission denied or sensor unavailable
- **Check**: Look for "stopping service" messages in logs
- **Fix**: Grant permissions, check if device has step counter sensor

**Issue: Steps increase but UI doesn't update**
- **Cause**: UI refresh issue
- **Check**: Look for "Refreshed step count" in logs
- **Fix**: Already fixed - UI now refreshes every 2 seconds in onResume

### 6. Manual Testing

To test if the sensor is working:

1. Clear app data (Settings > Apps > Fitness Cat > Storage > Clear Data)
2. Open the app
3. Grant permissions
4. Walk around
5. Check logcat for sensor events

If you see sensor events but steps aren't increasing, there's a logic bug.
If you don't see sensor events at all, the sensor isn't working or isn't available.

## Next Steps

After checking the logs, you should be able to identify:
- ✅ Is the service running?
- ✅ Are permissions granted?
- ✅ Is the sensor available?
- ✅ Is the sensor firing events?
- ✅ Are steps being calculated correctly?

Share the logcat output and I can help identify the specific issue!


