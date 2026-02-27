# Cloud Sync Summary - Fitness Cat App

## 📤 What the App Sends to the Cloud

### 1. **Single Step Record** (Individual Sync)
- **Format**: JSON object
- **Method**: HTTP POST
- **Endpoint**: `https://qdt4w3wkfj.execute-api.us-east-1.amazonaws.com/prod/steps`
- **Content-Type**: `application/json`

**JSON Structure:**
```json
{
  "user_id": "android_id_timestamp",
  "steps": 1234,
  "timestamp": 1704123456
}
```

**Field Details:**
- `user_id`: String - Unique device ID (Android ID + timestamp)
- `steps`: Integer - Total cumulative step count at that moment
- `timestamp`: Integer - Unix timestamp in **seconds** (not milliseconds)

---

### 2. **Batch Step Records** (Batch Sync)
- **Format**: JSON object with array of records
- **Method**: HTTP POST
- **Endpoint**: Same as single sync: `https://qdt4w3wkfj.execute-api.us-east-1.amazonaws.com/prod/steps`
- **Content-Type**: `application/json`

**JSON Structure:**
```json
{
  "user_id": "android_id_timestamp",
  "records": [
    {
      "steps_at_time": 100,
      "timestamp": 1704123456
    },
    {
      "steps_at_time": 250,
      "timestamp": 1704125256
    },
    {
      "steps_at_time": 400,
      "timestamp": 1704127056
    }
  ]
}
```

**Field Details:**
- `user_id`: String - Same unique device ID
- `records`: Array of objects, each containing:
  - `steps_at_time`: Integer - Step count at that specific timestamp
  - `timestamp`: Integer - Unix timestamp in **seconds**

---

## ⏰ Frequency & Triggers

### **Single Sync** (`syncSteps()`)
| Trigger | Frequency | When |
|---------|-----------|------|
| **Automatic (Hourly)** | Every **1 hour** | Only while app is **open/foreground** |
| **App Close** | Once per close | When app goes to background (`onStop()`) |
| **Manual Button** | On demand | User clicks "Sync" button |

**Notes:**
- Hourly sync **stops** when app goes to background
- App close sync happens **every time** app goes to background
- Manual sync **bypasses** all rate limiting

---

### **Batch Sync** (`syncStepsBatch()`)
| Trigger | Frequency | When |
|---------|-----------|------|
| **App Open** | Once per open | When app comes to foreground (`onResume()`) |

**Notes:**
- Sends **all pending records** accumulated since last sync
- Clears pending records **only on success**
- If sync fails, records remain for next retry

---

### **Background Step Recording** (`StepWorker`)
| Action | Frequency | When |
|---------|-----------|------|
| **Save to Local** | Every **30 minutes** | Runs in background via WorkManager |

**Notes:**
- **Does NOT send to cloud** - only saves locally
- Records accumulate in `SharedPreferences` as JSON array
- Records are sent when app opens (batch sync)

---

## 📊 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    BACKGROUND (App Closed)                   │
│                                                              │
│  StepWorker (every 30 min)                                  │
│    ↓                                                         │
│  Read Sensor → Calculate Steps                              │
│    ↓                                                         │
│  Save to Local Storage (pending records)                    │
│    [{"steps_at_time": 100, "timestamp": 1234}, ...]       │
└─────────────────────────────────────────────────────────────┘
                          ↓
                    (App Opens)
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                  FOREGROUND (App Open)                       │
│                                                              │
│  1. Batch Sync (onResume)                                   │
│     → Send all pending records                              │
│     → Clear on success                                      │
│                                                              │
│  2. Hourly Sync (every 1 hour while open)                  │
│     → Send current total step count                         │
│                                                              │
│  3. App Close Sync (onStop)                                 │
│     → Send current total step count                         │
│                                                              │
│  4. Manual Sync (button click)                              │
│     → Send current total step count                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔢 Example Numbers

### **Single Sync Example:**
```json
{
  "user_id": "a1b2c3d4e5f6_1704123456789",
  "steps": 5432,
  "timestamp": 1704123456
}
```

### **Batch Sync Example:**
```json
{
  "user_id": "a1b2c3d4e5f6_1704123456789",
  "records": [
    {"steps_at_time": 1200, "timestamp": 1704120000},
    {"steps_at_time": 1350, "timestamp": 1704121800},
    {"steps_at_time": 1500, "timestamp": 1704123600},
    {"steps_at_time": 1650, "timestamp": 1704125400}
  ]
}
```

**Typical Batch Size:**
- If app closed for 2 hours: **4 records** (30 min intervals)
- If app closed for 6 hours: **12 records**
- Maximum practical: ~48 records (24 hours)

---

## 📈 Summary Table

| Sync Type | Frequency | Data Format | Records per Request |
|-----------|-----------|-------------|---------------------|
| **Single** | 1 hour (while open) | `{user_id, steps, timestamp}` | 1 |
| **Single** | On app close | `{user_id, steps, timestamp}` | 1 |
| **Single** | Manual button | `{user_id, steps, timestamp}` | 1 |
| **Batch** | On app open | `{user_id, records: [...]}` | 1-N (all pending) |

---

## 🔧 Technical Details

### **HTTP Request Headers:**
```
Content-Type: application/json
```

### **Response Expected:**
```json
{
  "ok": true
}
```

### **Error Handling:**
- Failed syncs are logged but don't block app
- Batch sync failures keep records for retry
- Single sync failures show toast notification

### **Local Storage:**
- Pending records stored in `SharedPreferences` as JSON string
- Key: `pending_step_records`
- Format: `[{"steps_at_time": 100, "timestamp": 1234}, ...]`

---

## ⚠️ Important Notes

1. **Timestamp Format**: All timestamps are in **Unix seconds** (not milliseconds)
2. **Step Count**: Always **cumulative total**, not incremental
3. **Batch Records**: `steps_at_time` is the total at that moment, not delta
4. **Network**: Batch sync requires network connection (no offline queue)
5. **WorkManager**: StepWorker runs even when app is closed (Android background limits apply)


