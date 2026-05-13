package com.cocido.morfipolo.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.appwidget.AppWidgetManager
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.ActivityLoginBinding
import com.cocido.morfipolo.ui.main.MainActivity
import com.cocido.morfipolo.util.ValidationUtils
import com.cocido.morfipolo.util.widget.MenuWidgetProvider
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var appUpdateManager: AppUpdateManager
    private var immediateUpdateLaunched = false
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory((application as MorfipoloApplication).userRepository)
    }
    
    // Launcher para solicitar permiso de notificaciones
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("LoginActivity", "✅ Permiso de notificaciones concedido")
        } else {
            android.util.Log.w("LoginActivity", "⚠️ Permiso de notificaciones denegado")
        }
    }

    private val immediateUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        immediateUpdateLaunched = false
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(
                this,
                "Debes actualizar la app para continuar.",
                Toast.LENGTH_LONG
            ).show()
            finishAffinity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configurar status bar con iconos oscuros (sin fullscreen para que funcione adjustPan)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.statusBarColor = getColor(R.color.comedor_beige_primary)
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // Solicitar permiso de notificaciones si es necesario (Android 13+)
        requestNotificationPermissionIfNeeded()
        checkForImmediateUpdate()

        setupObservers()
        setupListeners()

        // Verificar y refrescar autenticación automáticamente
        lifecycleScope.launch {
            val app = application as MorfipoloApplication
            val authResult = app.authManager.verifyAndRefreshAuth()
            
            when (authResult) {
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                    // Usuario autenticado correctamente, ir a MainActivity
                    navigateToMain()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError -> {
                    // Error temporal (servidor/red), pero la sesión sigue válida localmente
                    // Verificar si la sesión es válida localmente e ir a MainActivity
                    if (app.authManager.isSessionLocallyValid()) {
                        android.util.Log.d("LoginActivity", "Error temporal pero sesión válida localmente, continuando...")
                        navigateToMain()
                    } else {
                        // La sesión expiró localmente, mantener en login
                        android.util.Log.w("LoginActivity", "Error temporal y sesión expirada localmente")
                    }
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed,
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                    // Mantener en login, no hacer nada
                }
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Idle -> {
                        setLoading(false)
                        binding.errorTextView.visibility = android.view.View.GONE
                    }
                    is LoginUiState.Loading -> {
                        setLoading(true)
                        binding.errorTextView.visibility = android.view.View.GONE
                    }
                    is LoginUiState.Success -> {
                        setLoading(false)
                        // Actualizar widget después del login exitoso
                        updateWidget()
                        // Programar recordatorio diario después del login
                        com.cocido.morfipolo.util.work.DailyReminderWorker.scheduleDailyReminder(this@LoginActivity)
                        navigateToMain()
                    }
                    is LoginUiState.Error -> {
                        setLoading(false)
                        handleError(state.message)
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        if (isLoading) {
            binding.loginButton.text = "Ingresando..."
            binding.loginButton.alpha = 0.7f
        } else {
            binding.loginButton.text = getString(R.string.login_button)
            binding.loginButton.alpha = 1.0f
        }
    }

    private fun handleError(message: String) {
        // Limpiar errores previos
        binding.dniInputLayout.error = null
        binding.passwordInputLayout.error = null
        binding.errorTextView.visibility = android.view.View.GONE

        when {
            // Prioridad 1: Credenciales incorrectas (Error general)
            message.contains("incorrectos", ignoreCase = true) ||
            message.contains("inválidos", ignoreCase = true) -> {
                binding.errorTextView.text = message
                binding.errorTextView.visibility = android.view.View.VISIBLE
                shakeView(binding.loginButton)
            }
            // Prioridad 2: Errores específicos de campo
            message.contains("DNI", ignoreCase = true) -> {
                binding.dniInputLayout.error = message
                binding.dniEditText.requestFocus()
            }
            message.contains("contraseña", ignoreCase = true) || 
            message.contains("password", ignoreCase = true) -> {
                binding.passwordInputLayout.error = message
                binding.passwordEditText.requestFocus()
            }
            // Prioridad 3: Errores generales de red/servidor
            else -> {
                val friendlyMessage = when {
                    message.contains("conexión", ignoreCase = true) || 
                    message.contains("connection", ignoreCase = true) ||
                    message.contains("conectar", ignoreCase = true) -> {
                        getString(R.string.error_connection)
                    }
                    message.contains("servidor", ignoreCase = true) ||
                    message.contains("server", ignoreCase = true) -> {
                        getString(R.string.error_server)
                    }
                    else -> getString(R.string.error_login_failed)
                }
                binding.errorTextView.text = friendlyMessage
                binding.errorTextView.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun shakeView(view: android.view.View) {
        val rotate = android.view.animation.TranslateAnimation(0f, 10f, 0f, 0f)
        rotate.duration = 50
        rotate.repeatCount = 5
        rotate.repeatMode = android.view.animation.Animation.REVERSE
        view.startAnimation(rotate)
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val dni = binding.dniEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            
            // Validar formato de DNI
            if (!ValidationUtils.isValidDni(dni)) {
                binding.dniInputLayout.error = getString(R.string.dni_invalid_format)
                return@setOnClickListener
            }
            
            viewModel.login(dni, password)
        }
        
        // Limpiar error cuando el usuario empieza a escribir
        binding.dniEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.dniInputLayout.error = null
                binding.errorTextView.visibility = android.view.View.GONE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.passwordEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.passwordInputLayout.error = null
                binding.errorTextView.visibility = android.view.View.GONE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        // Scroll automático al botón cuando los campos reciben foco
        binding.dniEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollToButton()
            }
        }
        
        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollToButton()
            }
        }
    }
    
    private fun scrollToButton() {
        binding.scrollView.postDelayed({
            binding.scrollView.smoothScrollTo(0, binding.loginButton.bottom)
        }, 300) // Esperar a que el teclado aparezca
    }

    override fun onStart() {
        super.onStart()
        startEntryAnimation()
    }

    override fun onResume() {
        super.onResume()
        checkForImmediateUpdate()
    }

    private fun startEntryAnimation() {
        val views = listOf(
            binding.logoImageView,
            binding.titleTextView,
            binding.dniInputLayout,
            binding.passwordInputLayout,
            binding.loginButton
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 100L)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkForImmediateUpdate() {
        if (immediateUpdateLaunched) return

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                val updateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val updateInProgress = appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                val immediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

                if ((updateAvailable && immediateAllowed) || updateInProgress) {
                    immediateUpdateLaunched = true
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        immediateUpdateLauncher,
                        com.google.android.play.core.appupdate.AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
            .addOnFailureListener {
                android.util.Log.w("LoginActivity", "No se pudo consultar In-App Update: ${it.message}")
            }
    }
    
    private fun updateWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(this, MenuWidgetProvider::class.java)
            )
            if (appWidgetIds.isNotEmpty()) {
                android.util.Log.d("LoginActivity", "Actualizando ${appWidgetIds.size} widgets después del login")
                val updateIntent = Intent(this, MenuWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Error al actualizar widget después del login", e)
        }
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("LoginActivity", "✅ Permiso de notificaciones ya concedido")
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    // El usuario denegó el permiso anteriormente, explicar por qué lo necesitamos
                    android.util.Log.d("LoginActivity", "Solicitando permiso de notificaciones (ya denegado antes)")
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Primera vez que se solicita
                    android.util.Log.d("LoginActivity", "Solicitando permiso de notificaciones por primera vez")
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

class LoginViewModelFactory(private val userRepository: com.cocido.morfipolo.data.repository.UserRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}




