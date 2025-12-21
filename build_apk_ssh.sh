#!/bin/bash

# Script para compilar APK desde SSH sin Android Studio
# Uso: ./build_apk_ssh.sh

set -e  # Exit on error

echo "=========================================="
echo "Building Fitness Cat APK from SSH"
echo "=========================================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar que estamos en el directorio correcto
if [ ! -f "build.gradle" ] || [ ! -f "settings.gradle" ]; then
    echo -e "${RED}Error: No se encuentra build.gradle. Asegúrate de estar en el directorio del proyecto.${NC}"
    exit 1
fi

# Verificar Java
echo -e "${YELLOW}Verificando Java...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java no está instalado. Instala JDK 8 o superior.${NC}"
    exit 1
fi
java -version

# Verificar local.properties
echo -e "${YELLOW}Verificando local.properties...${NC}"
if [ ! -f "local.properties" ]; then
    echo -e "${YELLOW}local.properties no encontrado. Creando...${NC}"
    
    # Intentar detectar ANDROID_HOME
    if [ -z "$ANDROID_HOME" ]; then
        ANDROID_HOME="$HOME/android-sdk"
        echo -e "${YELLOW}ANDROID_HOME no configurado. Usando: $ANDROID_HOME${NC}"
    fi
    
    cat > local.properties << EOF
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
sdk.dir=$ANDROID_HOME
EOF
    echo -e "${GREEN}✓ local.properties creado${NC}"
else
    echo -e "${GREEN}✓ local.properties existe${NC}"
fi

# Verificar que el SDK existe
SDK_DIR=$(grep "sdk.dir" local.properties | cut -d '=' -f2)
if [ -z "$SDK_DIR" ] || [ ! -d "$SDK_DIR" ]; then
    echo -e "${RED}Error: Android SDK no encontrado en: $SDK_DIR${NC}"
    echo -e "${YELLOW}Por favor instala Android SDK o actualiza local.properties${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Android SDK encontrado: $SDK_DIR${NC}"

# Dar permisos al gradle wrapper
if [ -f "gradlew" ]; then
    chmod +x gradlew
    echo -e "${GREEN}✓ Permisos del gradle wrapper configurados${NC}"
else
    echo -e "${RED}Error: gradlew no encontrado${NC}"
    exit 1
fi

# Limpiar build anterior (opcional, comentado por defecto)
# echo -e "${YELLOW}Limpiando build anterior...${NC}"
# ./gradlew clean

# Compilar
echo -e "${YELLOW}Compilando APK...${NC}"
echo -e "${YELLOW}Esto puede tomar varios minutos la primera vez (descarga dependencias)...${NC}"

./gradlew assembleDebug

# Verificar que el APK se generó
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo ""
    echo -e "${GREEN}=========================================="
    echo -e "✓ APK compilado exitosamente!"
    echo -e "==========================================${NC}"
    echo -e "Ubicación: ${GREEN}$APK_PATH${NC}"
    echo -e "Tamaño: ${GREEN}$APK_SIZE${NC}"
    echo ""
    echo "Para descargar el APK desde tu máquina local:"
    echo "  scp usuario@servidor:$(pwd)/$APK_PATH ./"
    echo ""
else
    echo -e "${RED}Error: APK no se generó correctamente${NC}"
    exit 1
fi

