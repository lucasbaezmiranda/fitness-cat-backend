#!/bin/bash

# 1. Asegurarnos de estar en la última versión del código
echo "--- Sincronizando con GitHub (Pull) ---"
git pull origin main

# 2. Crear la carpeta 'releases' si no existe
mkdir -p releases

# 3. Determinar el siguiente número de versión (v1, v2, v3...)
# Busca en la carpeta 'releases' el número más alto
LAST_VERSION=$(ls releases | grep -oE 'v[0-9]+' | sed 's/v//' | sort -n | tail -1)
if [ -z "$LAST_VERSION" ]; then
    NEXT_VERSION=1
else
    NEXT_VERSION=$((LAST_VERSION + 1))
fi
FILENAME="fitness-cat-v${NEXT_VERSION}.apk"

# 4. Compilar el APK
echo "--- Iniciando compilación de $FILENAME ---"
./gradlew assembleDebug

# 5. Verificar si la compilación tuvo éxito
if [ $? -eq 0 ]; then
    echo "--- ✓ Compilación exitosa ---"
    
    # 6. Copiar el APK a la carpeta 'releases' con el nuevo nombre
    cp app/build/outputs/apk/debug/app-debug.apk releases/$FILENAME
    
    # 7. Subir a GitHub
    echo "--- Subiendo nueva versión a GitHub ---"
    git add releases/$FILENAME
    git commit -m "Build: Se añade $FILENAME a la carpeta releases"
    git push origin main
    
    echo "=========================================="
    echo "✅ ¡Listo! Archivo disponible en: releases/$FILENAME"
    echo "=========================================="
else
    echo "❌ Error en la compilación. El proceso se detuvo."
    exit 1
fi