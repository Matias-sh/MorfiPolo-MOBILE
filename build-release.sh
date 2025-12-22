#!/bin/bash
# Script para generar el Android App Bundle (AAB) de producción
# Este es el archivo que necesitas subir a Google Play Console

echo "🚀 Generando Android App Bundle (AAB) para producción..."
echo ""

# Verificar que el keystore existe
if [ ! -f "morfipolo-release-key.jks" ]; then
    echo "❌ ERROR: No se encontró el archivo morfipolo-release-key.jks"
    echo "   Asegúrate de que el keystore esté en la raíz del proyecto."
    exit 1
fi

# Verificar que keystore.properties existe
if [ ! -f "keystore.properties" ]; then
    echo "❌ ERROR: No se encontró el archivo keystore.properties"
    exit 1
fi

echo "✅ Keystore y configuración encontrados"
echo ""

# Limpiar builds anteriores
echo "🧹 Limpiando builds anteriores..."
./gradlew clean

# Generar el AAB
echo ""
echo "📦 Generando Android App Bundle (AAB)..."
./gradlew bundleRelease

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ ¡AAB generado exitosamente!"
    echo ""
    echo "📍 Ubicación del AAB:"
    echo "   app/build/outputs/bundle/release/app-release.aab"
    echo ""
    echo "📤 Siguiente paso:"
    echo "   1. Ve a Google Play Console"
    echo "   2. Crea una nueva versión en Producción"
    echo "   3. Sube el archivo: app/build/outputs/bundle/release/app-release.aab"
    echo ""
else
    echo ""
    echo "❌ Error al generar el AAB. Revisa los mensajes de error arriba."
    exit 1
fi

