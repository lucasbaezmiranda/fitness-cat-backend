# Debugging Batch Sync Issues

## 🔍 Problema Reportado

La app solo está guardando un único registro cuando se abre, en lugar de múltiples records acumulados cada 30 minutos.

## 📋 Cambios Realizados

Se agregaron logs detallados en los siguientes archivos para rastrear el flujo completo:

1. **StepWorker.kt** - Logs antes y después de guardar cada record
2. **UserPreferences.kt** - Logs del contenido completo del JSON array
3. **MainActivity.kt** - Logs detallados del batch sync
4. **ApiClient.kt** - Log del JSON completo que se envía a la API

## 🧪 Cómo Debuggear

### 1. Ver los Logs en Android Studio

Abre **Logcat** en Android Studio y filtra por estos tags:

```
StepWorker|UserPreferences|MainActivity|ApiClient
```

### 2. Logs Clave a Buscar

#### Cuando StepWorker guarda un record (cada 30 min):
```
StepWorker: About to save record (current pending count: X)
UserPreferences: Added pending record: steps=XXX, timestamp=YYY (total records: X+1)
UserPreferences: All pending records: [{"steps_at_time":XXX,"timestamp":YYY}, ...]
```

#### Cuando la app se abre y hace batch sync:
```
MainActivity: Raw pending JSON: [...]
MainActivity: Syncing X pending records in batch for user: ...
MainActivity: Record 0: steps_at_time=XXX, timestamp=YYY
MainActivity: Record 1: steps_at_time=XXX, timestamp=YYY
...
ApiClient: Sending batch sync: X records for user: ...
ApiClient: Batch JSON body: {"user_id":"...","records":[...]}
```

### 3. Verificar StepWorker está Corriendo

Para verificar que StepWorker se está ejecutando cada 30 minutos:

```bash
adb logcat | grep StepWorker
```

Deberías ver logs cada 30 minutos como:
```
StepWorker: StepWorker started - reading current step count
StepWorker: ✓ Saved step record: steps=XXX, timestamp=YYY
```

### 4. Verificar Records Pendientes

Para ver cuántos records hay acumulados localmente:

```bash
adb logcat | grep "All pending records"
```

Esto mostrará el JSON completo con todos los records acumulados.

### 5. Verificar Batch Sync

Cuando abres la app, deberías ver:

```bash
adb logcat | grep "Syncing.*pending records in batch"
```

Y luego:

```bash
adb logcat | grep "Batch JSON body"
```

Esto mostrará el JSON exacto que se envía a la Lambda.

## 🐛 Problemas Comunes

### Problema 1: StepWorker no está corriendo

**Síntoma:** No ves logs de StepWorker cada 30 minutos

**Solución:** 
- Verificar que WorkManager tiene permisos
- Verificar que el dispositivo no está en modo "Doze" o "App Standby"
- Revisar logs de WorkManager: `adb logcat | grep WorkManager`

### Problema 2: Solo hay 1 record en el batch

**Síntoma:** Logs muestran `Syncing 1 pending records in batch`

**Posibles causas:**
1. StepWorker solo ha corrido una vez
2. Los records se están limpiando prematuramente
3. El JSON se está sobreescribiendo en lugar de agregarse

**Debug:**
- Buscar `All pending records:` para ver el JSON completo
- Verificar que `addPendingStepRecord()` se llama múltiples veces
- Verificar que el JSON array tiene múltiples elementos

### Problema 3: La Lambda recibe múltiples records pero solo guarda uno

**Síntoma:** El log `Batch JSON body:` muestra múltiples records, pero DynamoDB solo tiene uno

**Solución:**
- Revisar la Lambda para ver si está procesando correctamente el array `records`
- Verificar que la Lambda itera sobre todos los elementos del array
- Revisar logs de CloudWatch de la Lambda

### Problema 4: Los records se están limpiando antes del sync

**Síntoma:** Los records aparecen en logs pero desaparecen antes del batch sync

**Debug:**
- Buscar llamadas a `clearPendingStepRecords()`
- Verificar que solo se limpia después de un sync exitoso
- Revisar si hay múltiples instancias de MainActivity limpiando los records

## 🔧 Verificar la Lambda

Para verificar que la Lambda está recibiendo y procesando correctamente:

1. Ve a **AWS CloudWatch** → **Log Groups** → Busca el log group de tu Lambda
2. Busca logs recientes cuando abres la app
3. Verifica que el `body` del evento contiene múltiples records
4. Verifica que la Lambda itera sobre todos los records

## 📊 Formato Esperado del JSON

### Lo que envía el app:
```json
{
  "user_id": "a1b2c3d4_1234567890",
  "records": [
    {"steps_at_time": 100, "timestamp": 1704123456},
    {"steps_at_time": 250, "timestamp": 1704125256},
    {"steps_at_time": 400, "timestamp": 1704127056}
  ]
}
```

### Lo que la Lambda debe procesar:
- Iterar sobre `body["records"]`
- Para cada record, usar `record.get("steps_at_time")` o `record.get("steps")`
- Guardar cada uno en DynamoDB con un timestamp único

## ✅ Checklist de Verificación

- [ ] StepWorker está corriendo cada 30 minutos (verificar logs)
- [ ] Los records se están acumulando en SharedPreferences (verificar `All pending records`)
- [ ] El batch sync se ejecuta cuando abres la app (verificar `Syncing X pending records`)
- [ ] El JSON enviado contiene múltiples records (verificar `Batch JSON body`)
- [ ] La Lambda recibe el request completo (verificar CloudWatch)
- [ ] La Lambda procesa todos los records (verificar código Lambda)
- [ ] DynamoDB tiene múltiples registros (verificar con `aws dynamodb scan`)

## 🚀 Comandos Útiles

### Ver todos los logs relevantes:
```bash
adb logcat | grep -E "StepWorker|UserPreferences|MainActivity.*batch|ApiClient.*batch"
```

### Ver solo logs de batch sync:
```bash
adb logcat | grep -E "Syncing.*pending records|Batch JSON body|Successfully synced.*records in batch"
```

### Ver records en DynamoDB:
```bash
aws dynamodb scan --table-name user_steps --region us-east-1 | jq '.Items[] | {user_id: .user_id.S, timestamp: .timestamp.N, steps: .steps.N}'
```

### Contar records en DynamoDB:
```bash
aws dynamodb scan --table-name user_steps --region us-east-1 --select COUNT
```


