# 🚀 Instrucciones para Subir a Google Play

## ✅ Estado Actual

Todo está listo para generar el Android App Bundle (AAB) y subirlo a Google Play Console.

### Configuración Verificada:
- ✅ Signing config configurado correctamente
- ✅ Keystore (`morfipolo-release-key.jks`) presente
- ✅ `keystore.properties` configurado
- ✅ ProGuard/R8 habilitado para optimización
- ✅ `printStackTrace()` condicionados con `BuildConfig.DEBUG`
- ✅ Versión: `versionCode = 1`, `versionName = "1.0"`

## 📦 Generar el AAB

### Opción 1: Usar el Script (Recomendado)

**En Windows (PowerShell):**
```powershell
.\build-release.ps1
```

**En Linux/Mac:**
```bash
chmod +x build-release.sh
./build-release.sh
```

### Opción 2: Comando Manual

```bash
# Limpiar builds anteriores
./gradlew clean

# Generar el AAB
./gradlew bundleRelease
```

El AAB se generará en:
```
app/build/outputs/bundle/release/app-release.aab
```

## 📤 Subir a Google Play Console

1. **Accede a Google Play Console**
   - Ve a: https://play.google.com/console
   - Selecciona tu app "MorfiPolo"

2. **Crear una Nueva Versión**
   - Ve a "Producción" (o "Prueba interna" si quieres probar primero)
   - Haz clic en "Crear nueva versión"

3. **Subir el AAB**
   - Arrastra o selecciona el archivo: `app/build/outputs/bundle/release/app-release.aab`
   - Google Play procesará el archivo (puede tardar unos minutos)

4. **Completar Información de la Versión**
   - **Notas de la versión**: Describe los cambios de esta versión
   - **Países/regiones**: Selecciona dónde estará disponible
   - Revisa todas las secciones requeridas

5. **Revisar y Publicar**
   - Revisa toda la información
   - Haz clic en "Revisar versión"
   - Si todo está correcto, haz clic en "Iniciar publicación en producción"

## ⚠️ Importante

### Keystore
- **GUARDA SEGURO** el archivo `morfipolo-release-key.jks` y las contraseñas
- **SIN EL KEYSTORE NO PODRÁS ACTUALIZAR LA APP** en el futuro
- Considera hacer un backup en un lugar seguro

### Versiones Futuras
Para futuras actualizaciones:
1. Incrementa `versionCode` en `app/build.gradle.kts` (ej: de 1 a 2)
2. Actualiza `versionName` si es necesario (ej: de "1.0" a "1.1")
3. Genera el nuevo AAB con `./gradlew bundleRelease`
4. Sube el nuevo AAB a Google Play Console

### Verificación Pre-Release
Antes de publicar, verifica:
- [ ] El AAB se genera sin errores
- [ ] El tamaño del AAB es razonable (debería ser menor que un APK)
- [ ] Has probado la app en modo release localmente (si es posible)
- [ ] Todas las funcionalidades críticas funcionan

## 🔍 Troubleshooting

### Error: "Keystore not found"
- Verifica que `morfipolo-release-key.jks` esté en la raíz del proyecto
- Verifica que `keystore.properties` tenga la ruta correcta

### Error: "Signing config not found"
- Verifica que `keystore.properties` exista y tenga todas las propiedades:
  - `storePassword`
  - `keyPassword`
  - `keyAlias`
  - `storeFile`

### Error al generar el AAB
- Ejecuta `./gradlew clean` primero
- Verifica que no haya errores de compilación
- Revisa los logs para más detalles

## 📝 Notas Adicionales

- El AAB es más eficiente que el APK: Google Play genera APKs optimizados para cada dispositivo
- El proceso de revisión de Google Play puede tardar desde horas hasta días
- Una vez publicado, la app estará disponible gradualmente (puede tardar algunas horas en aparecer en la Play Store)

## 🎉 ¡Listo!

Una vez que subas el AAB y completes el proceso en Google Play Console, tu app estará en producción. ¡Felicitaciones!

