#!/bin/bash

# Build y subida a GitHub SIN fetch/pull inicial (usa el estado local actual).
# Misma lógica que build_and_push.sh pero sin "git pull origin main".

# 1. Crear la carpeta 'releases' si no existe
mkdir -p releases

# 2. Determinar el siguiente número de versión (v1, v2, v3...)
LAST_VERSION=$(ls releases 2>/dev/null | grep -oE 'v[0-9]+' | sed 's/v//' | sort -n | tail -1)
if [ -z "$LAST_VERSION" ]; then
    NEXT_VERSION=1
else
    NEXT_VERSION=$((LAST_VERSION + 1))
fi
FILENAME="fitness-cat-v${NEXT_VERSION}.apk"

# 3. Compilar el APK
echo "--- Iniciando compilación de $FILENAME ---"
./gradlew assembleDebug

# 4. Verificar si la compilación tuvo éxito
if [ $? -eq 0 ]; then
    echo "--- ✓ Compilación exitosa ---"

    # 5. Copiar el APK a la carpeta 'releases' con el nuevo nombre
    cp app/build/outputs/apk/debug/app-debug.apk releases/$FILENAME

    # 6. Subir a GitHub (sin pull previo)
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
