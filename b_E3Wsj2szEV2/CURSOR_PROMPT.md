# Prompt para Cursor - Comedor App Android

## Contexto del Proyecto

Estoy desarrollando una app Android nativa (XML + Kotlin) para el comedor de una institucion gubernamental en Argentina. Tengo un prototipo visual completo hecho en React/Next.js que necesito traducir a Android XML + Material 3.

## Archivos Importantes

1. **ANDROID_IMPLEMENTATION_GUIDE.md** - Guia completa con:
   - colors.xml (paleta completa)
   - dimens.xml (espaciado, radios, tipografia)
   - themes.xml (tema Material 3)
   - styles.xml (estilos de componentes)
   - Drawables XML (gradientes, shapes, selectors)
   - Animaciones XML
   - Ejemplos de layouts

2. **Componentes React a traducir** (en `/components/comedor/`):
   - `login-screen.tsx` -> `activity_login.xml`
   - `daily-menu-screen.tsx` -> `fragment_daily_menu.xml`
   - `weekly-menu-screen.tsx` -> `fragment_weekly_menu.xml`
   - `profile-screen.tsx` -> `fragment_profile.xml`
   - `change-password-dialog.tsx` -> `dialog_change_password.xml`
   - `bottom-nav.tsx` -> incluido en `activity_main.xml`

## Stack Tecnologico

- **Lenguaje**: Kotlin
- **UI**: Android XML con Material 3 (Material You)
- **Arquitectura**: MVVM con ViewBinding
- **Navegacion**: Navigation Component con Bottom Navigation
- **Animaciones**: MotionLayout, ObjectAnimator, Lottie
- **Minimo SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Caracteristicas de Diseno

### Paleta de Colores
- **Primary**: Teal profundo (#0D7377)
- **Secondary**: Coral calido (#F97316)  
- **Accent**: Dorado (#F59E0B)
- **Background**: Crema suave (#FAFAF9)

### Componentes Clave
1. **Hero Header** con gradiente animado y curvas SVG
2. **Cards** con elevacion, bordes suaves y estado seleccionado
3. **Bottom Navigation** flotante con efecto glassmorphism
4. **Inputs** con glow en focus y validacion visual
5. **Badges** de estado (Abierto/Cerrado/Seleccionado)
6. **Botones** con gradiente y efecto ripple premium

### Animaciones
- Entrada escalonada de elementos (stagger)
- Pulse en badges de estado
- Scale + elevation en press de cards
- Check animado al seleccionar opcion
- Confetti al confirmar seleccion (Lottie)
- Shimmer en loading states

## Pantallas

1. **Login** - Logo animado, form card, boton con gradiente
2. **Menu del Dia** - Header hero, lista de opciones seleccionables
3. **Menu Semanal** - Timeline vertical, cards por fecha
4. **Perfil** - Avatar con iniciales, opciones de config
5. **Cambiar Contrasena** - Bottom sheet con validacion real-time

## Instrucciones para Cursor

1. Empeza generando los archivos de recursos (`/res/values/`)
2. Luego los drawables (`/res/drawable/`)
3. Despues las animaciones (`/res/anim/` y `/res/animator/`)
4. Finalmente los layouts (`/res/layout/`)
5. Por ultimo el codigo Kotlin para las Activities/Fragments

Usa siempre los valores definidos en la guia. No inventes colores ni dimensiones.

## Dependencias Gradle

```groovy
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.7'
implementation 'androidx.navigation:navigation-ui-ktx:2.7.7'
implementation 'com.airbnb.android:lottie:6.3.0'
implementation 'com.facebook.shimmer:shimmer:0.5.0'
```
