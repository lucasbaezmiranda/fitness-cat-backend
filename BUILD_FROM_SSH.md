# Compilar APK desde SSH (sin Android Studio)

Guía completa para compilar la app Android en una máquina remota por SSH sin Android Studio.

## Requisitos Previos

- Acceso SSH a la máquina remota
- Java JDK 8 o superior
- ~5GB de espacio libre (para Android SDK)

## Paso 1: Instalar Android SDK Command Line Tools

```bash
# Crear directorio para Android SDK
mkdir -p ~/android-sdk
cd ~/android-sdk

# Descargar command line tools (Linux)
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip

# Extraer
unzip commandlinetools-linux-9477386_latest.zip

# Crear estructura de directorios
mkdir -p cmdline-tools
mv cmdline-tools cmdline-tools/latest
```

## Paso 2: Configurar Variables de Entorno

Agregar a `~/.bashrc` o `~/.zshrc`:

```bash
# Android SDK
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
```

Recargar:
```bash
source ~/.bashrc
```

## Paso 3: Instalar SDK Components Necesarios

```bash
# Aceptar licencias primero
yes | sdkmanager --licenses

# Instalar componentes necesarios
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

**Nota:** Puedes instalar múltiples versiones si necesitas compatibilidad:
```bash
sdkmanager "platforms;android-33" "platforms;android-34" "build-tools;33.0.0" "build-tools;34.0.0"
```

## Paso 4: Configurar local.properties en el Proyecto

```bash
cd /path/to/fitness-cat-backend

# Crear local.properties
cat > local.properties << EOF
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
sdk.dir=$HOME/android-sdk
EOF
```

## Paso 5: Compilar el APK

```bash
# Dar permisos al gradle wrapper
chmod +x gradlew

# Limpiar builds anteriores (opcional)
./gradlew clean

# Compilar APK debug
./gradlew assembleDebug

# O compilar APK release (requiere keystore configurado)
# ./gradlew assembleRelease
```

## Paso 6: Encontrar el APK Generado

El APK estará en:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Verificación Rápida

```bash
# Verificar que el APK existe
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Ver información del APK
file app/build/outputs/apk/debug/app-debug.apk
```

## Script Automatizado

Puedes usar el script `build_apk_ssh.sh` incluido en este repo:

```bash
chmod +x build_apk_ssh.sh
./build_apk_ssh.sh
```

## Troubleshooting

### Error: "SDK location not found"
- Verifica que `local.properties` existe y tiene `sdk.dir` configurado correctamente
- Verifica que el path en `local.properties` existe

### Error: "License not accepted"
```bash
yes | sdkmanager --licenses
```

### Error: "Command not found: sdkmanager"
- Verifica que agregaste `ANDROID_HOME/cmdline-tools/latest/bin` al PATH
- Recarga el shell: `source ~/.bashrc`

### Error: "No space left on device"
- Libera espacio o usa un directorio con más espacio
- Cambia `ANDROID_HOME` a otro disco si es necesario

### Error: "Gradle daemon not running"
- Es normal en la primera ejecución
- Gradle descargará dependencias automáticamente

## Optimizaciones para Compilación Remota

### Usar cache de Gradle compartido (opcional)

Si tienes múltiples proyectos, puedes configurar un cache global:

```bash
# En ~/.gradle/gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.daemon=true
org.gradle.configureondemand=true
```

### Compilar solo lo necesario

```bash
# Solo compilar sin tests
./gradlew assembleDebug -x test

# Compilar en modo offline (requiere dependencias ya descargadas)
./gradlew assembleDebug --offline
```

## Descargar APK desde SSH

Si quieres descargar el APK a tu máquina local:

```bash
# Desde tu máquina local (no SSH)
scp usuario@servidor:/path/to/fitness-cat-backend/app/build/outputs/apk/debug/app-debug.apk ./
```

## Build Release (Para Producción)

Para generar un APK release firmado, necesitas configurar un keystore:

```bash
# Generar keystore (solo primera vez)
keytool -genkey -v -keystore fitness-cat-release.keystore -alias fitnesscat -keyalg RSA -keysize 2048 -validity 10000

# Agregar a gradle.properties (NO subir a git)
echo "KEYSTORE_PASSWORD=tu_password" >> gradle.properties
echo "KEY_ALIAS=fitnesscat" >> gradle.properties
echo "KEY_PASSWORD=tu_password" >> gradle.properties
```

Luego compilar:
```bash
./gradlew assembleRelease
```

## Tiempo Estimado

- Primera compilación: 5-15 minutos (descarga dependencias)
- Compilaciones subsecuentes: 1-3 minutos

## Requisitos de Memoria

- Mínimo: 4GB RAM
- Recomendado: 8GB+ RAM
- Espacio en disco: ~5GB para SDK + dependencias

