# Guía de Implementación Android - Comedor App Premium

Esta guía traduce el diseño React/Framer Motion a Android XML + Material 3.

---

## 1. COLORES (res/values/colors.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Primary Colors - Deep Teal -->
    <color name="primary">#0D7377</color>
    <color name="primary_light">#14919B</color>
    <color name="primary_dark">#065A5C</color>
    <color name="primary_container">#A7F3D0</color>
    <color name="on_primary">#FFFFFF</color>
    <color name="on_primary_container">#065A5C</color>
    
    <!-- Secondary Colors - Warm Coral -->
    <color name="secondary">#F97316</color>
    <color name="secondary_light">#FB923C</color>
    <color name="secondary_dark">#EA580C</color>
    <color name="secondary_container">#FFEDD5</color>
    <color name="on_secondary">#FFFFFF</color>
    <color name="on_secondary_container">#9A3412</color>
    
    <!-- Accent - Gold -->
    <color name="accent">#F59E0B</color>
    <color name="accent_light">#FCD34D</color>
    
    <!-- Surfaces -->
    <color name="background">#FAFAF9</color>
    <color name="surface">#FFFFFF</color>
    <color name="surface_variant">#F5F5F4</color>
    <color name="surface_elevated">#FFFFFF</color>
    
    <!-- Text Colors -->
    <color name="on_background">#1C1917</color>
    <color name="on_surface">#1C1917</color>
    <color name="on_surface_variant">#78716C</color>
    <color name="text_hint">#A8A29E</color>
    
    <!-- Semantic Colors -->
    <color name="success">#10B981</color>
    <color name="success_light">#D1FAE5</color>
    <color name="warning">#F59E0B</color>
    <color name="warning_light">#FEF3C7</color>
    <color name="error">#EF4444</color>
    <color name="error_light">#FEE2E2</color>
    <color name="info">#3B82F6</color>
    <color name="info_light">#DBEAFE</color>
    
    <!-- Status Badge Colors -->
    <color name="badge_open_bg">#D1FAE5</color>
    <color name="badge_open_text">#065F46</color>
    <color name="badge_closed_bg">#FEE2E2</color>
    <color name="badge_closed_text">#991B1B</color>
    <color name="badge_selected_bg">#DBEAFE</color>
    <color name="badge_selected_text">#1E40AF</color>
    
    <!-- Gradients (para usar en drawables) -->
    <color name="gradient_primary_start">#0D7377</color>
    <color name="gradient_primary_end">#14919B</color>
    <color name="gradient_secondary_start">#F97316</color>
    <color name="gradient_secondary_end">#FB923C</color>
    <color name="gradient_hero_start">#0D7377</color>
    <color name="gradient_hero_middle">#14919B</color>
    <color name="gradient_hero_end">#F97316</color>
    
    <!-- Card & Border -->
    <color name="card_border">#E7E5E4</color>
    <color name="card_shadow">#1A000000</color>
    <color name="divider">#E7E5E4</color>
    
    <!-- Glassmorphism -->
    <color name="glass_white">#E6FFFFFF</color>
    <color name="glass_border">#33FFFFFF</color>
    
    <!-- Bottom Nav -->
    <color name="nav_inactive">#78716C</color>
    <color name="nav_active">#0D7377</color>
    <color name="nav_background">#F2FFFFFF</color>
</resources>
```

---

## 2. DIMENSIONES (res/values/dimens.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Spacing Scale -->
    <dimen name="spacing_xxs">2dp</dimen>
    <dimen name="spacing_xs">4dp</dimen>
    <dimen name="spacing_sm">8dp</dimen>
    <dimen name="spacing_md">12dp</dimen>
    <dimen name="spacing_lg">16dp</dimen>
    <dimen name="spacing_xl">20dp</dimen>
    <dimen name="spacing_2xl">24dp</dimen>
    <dimen name="spacing_3xl">32dp</dimen>
    <dimen name="spacing_4xl">40dp</dimen>
    <dimen name="spacing_5xl">48dp</dimen>
    
    <!-- Corner Radius -->
    <dimen name="radius_sm">8dp</dimen>
    <dimen name="radius_md">12dp</dimen>
    <dimen name="radius_lg">16dp</dimen>
    <dimen name="radius_xl">20dp</dimen>
    <dimen name="radius_2xl">24dp</dimen>
    <dimen name="radius_3xl">32dp</dimen>
    <dimen name="radius_full">999dp</dimen>
    
    <!-- Typography -->
    <dimen name="text_xs">12sp</dimen>
    <dimen name="text_sm">14sp</dimen>
    <dimen name="text_base">16sp</dimen>
    <dimen name="text_lg">18sp</dimen>
    <dimen name="text_xl">20sp</dimen>
    <dimen name="text_2xl">24sp</dimen>
    <dimen name="text_3xl">30sp</dimen>
    <dimen name="text_4xl">36sp</dimen>
    <dimen name="text_5xl">48sp</dimen>
    
    <!-- Touch Targets -->
    <dimen name="touch_target_min">48dp</dimen>
    <dimen name="button_height">56dp</dimen>
    <dimen name="input_height">56dp</dimen>
    <dimen name="nav_item_height">56dp</dimen>
    
    <!-- Component Sizes -->
    <dimen name="avatar_small">40dp</dimen>
    <dimen name="avatar_medium">64dp</dimen>
    <dimen name="avatar_large">96dp</dimen>
    <dimen name="icon_small">16dp</dimen>
    <dimen name="icon_medium">24dp</dimen>
    <dimen name="icon_large">32dp</dimen>
    <dimen name="logo_size">120dp</dimen>
    
    <!-- Elevation -->
    <dimen name="elevation_none">0dp</dimen>
    <dimen name="elevation_xs">1dp</dimen>
    <dimen name="elevation_sm">2dp</dimen>
    <dimen name="elevation_md">4dp</dimen>
    <dimen name="elevation_lg">8dp</dimen>
    <dimen name="elevation_xl">16dp</dimen>
    <dimen name="elevation_2xl">24dp</dimen>
    
    <!-- Bottom Nav -->
    <dimen name="bottom_nav_height">80dp</dimen>
    <dimen name="bottom_nav_margin">16dp</dimen>
    <dimen name="bottom_nav_radius">28dp</dimen>
    
    <!-- Hero Header -->
    <dimen name="hero_header_height">200dp</dimen>
    <dimen name="hero_header_height_expanded">240dp</dimen>
    
    <!-- Cards -->
    <dimen name="card_min_height">80dp</dimen>
    <dimen name="menu_card_height">120dp</dimen>
</resources>
```

---

## 3. TEMA (res/values/themes.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Comedor" parent="Theme.Material3.Light.NoActionBar">
        <!-- Primary -->
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryContainer">@color/primary_container</item>
        <item name="colorOnPrimary">@color/on_primary</item>
        <item name="colorOnPrimaryContainer">@color/on_primary_container</item>
        
        <!-- Secondary -->
        <item name="colorSecondary">@color/secondary</item>
        <item name="colorSecondaryContainer">@color/secondary_container</item>
        <item name="colorOnSecondary">@color/on_secondary</item>
        <item name="colorOnSecondaryContainer">@color/on_secondary_container</item>
        
        <!-- Tertiary (Accent) -->
        <item name="colorTertiary">@color/accent</item>
        
        <!-- Background & Surface -->
        <item name="android:colorBackground">@color/background</item>
        <item name="colorSurface">@color/surface</item>
        <item name="colorSurfaceVariant">@color/surface_variant</item>
        <item name="colorOnBackground">@color/on_background</item>
        <item name="colorOnSurface">@color/on_surface</item>
        <item name="colorOnSurfaceVariant">@color/on_surface_variant</item>
        
        <!-- Error -->
        <item name="colorError">@color/error</item>
        <item name="colorOnError">@color/on_primary</item>
        
        <!-- Outline -->
        <item name="colorOutline">@color/card_border</item>
        
        <!-- Status Bar -->
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
        
        <!-- Shape -->
        <item name="shapeAppearanceSmallComponent">@style/ShapeAppearance.Comedor.Small</item>
        <item name="shapeAppearanceMediumComponent">@style/ShapeAppearance.Comedor.Medium</item>
        <item name="shapeAppearanceLargeComponent">@style/ShapeAppearance.Comedor.Large</item>
    </style>
    
    <!-- Shapes -->
    <style name="ShapeAppearance.Comedor.Small" parent="ShapeAppearance.Material3.SmallComponent">
        <item name="cornerSize">@dimen/radius_sm</item>
    </style>
    
    <style name="ShapeAppearance.Comedor.Medium" parent="ShapeAppearance.Material3.MediumComponent">
        <item name="cornerSize">@dimen/radius_lg</item>
    </style>
    
    <style name="ShapeAppearance.Comedor.Large" parent="ShapeAppearance.Material3.LargeComponent">
        <item name="cornerSize">@dimen/radius_xl</item>
    </style>
</resources>
```

---

## 4. ESTILOS DE COMPONENTES (res/values/styles.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    
    <!-- ==================== BOTONES ==================== -->
    
    <!-- Botón Primario (filled) -->
    <style name="Widget.Comedor.Button.Primary" parent="Widget.Material3.Button">
        <item name="android:minHeight">@dimen/button_height</item>
        <item name="cornerRadius">@dimen/radius_full</item>
        <item name="backgroundTint">@color/primary</item>
        <item name="android:textColor">@color/on_primary</item>
        <item name="android:textSize">@dimen/text_base</item>
        <item name="android:fontFamily">@font/inter_semibold</item>
        <item name="android:letterSpacing">0.02</item>
        <item name="elevation">@dimen/elevation_md</item>
        <item name="android:stateListAnimator">@animator/button_press</item>
    </style>
    
    <!-- Botón con Gradiente (usar con background drawable) -->
    <style name="Widget.Comedor.Button.Gradient" parent="Widget.Material3.Button">
        <item name="android:minHeight">@dimen/button_height</item>
        <item name="android:background">@drawable/bg_button_gradient</item>
        <item name="android:textColor">@color/on_primary</item>
        <item name="android:textSize">@dimen/text_base</item>
        <item name="android:fontFamily">@font/inter_semibold</item>
        <item name="backgroundTint">@null</item>
        <item name="android:stateListAnimator">@animator/button_press</item>
    </style>
    
    <!-- Botón Secundario (outlined) -->
    <style name="Widget.Comedor.Button.Secondary" parent="Widget.Material3.Button.OutlinedButton">
        <item name="android:minHeight">@dimen/button_height</item>
        <item name="cornerRadius">@dimen/radius_full</item>
        <item name="strokeColor">@color/primary</item>
        <item name="strokeWidth">2dp</item>
        <item name="android:textColor">@color/primary</item>
        <item name="android:textSize">@dimen/text_base</item>
        <item name="rippleColor">@color/primary_container</item>
    </style>
    
    <!-- Botón de Texto -->
    <style name="Widget.Comedor.Button.Text" parent="Widget.Material3.Button.TextButton">
        <item name="android:minHeight">@dimen/touch_target_min</item>
        <item name="android:textColor">@color/primary</item>
        <item name="android:textSize">@dimen/text_sm</item>
        <item name="android:fontFamily">@font/inter_medium</item>
        <item name="rippleColor">@color/primary_container</item>
    </style>
    
    <!-- Botón Destructivo -->
    <style name="Widget.Comedor.Button.Destructive" parent="Widget.Material3.Button">
        <item name="android:minHeight">@dimen/button_height</item>
        <item name="cornerRadius">@dimen/radius_full</item>
        <item name="backgroundTint">@color/error</item>
        <item name="android:textColor">@color/on_primary</item>
    </style>
    
    <!-- ==================== TEXT INPUT ==================== -->
    
    <!-- Input Field Premium -->
    <style name="Widget.Comedor.TextInputLayout" parent="Widget.Material3.TextInputLayout.OutlinedBox">
        <item name="boxCornerRadiusTopStart">@dimen/radius_lg</item>
        <item name="boxCornerRadiusTopEnd">@dimen/radius_lg</item>
        <item name="boxCornerRadiusBottomStart">@dimen/radius_lg</item>
        <item name="boxCornerRadiusBottomEnd">@dimen/radius_lg</item>
        <item name="boxStrokeWidth">2dp</item>
        <item name="boxStrokeWidthFocused">2dp</item>
        <item name="boxStrokeColor">@color/selector_input_stroke</item>
        <item name="boxBackgroundColor">@color/surface</item>
        <item name="hintTextColor">@color/text_hint</item>
        <item name="startIconTint">@color/on_surface_variant</item>
        <item name="endIconTint">@color/on_surface_variant</item>
        <item name="errorIconDrawable">@null</item>
    </style>
    
    <style name="Widget.Comedor.TextInputEditText" parent="Widget.Material3.TextInputEditText.OutlinedBox">
        <item name="android:textSize">@dimen/text_base</item>
        <item name="android:textColor">@color/on_surface</item>
        <item name="android:fontFamily">@font/inter_regular</item>
        <item name="android:paddingStart">@dimen/spacing_lg</item>
        <item name="android:paddingEnd">@dimen/spacing_lg</item>
        <item name="android:minHeight">@dimen/input_height</item>
    </style>
    
    <!-- ==================== CARDS ==================== -->
    
    <!-- Card Premium -->
    <style name="Widget.Comedor.Card" parent="Widget.Material3.CardView.Elevated">
        <item name="cardCornerRadius">@dimen/radius_xl</item>
        <item name="cardElevation">@dimen/elevation_md</item>
        <item name="strokeWidth">1dp</item>
        <item name="strokeColor">@color/card_border</item>
        <item name="cardBackgroundColor">@color/surface</item>
        <item name="android:stateListAnimator">@animator/card_press</item>
    </style>
    
    <!-- Card Seleccionable -->
    <style name="Widget.Comedor.Card.Selectable" parent="Widget.Comedor.Card">
        <item name="android:clickable">true</item>
        <item name="android:focusable">true</item>
        <item name="rippleColor">@color/primary_container</item>
        <item name="checkedIcon">@drawable/ic_check_circle</item>
        <item name="checkedIconTint">@color/primary</item>
    </style>
    
    <!-- Card Menu Item -->
    <style name="Widget.Comedor.Card.MenuItem" parent="Widget.Comedor.Card.Selectable">
        <item name="android:minHeight">@dimen/menu_card_height</item>
    </style>
    
    <!-- ==================== BADGES ==================== -->
    
    <style name="Widget.Comedor.Badge" parent="">
        <item name="android:paddingStart">@dimen/spacing_md</item>
        <item name="android:paddingEnd">@dimen/spacing_md</item>
        <item name="android:paddingTop">@dimen/spacing_xs</item>
        <item name="android:paddingBottom">@dimen/spacing_xs</item>
        <item name="android:textSize">@dimen/text_xs</item>
        <item name="android:fontFamily">@font/inter_semibold</item>
        <item name="android:textAllCaps">true</item>
    </style>
    
    <style name="Widget.Comedor.Badge.Open" parent="Widget.Comedor.Badge">
        <item name="android:background">@drawable/bg_badge_open</item>
        <item name="android:textColor">@color/badge_open_text</item>
    </style>
    
    <style name="Widget.Comedor.Badge.Closed" parent="Widget.Comedor.Badge">
        <item name="android:background">@drawable/bg_badge_closed</item>
        <item name="android:textColor">@color/badge_closed_text</item>
    </style>
    
    <style name="Widget.Comedor.Badge.Selected" parent="Widget.Comedor.Badge">
        <item name="android:background">@drawable/bg_badge_selected</item>
        <item name="android:textColor">@color/badge_selected_text</item>
    </style>
    
    <!-- ==================== BOTTOM NAV ==================== -->
    
    <style name="Widget.Comedor.BottomNavigation" parent="Widget.Material3.BottomNavigationView">
        <item name="android:background">@drawable/bg_bottom_nav</item>
        <item name="itemIconTint">@color/selector_nav_icon</item>
        <item name="itemTextColor">@color/selector_nav_text</item>
        <item name="itemActiveIndicatorStyle">@style/Widget.Comedor.BottomNavigation.ActiveIndicator</item>
        <item name="labelVisibilityMode">labeled</item>
        <item name="elevation">@dimen/elevation_xl</item>
        <item name="android:minHeight">@dimen/bottom_nav_height</item>
    </style>
    
    <style name="Widget.Comedor.BottomNavigation.ActiveIndicator" parent="">
        <item name="android:color">@color/primary_container</item>
        <item name="android:width">64dp</item>
        <item name="android:height">32dp</item>
        <item name="shapeAppearance">@style/ShapeAppearance.Comedor.Full</item>
    </style>
    
    <style name="ShapeAppearance.Comedor.Full" parent="">
        <item name="cornerSize">50%</item>
    </style>
    
    <!-- ==================== TYPOGRAPHY ==================== -->
    
    <style name="TextAppearance.Comedor.Headline.Large" parent="TextAppearance.Material3.HeadlineLarge">
        <item name="android:textSize">@dimen/text_4xl</item>
        <item name="android:fontFamily">@font/inter_bold</item>
        <item name="android:textColor">@color/on_surface</item>
        <item name="android:letterSpacing">-0.02</item>
    </style>
    
    <style name="TextAppearance.Comedor.Headline.Medium" parent="TextAppearance.Material3.HeadlineMedium">
        <item name="android:textSize">@dimen/text_2xl</item>
        <item name="android:fontFamily">@font/inter_bold</item>
        <item name="android:textColor">@color/on_surface</item>
    </style>
    
    <style name="TextAppearance.Comedor.Title.Large" parent="TextAppearance.Material3.TitleLarge">
        <item name="android:textSize">@dimen/text_xl</item>
        <item name="android:fontFamily">@font/inter_semibold</item>
        <item name="android:textColor">@color/on_surface</item>
    </style>
    
    <style name="TextAppearance.Comedor.Title.Medium" parent="TextAppearance.Material3.TitleMedium">
        <item name="android:textSize">@dimen/text_lg</item>
        <item name="android:fontFamily">@font/inter_semibold</item>
        <item name="android:textColor">@color/on_surface</item>
    </style>
    
    <style name="TextAppearance.Comedor.Body.Large" parent="TextAppearance.Material3.BodyLarge">
        <item name="android:textSize">@dimen/text_base</item>
        <item name="android:fontFamily">@font/inter_regular</item>
        <item name="android:textColor">@color/on_surface</item>
        <item name="android:lineSpacingMultiplier">1.5</item>
    </style>
    
    <style name="TextAppearance.Comedor.Body.Medium" parent="TextAppearance.Material3.BodyMedium">
        <item name="android:textSize">@dimen/text_sm</item>
        <item name="android:fontFamily">@font/inter_regular</item>
        <item name="android:textColor">@color/on_surface_variant</item>
    </style>
    
    <style name="TextAppearance.Comedor.Label" parent="TextAppearance.Material3.LabelMedium">
        <item name="android:textSize">@dimen/text_xs</item>
        <item name="android:fontFamily">@font/inter_medium</item>
        <item name="android:textColor">@color/on_surface_variant</item>
        <item name="android:textAllCaps">true</item>
        <item name="android:letterSpacing">0.05</item>
    </style>
</resources>
```

---

## 5. DRAWABLES

### bg_button_gradient.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="@color/primary_dark">
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:type="linear"
                android:angle="90"
                android:startColor="@color/gradient_primary_start"
                android:endColor="@color/gradient_primary_end" />
            <corners android:radius="@dimen/radius_full" />
        </shape>
    </item>
</ripple>
```

### bg_hero_header.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Gradient base -->
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:type="linear"
                android:angle="135"
                android:startColor="@color/gradient_hero_start"
                android:centerColor="@color/gradient_hero_middle"
                android:endColor="@color/gradient_hero_end" />
        </shape>
    </item>
    <!-- Bottom rounded corners -->
    <item android:top="160dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/background" />
            <corners
                android:topLeftRadius="@dimen/radius_3xl"
                android:topRightRadius="@dimen/radius_3xl" />
        </shape>
    </item>
</layer-list>
```

### bg_bottom_nav.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/nav_background" />
    <corners android:radius="@dimen/bottom_nav_radius" />
    <stroke
        android:width="1dp"
        android:color="@color/glass_border" />
</shape>
```

### bg_card_selected.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Glow effect -->
    <item
        android:left="-2dp"
        android:top="-2dp"
        android:right="-2dp"
        android:bottom="-2dp">
        <shape android:shape="rectangle">
            <stroke
                android:width="3dp"
                android:color="@color/primary_container" />
            <corners android:radius="@dimen/radius_xl" />
        </shape>
    </item>
    <!-- Main card -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/surface" />
            <stroke
                android:width="2dp"
                android:color="@color/primary" />
            <corners android:radius="@dimen/radius_xl" />
        </shape>
    </item>
</layer-list>
```

### bg_badge_open.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/badge_open_bg" />
    <corners android:radius="@dimen/radius_full" />
</shape>
```

### bg_badge_closed.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/badge_closed_bg" />
    <corners android:radius="@dimen/radius_full" />
</shape>
```

### bg_input_focused.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Glow effect -->
    <item>
        <shape android:shape="rectangle">
            <stroke
                android:width="4dp"
                android:color="@color/primary_container" />
            <corners android:radius="@dimen/radius_lg" />
        </shape>
    </item>
    <!-- Main shape -->
    <item
        android:left="2dp"
        android:top="2dp"
        android:right="2dp"
        android:bottom="2dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/surface" />
            <stroke
                android:width="2dp"
                android:color="@color/primary" />
            <corners android:radius="@dimen/radius_lg" />
        </shape>
    </item>
</layer-list>
```

### selector_input_stroke.xml (res/color/)
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true" android:color="@color/primary" />
    <item android:state_hovered="true" android:color="@color/primary_light" />
    <item android:state_enabled="false" android:color="@color/divider" />
    <item android:color="@color/card_border" />
</selector>
```

### selector_nav_icon.xml (res/color/)
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_checked="true" android:color="@color/primary" />
    <item android:color="@color/nav_inactive" />
</selector>
```

### ic_check_animated.xml (AnimatedVectorDrawable)
```xml
<?xml version="1.0" encoding="utf-8"?>
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt">
    <aapt:attr name="android:drawable">
        <vector
            android:width="24dp"
            android:height="24dp"
            android:viewportWidth="24"
            android:viewportHeight="24">
            <path
                android:name="check"
                android:pathData="M9,16.17L4.83,12l-1.42,1.41L9,19L21,7l-1.41,-1.41z"
                android:strokeColor="@color/primary"
                android:strokeWidth="2"
                android:strokeLineCap="round"
                android:strokeLineJoin="round"
                android:trimPathEnd="0" />
        </vector>
    </aapt:attr>
    <target android:name="check">
        <aapt:attr name="android:animation">
            <objectAnimator
                android:propertyName="trimPathEnd"
                android:valueFrom="0"
                android:valueTo="1"
                android:duration="400"
                android:interpolator="@android:interpolator/fast_out_slow_in" />
        </aapt:attr>
    </target>
</animated-vector>
```

---

## 6. ANIMACIONES

### res/anim/fade_in_up.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:interpolator/decelerate_cubic">
    <alpha
        android:fromAlpha="0"
        android:toAlpha="1"
        android:duration="400" />
    <translate
        android:fromYDelta="30dp"
        android:toYDelta="0"
        android:duration="400" />
</set>
```

### res/anim/fade_in_scale.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:interpolator/overshoot">
    <alpha
        android:fromAlpha="0"
        android:toAlpha="1"
        android:duration="300" />
    <scale
        android:fromXScale="0.8"
        android:toXScale="1"
        android:fromYScale="0.8"
        android:toYScale="1"
        android:pivotX="50%"
        android:pivotY="50%"
        android:duration="400" />
</set>
```

### res/anim/pulse.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:repeatMode="reverse"
    android:repeatCount="infinite">
    <scale
        android:fromXScale="1"
        android:toXScale="1.05"
        android:fromYScale="1"
        android:toYScale="1.05"
        android:pivotX="50%"
        android:pivotY="50%"
        android:duration="1000"
        android:interpolator="@android:interpolator/accelerate_decelerate" />
</set>
```

### res/anim/shake.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <translate
        android:fromXDelta="0"
        android:toXDelta="10dp"
        android:duration="50" />
    <translate
        android:fromXDelta="10dp"
        android:toXDelta="-10dp"
        android:startOffset="50"
        android:duration="50" />
    <translate
        android:fromXDelta="-10dp"
        android:toXDelta="10dp"
        android:startOffset="100"
        android:duration="50" />
    <translate
        android:fromXDelta="10dp"
        android:toXDelta="0"
        android:startOffset="150"
        android:duration="50" />
</set>
```

### res/anim/confetti.xml (para usar con MotionLayout)
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <alpha
        android:fromAlpha="1"
        android:toAlpha="0"
        android:duration="1000" />
    <translate
        android:fromYDelta="0"
        android:toYDelta="100dp"
        android:duration="1000"
        android:interpolator="@android:interpolator/accelerate_quad" />
    <rotate
        android:fromDegrees="0"
        android:toDegrees="360"
        android:pivotX="50%"
        android:pivotY="50%"
        android:duration="1000" />
</set>
```

### res/animator/button_press.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <set>
            <objectAnimator
                android:propertyName="translationZ"
                android:valueTo="2dp"
                android:duration="100" />
            <objectAnimator
                android:propertyName="scaleX"
                android:valueTo="0.98"
                android:duration="100" />
            <objectAnimator
                android:propertyName="scaleY"
                android:valueTo="0.98"
                android:duration="100" />
        </set>
    </item>
    <item>
        <set>
            <objectAnimator
                android:propertyName="translationZ"
                android:valueTo="4dp"
                android:duration="200" />
            <objectAnimator
                android:propertyName="scaleX"
                android:valueTo="1"
                android:duration="200" />
            <objectAnimator
                android:propertyName="scaleY"
                android:valueTo="1"
                android:duration="200" />
        </set>
    </item>
</selector>
```

### res/animator/card_press.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <set>
            <objectAnimator
                android:propertyName="translationZ"
                android:valueTo="2dp"
                android:duration="150"
                android:interpolator="@android:interpolator/fast_out_slow_in" />
            <objectAnimator
                android:propertyName="scaleX"
                android:valueTo="0.98"
                android:duration="150" />
            <objectAnimator
                android:propertyName="scaleY"
                android:valueTo="0.98"
                android:duration="150" />
        </set>
    </item>
    <item>
        <set>
            <objectAnimator
                android:propertyName="translationZ"
                android:valueTo="8dp"
                android:duration="300"
                android:interpolator="@android:interpolator/fast_out_slow_in" />
            <objectAnimator
                android:propertyName="scaleX"
                android:valueTo="1"
                android:duration="300" />
            <objectAnimator
                android:propertyName="scaleY"
                android:valueTo="1"
                android:duration="300" />
        </set>
    </item>
</selector>
```

---

## 7. EJEMPLO DE LAYOUT - LOGIN

### activity_login.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background"
    android:fitsSystemWindows="true">

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fillViewport="true">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:gravity="center_horizontal"
            android:paddingHorizontal="@dimen/spacing_2xl">

            <!-- Logo Container -->
            <FrameLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="80dp">

                <!-- Glow Ring (animated) -->
                <View
                    android:id="@+id/logo_glow"
                    android:layout_width="140dp"
                    android:layout_height="140dp"
                    android:layout_gravity="center"
                    android:background="@drawable/bg_logo_glow" />

                <!-- Logo -->
                <com.google.android.material.imageview.ShapeableImageView
                    android:id="@+id/logo"
                    android:layout_width="@dimen/logo_size"
                    android:layout_height="@dimen/logo_size"
                    android:layout_gravity="center"
                    android:src="@drawable/ic_logo_comedor"
                    android:background="@drawable/bg_logo_circle"
                    android:padding="@dimen/spacing_lg"
                    app:shapeAppearanceOverlay="@style/ShapeAppearance.Comedor.Full" />
            </FrameLayout>

            <!-- Title -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_3xl"
                android:text="Iniciar Sesión"
                android:textAppearance="@style/TextAppearance.Comedor.Headline.Medium" />

            <!-- Subtitle -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_sm"
                android:text="Ingresá para ver el menú"
                android:textAppearance="@style/TextAppearance.Comedor.Body.Medium" />

            <!-- Form Card -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_3xl"
                style="@style/Widget.Comedor.Card"
                app:cardElevation="@dimen/elevation_lg">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="@dimen/spacing_2xl">

                    <!-- DNI Input -->
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/til_dni"
                        style="@style/Widget.Comedor.TextInputLayout"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="DNI"
                        app:startIconDrawable="@drawable/ic_person"
                        app:startIconTint="@color/on_surface_variant">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/et_dni"
                            style="@style/Widget.Comedor.TextInputEditText"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="number"
                            android:maxLength="8" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <!-- Password Input -->
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/til_password"
                        style="@style/Widget.Comedor.TextInputLayout"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/spacing_lg"
                        android:hint="Contraseña"
                        app:startIconDrawable="@drawable/ic_lock"
                        app:endIconMode="password_toggle"
                        app:endIconDrawable="@drawable/selector_password_toggle">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/et_password"
                            style="@style/Widget.Comedor.TextInputEditText"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="textPassword" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <!-- Login Button -->
                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_login"
                        style="@style/Widget.Comedor.Button.Gradient"
                        android:layout_width="match_parent"
                        android:layout_height="@dimen/button_height"
                        android:layout_marginTop="@dimen/spacing_2xl"
                        android:text="Ingresar"
                        app:icon="@drawable/ic_arrow_right"
                        app:iconGravity="end" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- First Login Hint -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_2xl"
                android:gravity="center"
                android:text="Para el primer ingreso utilizá como contraseña\nAb + tu DNI (Ej. Ab12345678)."
                android:textAppearance="@style/TextAppearance.Comedor.Body.Medium"
                android:lineSpacingMultiplier="1.4" />

            <!-- Change Password Link -->
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_change_password_hint"
                style="@style/Widget.Comedor.Button.Text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_sm"
                android:text="¡No olvides cambiar tu contraseña!"
                android:textColor="@color/secondary" />
        </LinearLayout>
    </ScrollView>

    <!-- Loading Overlay -->
    <FrameLayout
        android:id="@+id/loading_overlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#80FFFFFF"
        android:visibility="gone"
        android:clickable="true"
        android:focusable="true">

        <com.google.android.material.progressindicator.CircularProgressIndicator
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:indeterminate="true"
            app:indicatorColor="@color/primary"
            app:trackColor="@color/primary_container" />
    </FrameLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

---

## 8. EJEMPLO DE LAYOUT - MENU DEL DIA

### fragment_daily_menu.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background">

    <!-- Collapsing Hero Header -->
    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/app_bar"
        android:layout_width="match_parent"
        android:layout_height="@dimen/hero_header_height"
        android:background="@drawable/bg_hero_header"
        android:fitsSystemWindows="true"
        app:elevation="0dp">

        <com.google.android.material.appbar.CollapsingToolbarLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:layout_scrollFlags="scroll|exitUntilCollapsed"
            app:contentScrim="@color/primary">

            <!-- Header Content -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical"
                android:gravity="center"
                android:paddingTop="@dimen/spacing_5xl"
                app:layout_collapseMode="parallax">

                <!-- Date -->
                <TextView
                    android:id="@+id/tv_date"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="28/04/2026"
                    android:textAppearance="@style/TextAppearance.Comedor.Headline.Large"
                    android:textColor="@color/on_primary" />

                <!-- Time Range -->
                <TextView
                    android:id="@+id/tv_time_range"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_sm"
                    android:text="Horario para elegir: 08:00 - 11:00"
                    android:textAppearance="@style/TextAppearance.Comedor.Body.Large"
                    android:textColor="@color/on_primary"
                    android:alpha="0.9" />

                <!-- Status Badges -->
                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_lg"
                    android:orientation="horizontal">

                    <TextView
                        android:id="@+id/badge_selected"
                        style="@style/Widget.Comedor.Badge.Selected"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Ya elegiste"
                        android:visibility="gone" />

                    <TextView
                        android:id="@+id/badge_status"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="@dimen/spacing_sm" />
                </LinearLayout>
            </LinearLayout>
        </com.google.android.material.appbar.CollapsingToolbarLayout>
    </com.google.android.material.appbar.AppBarLayout>

    <!-- Content -->
    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior"
        android:clipToPadding="false"
        android:paddingBottom="100dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="@dimen/spacing_lg">

            <!-- Section Title -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginBottom="@dimen/spacing_lg"
                android:text="Menú de hoy"
                android:textAppearance="@style/TextAppearance.Comedor.Title.Large" />

            <!-- Options Label -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginBottom="@dimen/spacing_md"
                android:text="OPCIONES DISPONIBLES"
                android:textAppearance="@style/TextAppearance.Comedor.Label" />

            <!-- Menu Options RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rv_menu_options"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager"
                android:clipToPadding="false" />
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### item_menu_option.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="@dimen/spacing_md"
    style="@style/Widget.Comedor.Card.MenuItem"
    android:checkable="true">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/spacing_lg">

        <!-- Food Icon -->
        <FrameLayout
            android:id="@+id/icon_container"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:background="@drawable/bg_icon_container"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent">

            <ImageView
                android:id="@+id/iv_food_icon"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_restaurant"
                app:tint="@color/secondary" />
        </FrameLayout>

        <!-- Title -->
        <TextView
            android:id="@+id/tv_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/spacing_lg"
            android:layout_marginEnd="@dimen/spacing_lg"
            android:textAppearance="@style/TextAppearance.Comedor.Title.Medium"
            app:layout_constraintStart_toEndOf="@id/icon_container"
            app:layout_constraintEnd_toStartOf="@id/check_indicator"
            app:layout_constraintTop_toTopOf="@id/icon_container" />

        <!-- Selected Status -->
        <TextView
            android:id="@+id/tv_selected_status"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/spacing_lg"
            android:layout_marginTop="@dimen/spacing_xs"
            android:text="Usted ya seleccionó esta opción"
            android:textAppearance="@style/TextAppearance.Comedor.Body.Medium"
            android:textColor="@color/primary"
            android:visibility="gone"
            app:layout_constraintStart_toEndOf="@id/icon_container"
            app:layout_constraintTop_toBottomOf="@id/tv_title" />

        <!-- Check Indicator -->
        <ImageView
            android:id="@+id/check_indicator"
            android:layout_width="28dp"
            android:layout_height="28dp"
            android:src="@drawable/ic_check_circle_animated"
            android:visibility="gone"
            app:tint="@color/primary"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent" />
    </androidx.constraintlayout.widget.ConstraintLayout>
</com.google.android.material.card.MaterialCardView>
```

---

## 9. FUENTES

Descargá Inter de Google Fonts y ponelas en `res/font/`:
- `inter_regular.ttf`
- `inter_medium.ttf`
- `inter_semibold.ttf`
- `inter_bold.ttf`

### res/font/inter.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <font
        android:fontStyle="normal"
        android:fontWeight="400"
        android:font="@font/inter_regular"
        app:fontStyle="normal"
        app:fontWeight="400"
        app:font="@font/inter_regular" />
    <font
        android:fontStyle="normal"
        android:fontWeight="500"
        android:font="@font/inter_medium"
        app:fontStyle="normal"
        app:fontWeight="500"
        app:font="@font/inter_medium" />
    <font
        android:fontStyle="normal"
        android:fontWeight="600"
        android:font="@font/inter_semibold"
        app:fontStyle="normal"
        app:fontWeight="600"
        app:font="@font/inter_semibold" />
    <font
        android:fontStyle="normal"
        android:fontWeight="700"
        android:font="@font/inter_bold"
        app:fontStyle="normal"
        app:fontWeight="700"
        app:font="@font/inter_bold" />
</font-family>
```

---

## 10. TRADUCCION DE ANIMACIONES FRAMER -> ANDROID

| Framer Motion | Android Equivalent |
|---------------|-------------------|
| `initial={{ opacity: 0, y: 20 }}` + `animate={{ opacity: 1, y: 0 }}` | `@anim/fade_in_up.xml` con `Animation.loadAnimation()` |
| `whileTap={{ scale: 0.98 }}` | `@animator/button_press.xml` con `stateListAnimator` |
| `transition={{ type: "spring" }}` | `@android:interpolator/overshoot` |
| `layoutId` transitions | `TransitionManager.beginDelayedTransition()` con `ChangeBounds` |
| `AnimatePresence` | `RecyclerView.ItemAnimator` personalizado |
| Staggered children | `LayoutAnimationController` con delay |
| Infinite pulse | `ObjectAnimator.ofFloat().setRepeatCount(INFINITE)` |
| `whileHover` | No aplica en mobile (usar `state_pressed`) |

### Ejemplo: Animacion de entrada escalonada
```kotlin
// En tu Activity/Fragment
val animation = AnimationUtils.loadLayoutAnimation(
    context, 
    R.anim.layout_animation_fall_down
)
recyclerView.layoutAnimation = animation
recyclerView.scheduleLayoutAnimation()
```

### res/anim/layout_animation_fall_down.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<layoutAnimation xmlns:android="http://schemas.android.com/apk/res/android"
    android:animation="@anim/fade_in_up"
    android:delay="15%"
    android:animationOrder="normal" />
```

---

## 11. LIBRERÍAS RECOMENDADAS

```groovy
// build.gradle (app)
dependencies {
    // Material 3
    implementation 'com.google.android.material:material:1.11.0'
    
    // Animaciones
    implementation 'androidx.dynamicanimation:dynamicanimation:1.0.0'
    
    // Lottie (para confetti y animaciones complejas)
    implementation 'com.airbnb.android:lottie:6.3.0'
    
    // Shimmer loading effect
    implementation 'com.facebook.shimmer:shimmer:0.5.0'
    
    // Blur/Glassmorphism
    implementation 'jp.wasabeef:blurry:4.0.1'
    
    // MotionLayout (ya incluido en ConstraintLayout)
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

---

## 12. TIPS PARA CURSOR

1. **Pasale este archivo completo** como contexto principal
2. **Pasale capturas del prototipo** que ves en la preview de v0
3. **Pedile que genere archivos uno por uno** empezando por:
   - colors.xml
   - dimens.xml  
   - themes.xml
   - styles.xml
   - Los drawables
   - Las animaciones
   - Luego los layouts

4. **Prompt sugerido para Cursor:**
```
Necesito implementar este diseño en Android XML + Material 3.
Tengo la guía de implementación adjunta con todos los valores.
Empezá generando el archivo colors.xml basándote en la sección 1.
```

5. **Para las animaciones complejas** (confetti, shimmer), usá Lottie con archivos JSON de lottiefiles.com

---

## Recursos Adicionales

- **Iconos**: Material Symbols (https://fonts.google.com/icons)
- **Lottie Animations**: https://lottiefiles.com/
- **Colores**: Paleta ya optimizada para accesibilidad WCAG AA
