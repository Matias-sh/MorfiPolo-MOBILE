# Checklist de Preparación para Producción

## ✅ Completado

### 1. Logging y Debug
- [x] Configurado OkHttp HttpLoggingInterceptor para desactivar logs en release
- [x] Eliminados logs de debug/info/verbose en:
  - MenuRepository
  - UserRepository
  - MainActivity
  - VoteRepository
- [ ] **PENDIENTE**: Eliminar logs en MenuWidgetProvider (329 líneas)
- [ ] **PENDIENTE**: Eliminar logs en MenuWidgetService (139 líneas)
- [ ] **PENDIENTE**: Eliminar logs en ProfileViewModel
- [ ] **PENDIENTE**: Eliminar logs en otros archivos restantes
- [ ] **PENDIENTE**: Eliminar todos los `printStackTrace()`

### 2. ProGuard/R8
- [x] Habilitado minifyEnabled en release
- [x] Habilitado shrinkResources
- [x] Configuradas reglas de ProGuard para:
  - Kotlin
  - Room Database
  - Retrofit/Moshi
  - Widgets
  - WorkManager
  - Eliminación automática de logs de debug/info/verbose

### 3. Build Configuration
- [x] Configurado buildType debug con applicationIdSuffix
- [ ] **PENDIENTE**: Configurar signing config para release
- [ ] **PENDIENTE**: Verificar versionCode y versionName

## 🔧 Por Hacer

### 4. Optimizaciones de Base de Datos
- [ ] Verificar índices en Room (ya se usan queries optimizadas con LIMIT)
- [ ] Considerar caché de consultas frecuentes si es necesario
- [ ] Las consultas ya están optimizadas con Room Flow para actualizaciones reactivas

### 5. Optimizaciones de Red
- [x] OkHttp ya tiene timeouts configurados (30 segundos)
- [ ] Considerar cache HTTP si el backend lo soporta
- [x] Las consultas ya usan coroutines para no bloquear UI

### 6. Seguridad
- [ ] **CRÍTICO**: Configurar signing key para release
- [ ] Verificar que no haya credenciales hardcodeadas
- [ ] Verificar permisos en AndroidManifest (ya están correctos)
- [ ] Verificar allowBackup (ya configurado)

### 7. Testing
- [ ] Probar build de release completo
- [ ] Verificar que el widget funciona en release
- [ ] Probar en diferentes dispositivos
- [ ] Verificar que no hay crashes silenciosos

### 8. Documentación
- [ ] Documentar proceso de build de release
- [ ] Documentar configuración de signing key
- [ ] Documentar distribución del APK

## 📝 Notas Importantes

### MenuWidgetProvider y MenuWidgetService
Estos archivos tienen MUCHOS logs de debug que deben eliminarse para producción. Dado el volumen (329 líneas en MenuWidgetProvider), se recomienda:

1. **Opción 1**: Eliminar todos los logs manualmente (recomendado para producción limpia)
2. **Opción 2**: Usar BuildConfig.DEBUG para condicionar logs (menos óptimo pero más rápido)

**Recomendación**: Eliminar manualmente los logs de debug/info/verbose, dejar solo logs de error críticos condicionados con BuildConfig.DEBUG.

### Signing Key
Para distribuir el APK a tus compañeros, necesitas:

1. Generar un keystore:
```bash
keytool -genkey -v -keystore morfipolo-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias morfipolo
```

2. Configurar en `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("morfipolo-release-key.jks")
        storePassword = "TU_PASSWORD"
        keyAlias = "morfipolo"
        keyPassword = "TU_PASSWORD"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... resto de configuración
    }
}
```

3. **IMPORTANTE**: Guardar el keystore y passwords de forma segura. Sin ellos, no podrás actualizar la app.

### Distribución del APK
- Generar APK de release: `./gradlew assembleRelease`
- El APK estará en: `app/build/outputs/apk/release/app-release.apk`
- Compartir el APK con tus compañeros para instalación manual
- Considerar usar Google Play Internal Testing para distribución más fácil

### ProGuard
Las reglas de ProGuard están configuradas para:
- Eliminar logs de debug/info/verbose automáticamente
- Mantener clases necesarias para Room, Retrofit, Moshi, Widgets
- Mantener line numbers para stack traces (útil para debugging)

Si hay crashes en release, revisar las reglas de ProGuard y agregar `-keep` según sea necesario.

## 🚀 Comandos Útiles

```bash
# Build de debug
./gradlew assembleDebug

# Build de release (requiere signing config)
./gradlew assembleRelease

# Verificar que no haya problemas de ProGuard
./gradlew assembleRelease --stacktrace

# Ver tamaño del APK
ls -lh app/build/outputs/apk/release/
```


