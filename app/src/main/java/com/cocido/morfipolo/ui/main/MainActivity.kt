package com.cocido.morfipolo.ui.main

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.setupWithNavController
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.ActivityMainBinding
import com.cocido.morfipolo.ui.login.LoginActivity
import com.cocido.morfipolo.util.alarm.AlarmScheduler
import com.cocido.morfipolo.util.widget.MenuWidgetProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Evita mostrar cada diálogo de permisos más de una vez por apertura de la app.
    private var exactAlarmPromptShownThisSession = false
    private var batteryOptimizationPromptShownThisSession = false

    // true mientras el usuario está en la pantalla de Ajustes que abrimos nosotros
    // (alarma exacta o batería). Solo en ese caso onResume() debe intentar avanzar
    // a la siguiente pregunta de la cadena; si no, un onResume "normal" (por ejemplo
    // el que sigue inmediatamente al onCreate) podría disparar un segundo AlertDialog
    // mientras el diálogo de permiso de notificaciones del sistema todavía se está
    // mostrando, superponiendo dos prompts a la vez.
    private var awaitingSettingsReturn = false

    // Launcher para solicitar permiso de notificaciones
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Permiso de notificaciones concedido")
        } else {
            android.util.Log.w("MainActivity", "⚠️ Permiso de notificaciones denegado")
        }
        maybeShowNextPermissionPrompt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Habilitar edge-to-edge (compatible con Android 15+)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // NO aplicar padding al root - dejar que el contenido se extienda detrás de la barra de estado
        // El padding se aplicará solo a los elementos específicos que lo necesiten (como el header naranja)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            // No aplicar padding aquí - el contenido debe extenderse detrás de la barra de estado
            insets
        }
        
        // Configurar insets específicamente para el BottomNavigationView
        // Esto asegura que se ajuste correctamente a la navigation bar sin espacio extra
        ViewCompat.setOnApplyWindowInsetsListener(binding.navView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Aplicar padding inferior solo al BottomNavigationView para que se ajuste a la navigation bar
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }
        
        // Configurar status bar transparente con iconos oscuros (compatible con Android 15+)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = true
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Solicitar permiso de notificaciones si es necesario (Android 13+)
        requestNotificationPermissionIfNeeded()
        // En Android 12-12L (API 31-32) no hay permiso POST_NOTIFICATIONS, así que el
        // flujo de arriba no dispara la cadena de alarmas exactas/batería: cubrirlo acá también.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            maybeShowNextPermissionPrompt()
        }

        // Verificar y refrescar autenticación automáticamente
        // El SessionRefreshWorker debería mantener la sesión activa automáticamente
        lifecycleScope.launch {
            val app = application as MorfipoloApplication
            val authResult = app.authManager.verifyAndRefreshAuth()
            
            when (authResult) {
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                    // Usuario autenticado correctamente, continuar con la app
                    setupNavigation()
                    updateWidget()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError -> {
                    // Error temporal (servidor/red), pero la sesión puede seguir válida localmente
                    if (app.authManager.isSessionLocallyValid()) {
                        android.util.Log.w("MainActivity", "Error temporal pero sesión válida localmente, continuando...")
                        setupNavigation()
                        updateWidget()
                    } else {
                        android.util.Log.w("MainActivity", "Error temporal y sesión expirada localmente, redirigiendo al login")
                        navigateToLogin()
                    }
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed -> {
                    // Si el refresh falló, la sesión expiró - redirigir al login
                    android.util.Log.w("MainActivity", "Sesión expirada (RefreshFailed), redirigiendo al login")
                    navigateToLogin()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                    // No hay sesión guardada, redirigir al login
                    android.util.Log.d("MainActivity", "No hay sesión guardada, redirigiendo al login")
                    navigateToLogin()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Actualizar widget cuando la app vuelve al foreground
        updateWidget()

        // CRÍTICO: Emitir broadcast para que los fragments refresquen sus datos
        // Esto soluciona el bug de sincronización cuando el usuario vota desde la web
        notifyMenuUpdated()

        // Si el usuario acaba de volver de Ajustes tras conceder el permiso, reprogramar
        // ya mismo con setExactAndAllowWhileIdle (antes quedaban con la alarma inexacta
        // programada al iniciar la app hasta el próximo reinicio de proceso).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && AlarmScheduler.canScheduleExact(this)) {
            val app = application as MorfipoloApplication
            AlarmScheduler.scheduleCustomNotifications(this, app.notificationConfigRepository)
        }

        // Solo avanzar la cadena de permisos acá si este resume es porque el usuario
        // volvió de una pantalla de Ajustes que abrimos nosotros. Sin este chequeo,
        // el onResume que sigue inmediatamente al onCreate podría mostrar el diálogo
        // de batería superpuesto con el permiso de notificaciones del sistema, que
        // se pide de forma asincrónica.
        if (awaitingSettingsReturn) {
            awaitingSettingsReturn = false
            maybeShowNextPermissionPrompt()
        }
    }

    /**
     * Cadena de permisos que afectan si los recordatorios llegan con la app cerrada,
     * en orden de impacto: primero alarmas exactas, después optimización de batería.
     * Se muestran de a uno (nunca los dos diálogos superpuestos) y como mucho una vez
     * cada uno por apertura de la app; onResume() vuelve a llamar a esto para avanzar
     * al siguiente cuando el usuario vuelve de Ajustes.
     */
    private fun maybeShowNextPermissionPrompt() {
        if (maybeShowExactAlarmPrompt()) return
        maybeShowBatteryOptimizationPrompt()
    }

    /**
     * Sin esto, el permiso de alarmas exactas (API 31+) solo se pedía si el usuario
     * entraba manualmente a Perfil > Recordatorios y notaba el chip "Activar" — la
     * mayoría nunca lo hacía, así que sus recordatorios quedaban programados con
     * setAndAllowWhileIdle() (inexacta, el sistema puede demorarla o agruparla) en vez
     * de setExactAndAllowWhileIdle(), lo que explica que "no llegue si la app está
     * cerrada". Se pregunta una vez por apertura de la app hasta que se conceda.
     * @return true si se mostró el diálogo ahora (para no encadenar el siguiente permiso).
     */
    private fun maybeShowExactAlarmPrompt(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (exactAlarmPromptShownThisSession) return false
        if (AlarmScheduler.canScheduleExact(this)) return false
        exactAlarmPromptShownThisSession = true

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exact_alarm_prompt_title))
            .setMessage(getString(R.string.exact_alarm_prompt_message))
            .setPositiveButton(getString(R.string.exact_alarm_prompt_positive)) { _, _ ->
                awaitingSettingsReturn = true
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.fromParts("package", packageName, null))
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.exact_alarm_prompt_negative)) { _, _ ->
                maybeShowBatteryOptimizationPrompt()
            }
            .setCancelable(true)
            .show()
        return true
    }

    /**
     * Muchos fabricantes (Xiaomi/MIUI, Oppo/ColorOS, Huawei/EMUI, Samsung, etc.)
     * congelan o matan procesos en segundo plano por su cuenta, más allá de lo que
     * permite el permiso de alarma exacta — es la causa más probable de que un
     * recordatorio no suene en un celular real aunque en el emulador funcione
     * perfecto. Excluir la app de la optimización de batería del sistema es lo único
     * que se puede pedir de forma estándar (no hay API pública para las listas de
     * "autoinicio" propias de cada fabricante; esas hay que activarlas a mano).
     */
    private fun maybeShowBatteryOptimizationPrompt(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        if (batteryOptimizationPromptShownThisSession) return false
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return false
        batteryOptimizationPromptShownThisSession = true

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.battery_optimization_prompt_title))
            .setMessage(getString(R.string.battery_optimization_prompt_message))
            .setPositiveButton(getString(R.string.battery_optimization_prompt_positive)) { _, _ ->
                awaitingSettingsReturn = true
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.fromParts("package", packageName, null))
                    startActivity(intent)
                } catch (e: Exception) {
                    // Algunos fabricantes no implementan este intent estándar; mandar
                    // a la lista general de apps es mejor que no hacer nada.
                    android.util.Log.w("MainActivity", "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS no disponible: ${e.message}")
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (e2: Exception) {
                        android.util.Log.w("MainActivity", "Tampoco hay pantalla de batería disponible: ${e2.message}")
                    }
                }
            }
            .setNegativeButton(getString(R.string.battery_optimization_prompt_negative), null)
            .setCancelable(true)
            .show()
        return true
    }

    /**
     * Notifica a los fragments que deben refrescar el menú/votos.
     * Soluciona el bug de sincronización web-app.
     */
    private fun notifyMenuUpdated() {
        try {
            val updateIntent = Intent("com.cocido.morfipolo.MENU_UPDATED").apply {
                setPackage(packageName)
            }
            sendBroadcast(updateIntent)
            android.util.Log.d("MainActivity", "📱 Broadcast MENU_UPDATED enviado para sincronizar votos")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error al enviar broadcast de actualización", e)
        }
    }
    
    private fun updateWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(this, MenuWidgetProvider::class.java)
            )
            if (appWidgetIds.isNotEmpty()) {
                val updateIntent = Intent(this, MenuWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            // Error silencioso - no crítico
        }
    }
    
    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        
        // Esperar a que el FragmentContainerView esté completamente inicializado
        binding.root.post {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
            val navController = navHostFragment?.navController ?: return@post

            // Configurar BottomNavigationView con NavController
            navView.setupWithNavController(navController)
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("MainActivity", "✅ Permiso de notificaciones ya concedido")
                    maybeShowNextPermissionPrompt()
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    // El usuario denegó el permiso anteriormente, explicar por qué lo necesitamos
                    android.util.Log.d("MainActivity", "Solicitando permiso de notificaciones (ya denegado antes)")
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Primera vez que se solicita
                    android.util.Log.d("MainActivity", "Solicitando permiso de notificaciones por primera vez")
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

}

