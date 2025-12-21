#!/bin/bash

# 1. Pull de los últimos cambios
echo "--- Sincronizando repositorio ---"
git pull origin main

# 2. Obtener el siguiente número de versión
# Busca archivos apk en la carpeta builds y cuenta para decidir el siguiente número
mkdir -p builds
VERSION=$(ls builds | grep -oE 'v[0-9]+' | sed 's/v//' | sort -n | tail -1)
NEXT_VERSION=$((VERSION + 1))
FILENAME="fitness-cat-v${NEXT_VERSION}.apk"

# 3. Compilar APK
echo "--- Compilando versión $NEXT_VERSION ---"
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "--- Compilación exitosa ---"
    
    # 4. Mover y renombrar el APK a la carpeta de builds
    cp app/build/outputs/apk/debug/app-debug.apk builds/$FILENAME
    
    # 5. Push al repositorio
    echo "--- Subiendo a GitHub ---"
    git add builds/$FILENAME
    git commit -m "Añadida nueva build: $FILENAME"
    git push origin main
    
    echo "=========================================="
    echo "✓ Proceso completado: $FILENAME subido"
    echo "=========================================="
else
    echo "❌ Error en la compilación. Abortando."
    exit 1
fi
