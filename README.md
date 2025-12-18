# Fitness Cat - Step Tracker

A Kotlin Android application that tracks your daily steps and syncs them to AWS DynamoDB. Each time you open the app, you'll see your accumulated steps, and you can sync them to the cloud with a single tap.

## Features

- 📱 **Step Tracking**: Uses Android's built-in step counter sensor to track your steps
- ☁️ **Cloud Sync**: Syncs step data to AWS DynamoDB for each user
- 👤 **User Identification**: Automatically generates and stores a unique user ID
- ⏰ **Timestamp Tracking**: Records the exact time when the app is opened and synced
- 💾 **Local Storage**: Persists step count and sync status locally

## Architecture

- **Language**: Kotlin
- **UI**: Android Views with ViewBinding
- **Step Tracking**: Android SensorManager with TYPE_STEP_COUNTER
- **Cloud Storage**: AWS DynamoDB
- **Async Operations**: Kotlin Coroutines
- **Local Storage**: Android SharedPreferences

## Prerequisites

1. **Android Studio** (Arctic Fox or later recommended)
2. **JDK 8+**
3. **Android device or emulator** with:
   - Android 7.0 (API level 24) or higher
   - Step counter sensor (hardware requirement)
4. **AWS Account** with DynamoDB access

## Setup Instructions

### 1. Clone and Open Project

```bash
cd fitness-cat-backend
```

Open the project in Android Studio. Let Gradle sync complete.

### 2. Configure AWS DynamoDB

#### Create DynamoDB Table

1. Log in to AWS Console
2. Navigate to DynamoDB
3. Create a new table with the following configuration:
   - **Table name**: `StepRecords`
   - **Partition key**: `userId` (String)
   - **Sort key**: `timestamp` (Number)
   - Choose your preferred settings for capacity and encryption

#### Configure AWS Credentials

1. Copy the template file:
   ```bash
   cp app/src/main/assets/aws_config.properties.template app/src/main/assets/aws_config.properties
   ```

2. Edit `app/src/main/assets/aws_config.properties` and add your AWS credentials:
   ```properties
   aws_access_key_id=YOUR_ACCESS_KEY_ID
   aws_secret_access_key=YOUR_SECRET_ACCESS_KEY
   aws_region=us-east-1
   ```

   **⚠️ Security Note**: For production apps, use AWS Cognito for authentication instead of storing credentials directly. This file is already in `.gitignore` to prevent accidental commits.

#### Update AWS Region (Optional)

If your DynamoDB table is in a different region, update the region in `DynamoDBHelper.kt`:

```kotlin
setRegion(Region.getRegion(Regions.US_EAST_1)) // Change to your region
```

### 3. Build and Run

1. Connect your Android device or start an emulator
2. Build the project: `Build > Make Project`
3. Run the app: `Run > Run 'app'`

### 4. Grant Permissions

When you first launch the app, you'll be prompted to grant **Activity Recognition** permission. This is required for step tracking.

## Project Structure

```
app/src/main/
├── java/com/fitnesscat/stepstracker/
│   ├── MainActivity.kt          # Main UI and app lifecycle
│   ├── StepCounter.kt           # Step tracking logic using SensorManager
│   ├── DynamoDBHelper.kt        # AWS DynamoDB integration
│   ├── StepRecord.kt            # Data model for step records
│   └── UserPreferences.kt       # Local storage for user ID and preferences
├── res/
│   ├── layout/
│   │   └── activity_main.xml    # Main UI layout
│   └── values/
│       └── strings.xml          # String resources
└── assets/
    └── aws_config.properties    # AWS credentials (create from template)
```

## How It Works

1. **Step Tracking**:
   - Uses Android's `Sensor.TYPE_STEP_COUNTER` which provides cumulative steps since device boot
   - Tracks offset to calculate daily steps correctly
   - Persists step count to SharedPreferences

2. **User Identification**:
   - Generates a unique user ID based on Android ID + timestamp on first launch
   - Stores user ID in SharedPreferences for future use

3. **Cloud Sync**:
   - When "Sync to Cloud" button is tapped, creates a `StepRecord` with:
     - `userId`: Unique user identifier
     - `steps`: Current step count
     - `timestamp`: Current time in milliseconds
     - `date`: ISO date string (YYYY-MM-DD) for easier querying
   - Saves record to DynamoDB table `StepRecords`

4. **DynamoDB Schema**:
   ```
   Table: StepRecords
   Partition Key: userId (String)
   Sort Key: timestamp (Number)
   Attributes:
     - steps (Number)
     - date (String)
   ```

## DynamoDB Query Examples

### Get all records for a user:
```python
import boto3

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('StepRecords')

response = table.query(
    KeyConditionExpression='userId = :uid',
    ExpressionAttributeValues={
        ':uid': 'user_id_here'
    }
)
```

### Get records for a specific date:
```python
response = table.query(
    KeyConditionExpression='userId = :uid',
    FilterExpression='#date = :date',
    ExpressionAttributeNames={'#date': 'date'},
    ExpressionAttributeValues={
        ':uid': 'user_id_here',
        ':date': '2024-01-15'
    }
)
```

## Learning Kotlin Notes

Since you're coming from Python, here are some Kotlin concepts used in this project:

- **Data Classes**: `data class StepRecord(...)` - similar to Python dataclasses, automatically generates `equals()`, `hashCode()`, `toString()`, and copy methods
- **Coroutines**: `lifecycleScope.launch { ... }` - similar to Python async/await, for non-blocking operations
- **Null Safety**: `?.` (safe call operator) and `?:` (Elvis operator) - Kotlin's null safety features
- **Extension Functions**: Kotlin allows adding functions to existing classes
- **Lambda Expressions**: `{ param -> body }` - similar to Python lambdas
- **Property Delegates**: `lateinit var` - for properties initialized later

## Troubleshooting

### Steps not updating?
- Ensure the device has a step counter sensor (most modern phones do)
- Check that Activity Recognition permission is granted
- Try restarting the app or device

### DynamoDB sync failing?
- Verify AWS credentials in `aws_config.properties`
- Check that the DynamoDB table exists and is in the correct region
- Ensure your AWS credentials have DynamoDB write permissions
- Check Android logcat for error messages: `adb logcat | grep DynamoDBHelper`

### Build errors?
- Ensure all Gradle dependencies are downloaded
- Clean and rebuild: `Build > Clean Project`, then `Build > Rebuild Project`
- Check that you're using a compatible Android SDK version

## Next Steps / Enhancements

- [ ] Add AWS Cognito for secure authentication (instead of storing credentials)
- [ ] Implement daily step goals and progress visualization
- [ ] Add charts/graphs to visualize step history
- [ ] Implement automatic background sync
- [ ] Add data export functionality
- [ ] Support multiple date ranges in queries
- [ ] Add step history view in the app

## License

This project is created for learning purposes.




