# Quick Start Guide

This guide will help you get the Fitness Cat Step Tracker app running quickly.

## Prerequisites Check

✅ Android Studio (Arctic Fox or later)  
✅ Android device or emulator (API 24+)  
✅ AWS Account

## Step 1: Set Up AWS DynamoDB Table

Run the provided Python script to create the DynamoDB table:

```bash
# Install boto3 if needed
pip install boto3

# Configure AWS credentials (if not already done)
aws configure

# Create the table
python dynamodb_setup.py --region us-east-1
```

Or manually create the table in AWS Console:
- Table name: `StepRecords`
- Partition key: `userId` (String)
- Sort key: `timestamp` (Number)

## Step 2: Configure AWS Credentials in App

1. Copy the template file:
   ```bash
   cp app/src/main/assets/aws_config.properties.template app/src/main/assets/aws_config.properties
   ```

2. Edit `app/src/main/assets/aws_config.properties`:
   ```properties
   aws_access_key_id=YOUR_ACCESS_KEY_HERE
   aws_secret_access_key=YOUR_SECRET_KEY_HERE
   aws_region=us-east-1
   ```

## Step 3: Open in Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to this directory (`fitness-cat-backend`)
4. Wait for Gradle sync to complete

## Step 4: Build and Run

1. Connect your Android device (with USB debugging enabled) or start an emulator
2. Click "Run" (green play button) or press `Shift+F10`
3. Grant Activity Recognition permission when prompted
4. The app will display your step count and allow you to sync to DynamoDB

## Testing

1. **Check Step Tracking**: Walk around and open the app again - steps should update
2. **Test Cloud Sync**: Tap "Sync to Cloud" button - you should see a success message
3. **Verify in DynamoDB**: Check your AWS DynamoDB console to see the saved records

## Troubleshooting

- **Build errors?** Try: `File > Invalidate Caches / Restart`
- **No steps showing?** Ensure your device has a step counter sensor
- **Sync failing?** Check AWS credentials and DynamoDB table exists
- **Permission denied?** Go to Settings > Apps > Fitness Cat > Permissions and enable Activity Recognition

## Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Explore the Kotlin code to understand Android development
- Customize the UI or add features like step history graphs




