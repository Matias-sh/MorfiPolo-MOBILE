package com.cocido.morfipolo.util.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Widget provider para mostrar el menú del día.
 * 
 * Enfoque simplificado:
 * - Carga datos de forma asíncrona usando corrutinas
 * - Muestra estado de carga inmediatamente
 * - Maneja errores de forma robusta
 * - Actualiza el widget solo cuando hay datos válidos
 */
class MenuWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "MenuWidgetProvider"
        const val ACTION_SELECT_OPTION = "com.cocido.morfipolo.SELECT_OPTION"
        const val ACTION_DELETE_VOTE = "com.cocido.morfipolo.DELETE_VOTE"
        const val EXTRA_MENU_ID = "menu_id"
        const val EXTRA_OPTION_ID = "option_id"
        const val EXTRA_VOTE_ID = "vote_id"
        const val EXTRA_OPTION_INDEX = "option_index"
    }

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        android.util.Log.d(TAG, "=== onUpdate llamado con ${appWidgetIds.size} widgets ===")
        appWidgetIds.forEach { appWidgetId ->
            android.util.Log.d(TAG, "onUpdate: Procesando widget $appWidgetId")
            try {
                // CRÍTICO: Usar widget_menu_simple desde el principio para evitar cambio de layout
                android.util.Log.d(TAG, "onUpdate: Creando RemoteViews con widget_menu_simple")
                val initialViews = try {
                    RemoteViews(context.packageName, R.layout.widget_menu_simple)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "onUpdate: ERROR al crear RemoteViews", e)
                    e.printStackTrace()
                    return@forEach
                }
                android.util.Log.d(TAG, "onUpdate: RemoteViews creado exitosamente")
                
                // Configurar estado inicial: mostrar solo el texto de carga, ocultar todo lo demás
                try {
                    initialViews.setTextViewText(R.id.widgetDateTextView, "Menú del Día - Cargando...")
                    initialViews.setTextViewText(R.id.widgetMenuDescriptionTextView, "")
                    initialViews.setViewVisibility(R.id.widgetStatusTextView, View.GONE)
                    initialViews.setViewVisibility(R.id.widgetNoMenuTextView, View.GONE)
                    android.util.Log.d(TAG, "onUpdate: Estado inicial configurado")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "onUpdate: ERROR al configurar estado inicial", e)
                    e.printStackTrace()
                }
                
                // Actualizar widget
                try {
                    appWidgetManager.updateAppWidget(appWidgetId, initialViews)
                    android.util.Log.d(TAG, "onUpdate: ✅ Widget $appWidgetId actualizado con estado inicial exitosamente")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "onUpdate: ❌ ERROR al actualizar widget $appWidgetId", e)
                    e.printStackTrace()
                    throw e // Re-lanzar para que se capture arriba
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "onUpdate: ❌ ERROR CRÍTICO al procesar widget $appWidgetId", e)
                e.printStackTrace()
                // Intentar mostrar un widget de error
                try {
                    val errorViews = RemoteViews(context.packageName, R.layout.widget_menu_simple)
                    errorViews.setTextViewText(R.id.widgetDateTextView, "Error al cargar")
                    errorViews.setViewVisibility(R.id.widgetMenuDescriptionTextView, View.GONE)
                    errorViews.setViewVisibility(R.id.widgetStatusTextView, View.GONE)
                    errorViews.setViewVisibility(R.id.widgetNoMenuTextView, View.GONE)
                    appWidgetManager.updateAppWidget(appWidgetId, errorViews)
                } catch (e2: Exception) {
                    android.util.Log.e(TAG, "onUpdate: ❌ ERROR incluso al mostrar widget de error", e2)
                    e2.printStackTrace()
                }
                return@forEach
            }
            // Luego actualizar con datos
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, MenuWidgetProvider::class.java)
        )
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        widgetScope.coroutineContext.cancel()
    }

    override fun onReceive(context: Context, intent: Intent) {
        // CRÍTICO: Este log debe aparecer SIEMPRE que se reciba cualquier intent
        android.util.Log.d(TAG, "═══════════════════════════════════════════════════════════")
        android.util.Log.d(TAG, "onReceive: ⚡ INTENT RECIBIDO - SIEMPRE DEBE APARECER ESTE LOG")
        android.util.Log.d(TAG, "onReceive: Action: ${intent.action}")
        android.util.Log.d(TAG, "onReceive: Data URI: ${intent.data}")
        android.util.Log.d(TAG, "onReceive: Component: ${intent.component}")
        android.util.Log.d(TAG, "onReceive: Intent completo: $intent")
        android.util.Log.d(TAG, "onReceive: Intent extras count: ${intent.extras?.size() ?: 0}")
        
        // IMPORTANTE: Cuando usamos FillInIntent con template, el intent siempre tiene ACTION_APPWIDGET_UPDATE
        // Los extras del FillInIntent (action, menu_id, option_id, vote_id) se fusionan con el template intent
        if (intent.action == android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            // Verificar si tiene extras de FillInIntent (viene de un click en un item)
            val actionFromExtras = intent.getStringExtra("action")
            val hasFillInIntentExtras = actionFromExtras != null || 
                intent.hasExtra(EXTRA_MENU_ID) || 
                intent.hasExtra(EXTRA_VOTE_ID) || 
                intent.hasExtra(EXTRA_OPTION_ID)
            
            android.util.Log.d(TAG, "onReceive: Verificando extras de FillInIntent:")
            android.util.Log.d(TAG, "onReceive:   -> actionFromExtras: '$actionFromExtras'")
            android.util.Log.d(TAG, "onReceive:   -> hasExtra(${EXTRA_MENU_ID}): ${intent.hasExtra(EXTRA_MENU_ID)}")
            android.util.Log.d(TAG, "onReceive:   -> hasExtra(${EXTRA_VOTE_ID}): ${intent.hasExtra(EXTRA_VOTE_ID)}")
            android.util.Log.d(TAG, "onReceive:   -> hasExtra(${EXTRA_OPTION_ID}): ${intent.hasExtra(EXTRA_OPTION_ID)}")
            android.util.Log.d(TAG, "onReceive:   -> hasFillInIntentExtras: $hasFillInIntentExtras")
            
            // Log de todos los extras disponibles
            val allExtras = intent.extras?.keySet()?.joinToString(", ") ?: "ninguno"
            android.util.Log.d(TAG, "onReceive:   -> Todos los extras: $allExtras")
            
            // Log detallado de cada extra
            intent.extras?.keySet()?.forEach { key ->
                val value = intent.extras?.get(key)
                android.util.Log.d(TAG, "onReceive:   -> Extra '$key': $value (tipo: ${value?.javaClass?.simpleName})")
            }
            
            if (hasFillInIntentExtras && actionFromExtras != null) {
                // Es un click de un item de la lista - procesar según la action en los extras
                android.util.Log.d(TAG, "onReceive: ✅ CLICK DE ITEM DETECTADO")
                
                // Obtener appWidgetId - puede venir en extras o en EXTRA_APPWIDGET_ID
                val appWidgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (appWidgetId == -1) {
                    // Intentar obtener desde extras como int[]
                    val appWidgetIds = intent.getIntArrayExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                        // Usar el primer widget ID
                        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[0])
                        android.util.Log.d(TAG, "onReceive: appWidgetId obtenido de EXTRA_APPWIDGET_IDS: ${appWidgetIds[0]}")
                    } else {
                        android.util.Log.w(TAG, "onReceive: ⚠️ No se pudo obtener appWidgetId")
                    }
                } else {
                    android.util.Log.d(TAG, "onReceive: appWidgetId obtenido de EXTRA_APPWIDGET_ID: $appWidgetId")
                }
                
                when (actionFromExtras) {
                    ACTION_SELECT_OPTION -> {
                        android.util.Log.d(TAG, "onReceive: 🎯 PROCESANDO ACTION_SELECT_OPTION desde FillInIntent")
                        android.util.Log.d(TAG, "onReceive:   -> menuId: ${intent.getStringExtra(EXTRA_MENU_ID)}")
                        android.util.Log.d(TAG, "onReceive:   -> optionId: ${intent.getStringExtra(EXTRA_OPTION_ID)}")
                        android.util.Log.d(TAG, "onReceive:   -> optionIndex: ${intent.getIntExtra(EXTRA_OPTION_INDEX, -1)}")
                        handleSelectOption(context, intent)
                        android.util.Log.d(TAG, "onReceive: ✅ handleSelectOption llamado")
                    }
                    ACTION_DELETE_VOTE -> {
                        android.util.Log.d(TAG, "onReceive: 🗑️ PROCESANDO ACTION_DELETE_VOTE desde FillInIntent")
                        android.util.Log.d(TAG, "onReceive:   -> voteId: ${intent.getStringExtra(EXTRA_VOTE_ID)}")
                        android.util.Log.d(TAG, "onReceive:   -> optionIndex: ${intent.getIntExtra(EXTRA_OPTION_INDEX, -1)}")
                        handleDeleteVote(context, intent)
                        android.util.Log.d(TAG, "onReceive: ✅ handleDeleteVote llamado")
                    }
                    else -> {
                        android.util.Log.w(TAG, "onReceive: ⚠️ Action desconocida en extras: '$actionFromExtras'")
                        super.onReceive(context, intent)
                    }
                }
                android.util.Log.d(TAG, "═══════════════════════════════════════════════════════════")
                return
            } else {
                // Es una actualización normal del widget
                android.util.Log.d(TAG, "onReceive: 📋 ACTUALIZACIÓN NORMAL DEL WIDGET")
                super.onReceive(context, intent)
                android.util.Log.d(TAG, "═══════════════════════════════════════════════════════════")
                return
            }
        }
        
        // Para actions directas (por si acaso)
        when (intent.action) {
            ACTION_SELECT_OPTION -> {
                android.util.Log.d(TAG, "onReceive: 🎯 PROCESANDO ACTION_SELECT_OPTION DIRECTA")
                handleSelectOption(context, intent)
            }
            ACTION_DELETE_VOTE -> {
                android.util.Log.d(TAG, "onReceive: 🗑️ PROCESANDO ACTION_DELETE_VOTE DIRECTA")
                handleDeleteVote(context, intent)
            }
            else -> {
                android.util.Log.d(TAG, "onReceive: ⚠️ Action desconocida: '${intent.action}'")
                super.onReceive(context, intent)
            }
        }
        android.util.Log.d(TAG, "═══════════════════════════════════════════════════════════")
    }

    /**
     * Actualiza un widget específico.
     * Enfoque: mostrar estado de carga inmediatamente, luego cargar datos y actualizar.
     */
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        android.util.Log.d(TAG, "updateWidget: Iniciando actualización para widget $appWidgetId")
        
        // Cargar datos de forma asíncrona
        widgetScope.launch {
            try {
                android.util.Log.d(TAG, "updateWidget: Obteniendo aplicación...")
                val app = context.applicationContext as? MorfipoloApplication
                    ?: run {
                        android.util.Log.e(TAG, "updateWidget: ERROR - No se pudo obtener MorfipoloApplication")
                        showErrorState(context, appWidgetManager, appWidgetId, "Error de inicialización")
                        return@launch
                    }
                android.util.Log.d(TAG, "updateWidget: Aplicación obtenida correctamente")

                // Verificar autenticación
                android.util.Log.d(TAG, "updateWidget: Verificando autenticación...")
                if (!app.sessionManager.isLoggedIn()) {
                    android.util.Log.w(TAG, "updateWidget: Usuario no logueado")
                    showNotLoggedInState(context, appWidgetManager, appWidgetId)
                    return@launch
                }
                android.util.Log.d(TAG, "updateWidget: Usuario logueado")

                // Verificar y refrescar autenticación
                android.util.Log.d(TAG, "updateWidget: Verificando y refrescando autenticación...")
                val authResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    try {
                        app.authManager.verifyAndRefreshAuth()
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "updateWidget: Error al verificar autenticación", e)
                        null
                    }
                }

                when (authResult) {
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                        android.util.Log.d(TAG, "updateWidget: Autenticación exitosa")
                    }
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError -> {
                        android.util.Log.w(TAG, "updateWidget: Error temporal de autenticación, continuando...")
                        // Continuar intentando cargar el menú
                    }
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed,
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn,
                    null -> {
                        android.util.Log.w(TAG, "updateWidget: Autenticación fallida")
                        showNotLoggedInState(context, appWidgetManager, appWidgetId)
                        return@launch
                    }
                }

                // Obtener menú del día
                android.util.Log.d(TAG, "updateWidget: Obteniendo menú del día...")
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val menu = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    try {
                        app.menuRepository.getMenuByDate(today.time)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "updateWidget: Error al obtener menú", e)
                        null
                    }
                }

                if (menu == null) {
                    android.util.Log.w(TAG, "updateWidget: No hay menú disponible")
                    showNoMenuState(context, appWidgetManager, appWidgetId)
                    return@launch
                }
                android.util.Log.d(TAG, "updateWidget: Menú obtenido: ${menu.id}, opciones: ${menu.getOptionsOrEmpty().size}")

                val options = menu.getOptionsOrEmpty()
                if (options.isEmpty()) {
                    android.util.Log.w(TAG, "updateWidget: Menú sin opciones")
                    showNoOptionsState(context, appWidgetManager, appWidgetId, menu)
                    return@launch
                }
                android.util.Log.d(TAG, "updateWidget: Opciones encontradas: ${options.size}")

                // Obtener voto del usuario
                android.util.Log.d(TAG, "updateWidget: Obteniendo voto del usuario...")
                val userId = app.sessionManager.getCurrentUserId()
                if (userId == null) {
                    android.util.Log.w(TAG, "updateWidget: No hay userId")
                    showNotLoggedInState(context, appWidgetManager, appWidgetId)
                    return@launch
                }

                val userVote = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    try {
                        app.voteRepository.getUserVoteForMenu(menu.id, userId)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "updateWidget: Error al obtener voto", e)
                        null
                    }
                }
                android.util.Log.d(TAG, "updateWidget: Voto obtenido: ${if (userVote != null) "Sí" else "No"}")

                // Mostrar menú con datos
                android.util.Log.d(TAG, "updateWidget: Mostrando estado del menú...")
                showMenuState(context, appWidgetManager, appWidgetId, menu, options, userVote)
                android.util.Log.d(TAG, "updateWidget: ✅ Widget actualizado exitosamente")

            } catch (e: Exception) {
                android.util.Log.e(TAG, "updateWidget: ❌ ERROR al actualizar widget", e)
                e.printStackTrace()
                showErrorState(context, appWidgetManager, appWidgetId, "Error al cargar datos")
            }
        }
    }

    /**
     * Muestra el estado de carga.
     */
    private fun showLoadingState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        android.util.Log.d(TAG, "showLoadingState: Mostrando estado de carga para widget $appWidgetId")
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_menu_simple)
            views.setTextViewText(R.id.widgetDateTextView, "Menú del Día - Cargando...")
            views.setTextViewText(R.id.widgetMenuDescriptionTextView, "")
            views.setViewVisibility(R.id.widgetStatusTextView, View.GONE)
            views.setViewVisibility(R.id.widgetOption1Container, View.GONE)
            views.setViewVisibility(R.id.widgetOption2Container, View.GONE)
            views.setViewVisibility(R.id.widgetNoMenuTextView, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            android.util.Log.d(TAG, "showLoadingState: ✅ Estado de carga mostrado")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "showLoadingState: ❌ ERROR", e)
            e.printStackTrace()
        }
    }

    /**
     * Muestra el estado cuando no hay usuario logueado.
     */
    private fun showNotLoggedInState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_menu_simple)
            views.setTextViewText(R.id.widgetDateTextView, "Inicia sesión para ver el menú")
            views.setTextViewText(R.id.widgetMenuDescriptionTextView, "")
            views.setViewVisibility(R.id.widgetStatusTextView, View.GONE)
            views.setViewVisibility(R.id.widgetOption1Container, View.GONE)
            views.setViewVisibility(R.id.widgetOption2Container, View.GONE)
            views.setViewVisibility(R.id.widgetNoMenuTextView, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "showNotLoggedInState: ❌ ERROR", e)
            e.printStackTrace()
        }
    }

    /**
     * Muestra el estado cuando no hay menú disponible.
     */
    private fun showNoMenuState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_menu_simple)
            views.setTextViewText(R.id.widgetDateTextView, "No hay menú disponible")
            views.setTextViewText(R.id.widgetMenuDescriptionTextView, "")
            views.setViewVisibility(R.id.widgetStatusTextView, View.GONE)
            views.setViewVisibility(R.id.widgetNoMenuTextView, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "showNoMenuState: ❌ ERROR", e)
            e.printStackTrace()
        }
    }

    /**
     * Muestra el estado cuando no hay opciones disponibles.
     */
    private fun showNoOptionsState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        menu: com.cocido.morfipolo.domain.model.Menu
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_menu_simple)
            views.setTextViewText(R.id.widgetDateTextView, "${formatDate(menu.date)} - Sin opciones")
            views.setTextViewText(R.id.widgetMenuDescriptionTextView, menu.description)
            views.setViewVisibility(R.id.widgetStatusTextView, View.GONE)
            views.setViewVisibility(R.id.widgetNoMenuTextView, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "showNoOptionsState: ❌ ERROR", e)
            e.printStackTrace()
        }
    }

    /**
     * Muestra el estado de error.
     */
    private fun showErrorState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        errorMessage: String
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_menu_simple)
            views.setTextViewText(R.id.widgetDateTextView, "Error: $errorMessage")
            views.setTextViewText(R.id.widgetMenuDescriptionTextView, "")
            views.setViewVisibility(R.id.widgetStatusTextView, View.GONE)
            views.setViewVisibility(R.id.widgetNoMenuTextView, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "showErrorState: ❌ ERROR", e)
            e.printStackTrace()
        }
    }

    /**
     * Muestra el estado con el menú y opciones.
     */
    private fun showMenuState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        menu: com.cocido.morfipolo.domain.model.Menu,
        options: List<com.cocido.morfipolo.domain.model.MenuOption>,
        userVote: com.cocido.morfipolo.domain.model.Vote?
    ) {
        android.util.Log.d(TAG, "showMenuState: Mostrando menú para widget $appWidgetId")
        try {
            // CRÍTICO: Usar widget_menu_simple que tiene todos los elementos necesarios
            val views = RemoteViews(context.packageName, R.layout.widget_menu_simple)
            android.util.Log.d(TAG, "showMenuState: RemoteViews creado con widget_menu_simple")
            
            // Configurar textos principales
            views.setTextViewText(R.id.widgetDateTextView, formatDate(menu.date))
            views.setTextViewText(R.id.widgetMenuDescriptionTextView, menu.description)
            android.util.Log.d(TAG, "showMenuState: Textos principales configurados")
            
            // Configurar estado del menú - validar si realmente está abierto según el horario (08:00 - 11:00)
            val isWithinTime = isWithinSelectionTime(menu)
            val isActuallyOpen = menu.status == "open" && isWithinTime
            val statusText = when {
                isActuallyOpen -> "Abierto"
                menu.status == "closed" -> "Cerrado"
                else -> "Cerrado" // Si pasó el horario, mostrar cerrado
            }
            // Usar color sólido en lugar de drawable para evitar problemas de recursos
            views.setViewVisibility(R.id.widgetStatusTextView, View.VISIBLE)
            views.setTextViewText(R.id.widgetStatusTextView, statusText)
            try {
                val statusColor = if (isActuallyOpen) {
                    0xFF6B8E23.toInt() // Verde Nonna
                } else {
                    0xFFC85A5A.toInt() // Rojo Nonna
                }
                views.setInt(R.id.widgetStatusTextView, "setBackgroundColor", statusColor)
                android.util.Log.d(TAG, "showMenuState: Estado del menú configurado: $statusText")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "showMenuState: Error al configurar color de estado", e)
                // Continuar sin el color de fondo
            }
            
            // CRÍTICO: Ocultar mensaje de error ANTES de mostrar contenido
            views.setViewVisibility(R.id.widgetNoMenuTextView, View.GONE)
            views.setTextViewText(R.id.widgetNoMenuTextView, "") // Limpiar texto
            android.util.Log.d(TAG, "showMenuState: Mensaje de error oculto")
            
            // Configurar ListView con RemoteViewsService para lista dinámica
            try {
                // CRÍTICO: Usar un URI de datos único con timestamp para forzar recreación del Factory
                // Esto asegura que Android cree una nueva instancia del RemoteViewsService
                // cuando los datos cambian, forzando que onDataSetChanged() se llame
                val timestamp = System.currentTimeMillis()
                val serviceIntent = Intent(context, MenuWidgetService::class.java).apply {
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    // Usar un URI de datos único con timestamp para forzar recreación del Factory
                    data = android.net.Uri.parse("widget://service/${appWidgetId}?t=$timestamp")
                }
                android.util.Log.d(TAG, "showMenuState: ServiceIntent con data URI único (timestamp=$timestamp): ${serviceIntent.data}")
                
                android.util.Log.d(TAG, "showMenuState: Configurando RemoteViewsService:")
                android.util.Log.d(TAG, "showMenuState:   -> ServiceIntent action: ${serviceIntent.action}")
                android.util.Log.d(TAG, "showMenuState:   -> ServiceIntent data URI: ${serviceIntent.data}")
                android.util.Log.d(TAG, "showMenuState:   -> ServiceIntent extras: ${serviceIntent.extras?.keySet()?.joinToString(", ")}")
                
                views.setRemoteAdapter(R.id.widgetOptionsList, serviceIntent)
                android.util.Log.d(TAG, "showMenuState: ✅ ListView configurado con RemoteViewsService")
                
                // CRÍTICO: Para listas dinámicas en widgets, DEBEMOS usar setPendingIntentTemplate + setOnClickFillInIntent
                // No podemos usar setOnClickPendingIntent directamente en items de un ListView dinámico
                // IMPORTANTE: El template NO debe tener data URI ni extras - solo la action
                // Los extras se añaden en el FillInIntent y Android los fusiona
                val clickTemplateIntent = Intent(context, MenuWidgetProvider::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    // NO usar data URI ni extras aquí - Android fusionará los extras del FillInIntent
                    // El data URI puede interferir con la fusión de extras
                }
                // CRÍTICO: Usar FLAG_MUTABLE para permitir que Android modifique el intent con los extras del FillInIntent
                // FLAG_IMMUTABLE puede impedir que Android fusione los extras correctamente
                val clickTemplatePendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    clickTemplateIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setPendingIntentTemplate(R.id.widgetOptionsList, clickTemplatePendingIntent)
                android.util.Log.d(TAG, "showMenuState: ✅ Template de PendingIntent configurado para ListView (sin data URI ni extras - solo action)")
                
                // IMPORTANTE: Notificar al widget que los datos cambiaron para que actualice la lista
                // Esto fuerza a Android a llamar onDataSetChanged() y getViewAt() en el RemoteViewsFactory
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetOptionsList)
                android.util.Log.d(TAG, "showMenuState: ✅ Notificando actualización de datos de ListView (appWidgetId=$appWidgetId, viewId=${R.id.widgetOptionsList})")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "showMenuState: ❌ ERROR al configurar ListView", e)
                e.printStackTrace()
            }
            
            configureClickIntent(context, views)
            android.util.Log.d(TAG, "showMenuState: Llamando updateAppWidget...")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            android.util.Log.d(TAG, "showMenuState: ✅ Widget actualizado con menú")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "showMenuState: ❌ ERROR", e)
            e.printStackTrace()
        }
    }

    /**
     * Configura una opción del menú en el widget.
     */
    private fun configureOption(
        context: Context,
        views: RemoteViews,
        optionContainerId: Int,
        optionNameId: Int,
        optionButtonId: Int,
        optionSelectedIndicatorId: Int,
        option: com.cocido.morfipolo.domain.model.MenuOption?,
        optionIndex: Int,
        menuId: String,
        userVote: com.cocido.morfipolo.domain.model.Vote?,
        canVote: Boolean,
        totalOptions: Int
    ) {
        if (option == null) {
            views.setViewVisibility(optionContainerId, View.GONE)
            return
        }

        views.setViewVisibility(optionContainerId, View.VISIBLE)
        
        // Configurar nombre de la opción
        val optionName = if (totalOptions > 1) {
            "Opción ${optionIndex + 1}: ${option.name}"
        } else {
            option.name
        }
        views.setTextViewText(optionNameId, optionName)
        
        // Verificar si está seleccionada
        val isSelected = userVote?.option?.id == option.id
        
        // Mostrar indicador de selección (solo si existe en el layout)
        // Nota: widget_menu_simple no tiene estos indicadores, así que los ignoramos silenciosamente
        try {
            // Intentar usar el indicador solo si el ID es válido (no 0)
            if (optionSelectedIndicatorId != 0) {
                views.setViewVisibility(
                    optionSelectedIndicatorId,
                    if (isSelected) View.VISIBLE else View.GONE
                )
            }
        } catch (e: Exception) {
            // El indicador no existe en el layout simplificado, ignorar silenciosamente
            android.util.Log.d(TAG, "configureOption: Indicador de selección no disponible (esto es normal en widget_menu_simple)")
        }
        
        // Configurar botón (TextView) - usar drawables para compatibilidad con RemoteViews
        try {
            if (isSelected && userVote != null) {
                // Botón para quitar selección
                views.setTextViewText(optionButtonId, "Quitar selección")
                try {
                    // Usar drawable rojo para el botón
                    views.setInt(optionButtonId, "setBackgroundResource", R.drawable.button_red)
                } catch (e: Exception) {
                    android.util.Log.d(TAG, "configureOption: Error al configurar drawable de botón rojo, usando color sólido")
                    try {
                        views.setInt(optionButtonId, "setBackgroundColor", 0xFFC85A5A.toInt()) // Rojo Nonna
                    } catch (e2: Exception) {
                        android.util.Log.e(TAG, "configureOption: Error al configurar color de botón rojo", e2)
                    }
                }
                try {
                    val pendingIntent = createPendingIntent(
                        context,
                        ACTION_DELETE_VOTE,
                        userVote.id,
                        optionIndex
                    )
                    views.setOnClickPendingIntent(optionButtonId, pendingIntent)
                    android.util.Log.d(TAG, "configureOption: PendingIntent configurado para quitar selección")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "configureOption: Error al configurar PendingIntent para quitar selección", e)
                }
            } else {
                // Botón para seleccionar
                views.setTextViewText(optionButtonId, if (canVote) "Elegir esta opción" else "No disponible")
                try {
                    if (canVote) {
                        // Usar drawable del botón primario
                        views.setInt(optionButtonId, "setBackgroundResource", R.drawable.button_primary_solid)
                    } else {
                        // Usar color gris cuando no se puede votar
                        views.setInt(optionButtonId, "setBackgroundColor", 0xFFA1887F.toInt()) // Gris Nonna
                    }
                } catch (e: Exception) {
                    android.util.Log.d(TAG, "configureOption: Error al configurar drawable de botón, usando color sólido")
                    try {
                        val buttonColor = if (canVote) 0xFF8B6F47.toInt() else 0xFFA1887F.toInt() // Marrón Nonna o Gris
                        views.setInt(optionButtonId, "setBackgroundColor", buttonColor)
                    } catch (e2: Exception) {
                        android.util.Log.e(TAG, "configureOption: Error al configurar color de botón", e2)
                    }
                }
                if (canVote) {
                    try {
                        val pendingIntent = createPendingIntent(
                            context,
                            ACTION_SELECT_OPTION,
                            menuId,
                            optionIndex,
                            option.id
                        )
                        views.setOnClickPendingIntent(optionButtonId, pendingIntent)
                        android.util.Log.d(TAG, "configureOption: PendingIntent configurado para seleccionar")
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "configureOption: Error al configurar PendingIntent para seleccionar", e)
                    }
                } else {
                    // Si no se puede votar, eliminar cualquier PendingIntent previo
                    try {
                        views.setOnClickPendingIntent(optionButtonId, null)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "configureOption: Error al eliminar PendingIntent", e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "configureOption: ERROR general al configurar botón", e)
            e.printStackTrace()
        }
    }

    /**
     * Configura el intent de click principal del widget.
     */
    private fun configureClickIntent(context: Context, views: RemoteViews) {
        try {
            // IMPORTANTE: Solo configurar click en el header del widget, NO en todo el contenedor
            // De esta manera, los clicks en los items de la lista (botones) no abrirán la app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            // Configurar click solo en el TextView de la fecha (header)
            // Esto permite que los clicks en los botones de los items no abran la app
            views.setOnClickPendingIntent(R.id.widgetDateTextView, pendingIntent)
            views.setOnClickPendingIntent(R.id.widgetMenuDescriptionTextView, pendingIntent)
            android.util.Log.d(TAG, "configureClickIntent: Click intent configurado solo en header del widget")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "configureClickIntent: ERROR al configurar click intent", e)
            e.printStackTrace()
        }
    }

    /**
     * Crea un PendingIntent para acciones del widget.
     */
    private fun createPendingIntent(
        context: Context,
        action: String,
        menuId: String,
        optionIndex: Int,
        optionId: String? = null
    ): PendingIntent {
        val intent = Intent(context, MenuWidgetProvider::class.java).apply {
            this.action = action
            putExtra(EXTRA_MENU_ID, menuId)
            putExtra(EXTRA_OPTION_ID, optionId)
            putExtra(EXTRA_OPTION_INDEX, optionIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            (menuId + optionIndex).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Crea un PendingIntent para eliminar voto.
     */
    private fun createPendingIntent(
        context: Context,
        action: String,
        voteId: String,
        optionIndex: Int
    ): PendingIntent {
        val intent = Intent(context, MenuWidgetProvider::class.java).apply {
            this.action = action
            putExtra(EXTRA_VOTE_ID, voteId)
            putExtra(EXTRA_OPTION_INDEX, optionIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            (voteId + optionIndex).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Maneja la selección de una opción.
     */
    private fun handleSelectOption(context: Context, intent: Intent) {
        val menuId = intent.getStringExtra(EXTRA_MENU_ID)
        val optionId = intent.getStringExtra(EXTRA_OPTION_ID)
        val appWidgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        
        if (menuId == null || optionId == null) {
            android.util.Log.w(TAG, "handleSelectOption: ⚠️ menuId o optionId es null")
            return
        }
        
        widgetScope.launch {
            try {
                val app = context.applicationContext as? MorfipoloApplication ?: return@launch
                val userId = app.sessionManager.getCurrentUserId() ?: return@launch
                
                android.util.Log.d(TAG, "handleSelectOption: 🎯 Seleccionando opción $optionId del menú $menuId...")
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    app.voteRepository.createVoteOrReplace(optionId, menuId, userId)
                }
                
                android.util.Log.d(TAG, "handleSelectOption: Resultado: ${if (result.isSuccess) "Éxito" else "Error: ${result.exceptionOrNull()?.message}"}")
                
                // Verificar si es un error de horario cerrado
                if (!result.isSuccess) {
                    val exception = result.exceptionOrNull()
                    val message = exception?.message ?: ""
                    
                    if (message.contains("cerrado", ignoreCase = true) || 
                        message.contains("horario", ignoreCase = true) || 
                        message.contains("time", ignoreCase = true) || 
                        message.contains("expired", ignoreCase = true) ||
                        message.contains("closed", ignoreCase = true)) {
                        // Mostrar mensaje temporal en el widget
                        showTemporaryMessage(context, appWidgetId, "El menú está cerrado. No puedes agregar votos fuera del horario de selección (08:00 - 11:00).")
                        return@launch
                    }
                }
                
                // IMPORTANTE: Actualizar el widget SIEMPRE, incluso si hay un error
                // Esto asegura que el widget refleje el estado actual después de la acción
                android.util.Log.d(TAG, "handleSelectOption: 🔄 Forzando actualización del widget...")
                
                // CRÍTICO: Esperar más tiempo para asegurar que los datos del repositorio se hayan actualizado
                // El repositorio siempre hace petición HTTP fresca, pero necesitamos tiempo para que el servidor procese
                kotlinx.coroutines.delay(800)
                
                // CRÍTICO: Actualizar todos los widgets - esto forzará una recarga completa
                // updateAllWidgets ya llama a notifyAppWidgetViewDataChanged y updateWidget
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    updateAllWidgets(context)
                }
                android.util.Log.d(TAG, "handleSelectOption: ✅ Widget actualizado")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error al seleccionar opción", e)
                val message = e.message ?: ""
                if (message.contains("cerrado", ignoreCase = true) || 
                    message.contains("horario", ignoreCase = true) || 
                    message.contains("time", ignoreCase = true) || 
                    message.contains("expired", ignoreCase = true) ||
                    message.contains("closed", ignoreCase = true)) {
                    showTemporaryMessage(context, appWidgetId, "El menú está cerrado. No puedes agregar votos fuera del horario de selección (08:00 - 11:00).")
                } else {
                    // Aún así intentar actualizar el widget
                    updateAllWidgets(context)
                }
            }
        }
    }

    /**
     * Maneja la eliminación de un voto.
     */
    private fun handleDeleteVote(context: Context, intent: Intent) {
        val voteId = intent.getStringExtra(EXTRA_VOTE_ID) ?: return
        val appWidgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        
        widgetScope.launch {
            try {
                val app = context.applicationContext as? MorfipoloApplication ?: return@launch
                
                android.util.Log.d(TAG, "handleDeleteVote: 🗑️ Eliminando voto $voteId...")
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    app.voteRepository.deleteVote(voteId)
                }
                
                android.util.Log.d(TAG, "handleDeleteVote: Resultado: ${if (result.isSuccess) "Éxito" else "Error: ${result.exceptionOrNull()?.message}"}")
                
                // Verificar si es un error de horario cerrado
                if (!result.isSuccess) {
                    val exception = result.exceptionOrNull()
                    val message = exception?.message ?: ""
                    
                    if (message.contains("cerrado", ignoreCase = true) || 
                        message.contains("horario", ignoreCase = true) || 
                        message.contains("time", ignoreCase = true) || 
                        message.contains("expired", ignoreCase = true) ||
                        message.contains("closed", ignoreCase = true)) {
                        // Mostrar mensaje temporal en el widget
                        showTemporaryMessage(context, appWidgetId, "El menú está cerrado. No puedes quitar votos fuera del horario de selección (08:00 - 11:00).")
                        return@launch
                    }
                }
                
                // IMPORTANTE: Actualizar el widget SIEMPRE, incluso si el servidor devuelve 404
                // El voto puede ya estar eliminado en el servidor pero aún mostrarse en el widget
                // Forzar actualización para recargar los datos y reflejar el estado actual
                android.util.Log.d(TAG, "handleDeleteVote: 🔄 Forzando actualización del widget...")
                
                // CRÍTICO: Esperar más tiempo para asegurar que los datos del repositorio se hayan actualizado
                // El repositorio siempre hace petición HTTP fresca, pero necesitamos tiempo para que el servidor procese
                kotlinx.coroutines.delay(800)
                
                // CRÍTICO: Actualizar todos los widgets - esto forzará una recarga completa
                // updateAllWidgets ya llama a notifyAppWidgetViewDataChanged y updateWidget
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    updateAllWidgets(context)
                }
                android.util.Log.d(TAG, "handleDeleteVote: ✅ Widget actualizado")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error al eliminar voto", e)
                val message = e.message ?: ""
                if (message.contains("cerrado", ignoreCase = true) || 
                    message.contains("horario", ignoreCase = true) || 
                    message.contains("time", ignoreCase = true) || 
                    message.contains("expired", ignoreCase = true) ||
                    message.contains("closed", ignoreCase = true)) {
                    showTemporaryMessage(context, appWidgetId, "El menú está cerrado. No puedes quitar votos fuera del horario de selección (08:00 - 11:00).")
                } else {
                    // Aún así intentar actualizar el widget
                    updateAllWidgets(context)
                }
            }
        }
    }

    /**
     * Actualiza todos los widgets instalados.
     */
    private fun updateAllWidgets(context: Context) {
        try {
            android.util.Log.d(TAG, "updateAllWidgets: 🔄 Iniciando actualización de todos los widgets...")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, MenuWidgetProvider::class.java)
            )
            android.util.Log.d(TAG, "updateAllWidgets: Encontrados ${appWidgetIds.size} widgets")
            
            // CRÍTICO: Actualizar todos los widgets - el cambio de data URI forzará recreación del Factory
            // No necesitamos notifyAppWidgetViewDataChanged() porque updateWidget ya recrea el servicio
            appWidgetIds.forEach { appWidgetId ->
                android.util.Log.d(TAG, "updateAllWidgets: 🔄 Actualizando widget $appWidgetId")
                updateWidget(context, appWidgetManager, appWidgetId)
            }
            
            android.util.Log.d(TAG, "updateAllWidgets: ✅ Todos los widgets actualizados")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al actualizar widgets", e)
            e.printStackTrace()
        }
    }

    /**
     * Muestra un mensaje temporal en el widget durante unos segundos.
     */
    private fun showTemporaryMessage(context: Context, appWidgetId: Int, message: String) {
        if (appWidgetId == -1) {
            // Si no tenemos un widgetId específico, mostrar en todos los widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, MenuWidgetProvider::class.java)
            )
            appWidgetIds.forEach { id ->
                showTemporaryMessageForWidget(context, appWidgetManager, id, message)
            }
        } else {
            showTemporaryMessageForWidget(context, AppWidgetManager.getInstance(context), appWidgetId, message)
        }
    }
    
    private fun showTemporaryMessageForWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        message: String
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_menu_simple)
            
            // Mostrar el mensaje temporalmente en el widgetNoMenuTextView
            views.setViewVisibility(R.id.widgetNoMenuTextView, View.VISIBLE)
            views.setTextViewText(R.id.widgetNoMenuTextView, message)
            views.setTextColor(R.id.widgetNoMenuTextView, context.resources.getColor(R.color.nonna_error, null))
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Ocultar el mensaje después de 5 segundos
            widgetScope.launch {
                kotlinx.coroutines.delay(5000) // 5 segundos
                
                // Restaurar el widget a su estado normal
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al mostrar mensaje temporal en widget", e)
        }
    }

    /**
     * Formatea una fecha de formato "yyyy-MM-dd" a "dd/MM/yyyy".
     */
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString) ?: Date()
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Verifica si la hora actual está dentro del tiempo de selección del menú.
     * Horario fijo: 08:00 - 11:00
     */
    private fun isWithinSelectionTime(menu: com.cocido.morfipolo.domain.model.Menu): Boolean {
        if (menu.status != "open") return false
        
        return try {
            // Horario fijo: 08:00 - 11:00
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)

            val startHour = 8
            val startMin = 0
            val endHour = 11
            val endMin = 0

            val currentTimeInMinutes = currentHour * 60 + currentMinute
            val startTimeInMinutes = startHour * 60 + startMin
            val endTimeInMinutes = endHour * 60 + endMin

            currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
        } catch (e: Exception) {
            false
        }
    }
}

