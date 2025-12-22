# Script para generar el Android App Bundle (AAB) de producción
# Este es el archivo que necesitas subir a Google Play Console

Write-Host "🚀 Generando Android App Bundle (AAB) para producción..." -ForegroundColor Green
Write-Host ""

# Verificar que el keystore existe
if (-not (Test-Path "morfipolo-release-key.jks")) {
    Write-Host "❌ ERROR: No se encontró el archivo morfipolo-release-key.jks" -ForegroundColor Red
    Write-Host "   Asegúrate de que el keystore esté en la raíz del proyecto." -ForegroundColor Yellow
    exit 1
}

# Verificar que keystore.properties existe
if (-not (Test-Path "keystore.properties")) {
    Write-Host "❌ ERROR: No se encontró el archivo keystore.properties" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Keystore y configuración encontrados" -ForegroundColor Green
Write-Host ""

# Limpiar builds anteriores
Write-Host "🧹 Limpiando builds anteriores..." -ForegroundColor Yellow
.\gradlew.bat clean

# Generar el AAB
Write-Host ""
Write-Host "📦 Generando Android App Bundle (AAB)..." -ForegroundColor Cyan
.\gradlew.bat bundleRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ ¡AAB generado exitosamente!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📍 Ubicación del AAB:" -ForegroundColor Cyan
    Write-Host "   app\build\outputs\bundle\release\app-release.aab" -ForegroundColor White
    Write-Host ""
    Write-Host "📤 Siguiente paso:" -ForegroundColor Yellow
    Write-Host "   1. Ve a Google Play Console" -ForegroundColor White
    Write-Host "   2. Crea una nueva versión en Producción" -ForegroundColor White
    Write-Host "   3. Sube el archivo: app\build\outputs\bundle\release\app-release.aab" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ Error al generar el AAB. Revisa los mensajes de error arriba." -ForegroundColor Red
    exit 1
}

