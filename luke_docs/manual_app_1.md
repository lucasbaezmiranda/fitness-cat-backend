---

**User**

in which files are like... the main parts? I mean i dont wanna know low level kotlin but at least understand it. I come from 6 years exp with python and 4 years in javascript, also worked 2 years in c++ for algo trading im a computer engineer so i have some good background. I would like you first to tell me which the main files are and what does everything do, like, first the forest then the trees, so i can help debug

---

**Cursor**

Explorando la estructura del proyecto para darte una visión general:


## THE FOREST: High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Android App                          │
│                                                         │
│  ┌──────────────┐         ┌──────────────────────┐    │
│  │ MainActivity │────────▶│ StepTrackingService  │    │
│  │  (UI Layer)  │ starts  │ (Background Worker) │    │
│  └──────┬───────┘         └──────────┬───────────┘    │
│         │                            │                 │
│         │                            │                 │
│         ▼                            ▼                 │
│  ┌──────────────┐         ┌──────────────────────┐    │
│  │ StepCounter  │         │   UserPreferences     │    │
│  │  (Sensor)    │────────▶│   (Local Storage)     │    │
│  └──────────────┘         └──────────┬───────────┘    │
│                                     │                 │
│                                     │                 │
│  ┌──────────────┐                   │                 │
│  │  ApiClient  │◀───────────────────┘                 │
│  │  (HTTP)     │                                       │
│  └──────────────┘                                       │
│         │                                               │
│         ▼                                               │
│  AWS API Gateway → Lambda → DynamoDB                   │
└─────────────────────────────────────────────────────────┘
```

## THE TREES: Main Files Explained

### 1. MainActivity.kt — Entry point (like `main()` or `app.py`)

Purpose: UI controller and app lifecycle.

Responsibilities:
- Handles permissions
- Shows step count on screen
- Starts the background service
- Triggers API sync when app opens

Key concepts:
- `onCreate()`: runs once when app starts (like `__init__`)
- `onResume()`: runs when app comes to foreground (when user opens it)
- `lifecycleScope.launch`: async operations (like `async/await` in JS)

When to look here: UI issues, permission problems, app crashes on startup.

---

### 2. StepTrackingService.kt — Background worker (like a daemon process)

Purpose: Runs continuously in the background, even when the app is closed.

Responsibilities:
- Listens to step sensor 24/7
- Updates step count in storage
- Shows a notification (required for foreground services)

Key concepts:
- Foreground Service: Android service that shows a notification (required for background work)
- `onSensorChanged()`: callback when sensor detects new steps (like an event listener)
- `START_STICKY`: Android restarts the service if it gets killed

When to look here: Steps not counting when app is closed, service crashes, notification issues.

---

### 3. StepCounter.kt — Sensor wrapper (like a hardware abstraction layer)

Purpose: Wraps Android's step sensor and calculates total steps.

Responsibilities:
- Registers/unregisters sensor listener
- Calculates steps from sensor readings
- Handles device reboots (sensor resets to 0)

Key concepts:
- `SensorEventListener`: interface for sensor callbacks
- Sensor provides cumulative steps since boot, not total lifetime
- Reboot handling: when device reboots, sensor resets, so we track the difference

When to look here: Steps counting incorrectly, missing steps after reboot, sensor not working.

---

### 4. UserPreferences.kt — Local storage (like a simple database)

Purpose: Persistent key-value storage (like `localStorage` in JS or a config file).

Responsibilities:
- Stores user ID (generates if first time)
- Stores total step count
- Stores last sensor reading
- Stores last API sync timestamp

Key concepts:
- `SharedPreferences`: Android's key-value storage (like a JSON file)
- `apply()`: async save (non-blocking)
- Data persists across app restarts

When to look here: Data not persisting, user ID issues, sync timestamp problems.

---

### 5. ApiClient.kt — HTTP client (like `requests` in Python or `fetch` in JS)

Purpose: Sends step data to your AWS API Gateway.

Responsibilities:
- Builds JSON request body
- Makes POST request to API Gateway
- Handles success/error responses

Key concepts:
- `OkHttpClient`: HTTP client library (like `requests` in Python)
- `CoroutineScope(Dispatchers.IO)`: runs on background thread (like `threading` or `asyncio`)
- Callback pattern: `callback: ((Boolean, String?) -> Unit)?` (like a promise callback)

When to look here: API calls failing, network errors, wrong data format.

---

## DATA FLOW: How Everything Works Together

### Scenario 1: User opens app for first time

```
1. MainActivity.onCreate()
   ├─> Request permissions (ACTIVITY_RECOGNITION)
   ├─> Initialize UserPreferences → generates user_id
   ├─> Initialize StepCounter → loads saved steps (0)
   └─> Start StepTrackingService

2. StepTrackingService.onCreate()
   ├─> Load saved step count from UserPreferences
   ├─> Register sensor listener
   └─> Show notification

3. Sensor detects steps → onSensorChanged()
   ├─> Calculate: new_steps = current_sensor_value - last_sensor_value
   ├─> Update: total_steps += new_steps
   ├─> Save to UserPreferences
   └─> Update notification

4. MainActivity.onResume() (when user opens app)
   ├─> Refresh UI with current steps
   └─> syncStepsToAPI() → POST to AWS
```

### Scenario 2: User closes app, walks, then reopens

```
1. App closed → StepTrackingService still running
   └─> Sensor keeps counting → saves to UserPreferences

2. User reopens app → MainActivity.onResume()
   ├─> Reads current steps from UserPreferences
   ├─> Updates UI
   └─> syncStepsToAPI() (if 5+ minutes since last sync)
```

---

## KEY ANDROID/KOTLIN CONCEPTS (for Python/JS/C++ devs)

### 1. Lifecycle methods (like React hooks or Flask routes)
```kotlin
onCreate()   // App starts (like __init__)
onResume()   // App comes to foreground (like componentDidMount)
onPause()    // App goes to background
onDestroy()  // App closes
```

### 2. Coroutines (like async/await in JS or asyncio in Python)
```kotlin
lifecycleScope.launch {  // Like: async function
    val result = apiCall()  // Runs on background thread
    updateUI(result)  // Switch back to UI thread
}
```

### 3. Callbacks (like promises or function pointers)
```kotlin
fun doSomething(callback: (Boolean, String?) -> Unit) {
    // Like: def do_something(callback: Callable)
    callback(true, "success")
}
```

### 4. Null safety (Kotlin's `?` operator)
```kotlin
val value: String? = null  // Can be null
value?.length  // Safe call (like: value?.length in JS)
value ?: "default"  // Elvis operator (like: value || "default")
```

### 5. Data classes (like dataclasses in Python)
```kotlin
data class User(val id: String, val steps: Int)
// Like: @dataclass in Python
```

---

## DEBUGGING GUIDE: Where to Look

| Problem | Check This File First |
|---------|----------------------|
| App crashes on startup | `MainActivity.kt` (permissions, initialization) |
| Steps not counting | `StepTrackingService.kt` (sensor listener) |
| Steps lost after reboot | `StepCounter.kt` (reboot handling logic) |
| API call fails | `ApiClient.kt` (network, JSON format) |
| Data not saving | `UserPreferences.kt` (SharedPreferences) |
| UI not updating | `MainActivity.kt` (onResume, UI thread) |

---

## QUICK REFERENCE: File Sizes & Complexity

- MainActivity.kt: ~225 lines — Medium complexity (lifecycle, permissions)
- StepTrackingService.kt: ~220 lines — Medium complexity (service, sensor)
- StepCounter.kt: ~140 lines — Low complexity (sensor wrapper)
- UserPreferences.kt: ~70 lines — Low complexity (storage wrapper)
- ApiClient.kt: ~140 lines — Low complexity (HTTP client)

---

## NEXT STEPS

1. Start with `MainActivity.kt` to understand the flow
2. Then `StepTrackingService.kt` for background tracking
3. `UserPreferences.kt` for data persistence
4. `ApiClient.kt` for API integration
5. `StepCounter.kt` for sensor logic

If you want, puedo explicar cualquier archivo en detalle o ayudarte a debuggear un problema específico.

```plaintext
┌─────────────────────────────────────────────────────────┐
│                    Android App                          │
│                                                         │
│  ┌──────────────┐         ┌──────────────────────┐    │
│  │ MainActivity │────────▶│ StepTrackingService  │    │
│  │  (UI Layer)  │ starts  │ (Background Worker) │    │
│  └──────┬───────┘         └──────────┬───────────┘    │
│         │                            │                 │
│         │                            │                 │
│         ▼                            ▼                 │
│  ┌──────────────┐         ┌──────────────────────┐    │
│  │ StepCounter  │         │   UserPreferences     │    │
│  │  (Sensor)    │────────▶│   (Local Storage)     │    │
│  └──────────────┘         └──────────┬───────────┘    │
│                                     │                 │
│                                     │                 │
│  ┌──────────────┐                   │                 │
│  │  ApiClient  │◀───────────────────┘                 │
│  │  (HTTP)     │                                       │
│  └──────────────┘                                       │
│         │                                               │
│         ▼                                               │
│  AWS API Gateway → Lambda → DynamoDB                   │
└─────────────────────────────────────────────────────────┘
```

```plaintext
1. MainActivity.onCreate()
   ├─> Request permissions (ACTIVITY_RECOGNITION)
   ├─> Initialize UserPreferences → generates user_id
   ├─> Initialize StepCounter → loads saved steps (0)
   └─> Start StepTrackingService

2. StepTrackingService.onCreate()
   ├─> Load saved step count from UserPreferences
   ├─> Register sensor listener
   └─> Show notification

3. Sensor detects steps → onSensorChanged()
   ├─> Calculate: new_steps = current_sensor_value - last_sensor_value
   ├─> Update: total_steps += new_steps
   ├─> Save to UserPreferences
   └─> Update notification

4. MainActivity.onResume() (when user opens app)
   ├─> Refresh UI with current steps
   └─> syncStepsToAPI() → POST to AWS
```

```plaintext
1. App closed → StepTrackingService still running
   └─> Sensor keeps counting → saves to UserPreferences

2. User reopens app → MainActivity.onResume()
   ├─> Reads current steps from UserPreferences
   ├─> Updates UI
   └─> syncStepsToAPI() (if 5+ minutes since last sync)
```

```plaintext
onCreate()   // App starts (like __init__)
onResume()   // App comes to foreground (like componentDidMount)
onPause()    // App goes to background
onDestroy()  // App closes
```

```plaintext
lifecycleScope.launch {  // Like: async function
    val result = apiCall()  // Runs on background thread
    updateUI(result)  // Switch back to UI thread
}
```

```plaintext
fun doSomething(callback: (Boolean, String?) -> Unit) {
    // Like: def do_something(callback: Callable)
    callback(true, "success")
}
```

```plaintext
val value: String? = null  // Can be null
value?.length  // Safe call (like: value?.length in JS)
value ?: "default"  // Elvis operator (like: value || "default")
```

```plaintext
data class User(val id: String, val steps: Int)
// Like: @dataclass in Python
```
