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
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_SELECT_OPTION -> {
                handleSelectOption(context, intent)
            }
            ACTION_DELETE_VOTE -> {
                handleDeleteVote(context, intent)
            }
        }
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

                if (authResult !is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated) {
                    android.util.Log.w(TAG, "updateWidget: Autenticación fallida")
                    showNotLoggedInState(context, appWidgetManager, appWidgetId)
                    return@launch
                }
                android.util.Log.d(TAG, "updateWidget: Autenticación exitosa")

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
                val serviceIntent = Intent(context, MenuWidgetService::class.java)
                serviceIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                serviceIntent.data = android.net.Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
                
                views.setRemoteAdapter(R.id.widgetOptionsList, serviceIntent)
                android.util.Log.d(TAG, "showMenuState: ListView configurado con RemoteViewsService")
                
                // Configurar template para clicks vacíos (el click se maneja en cada item)
                val emptyViewIntent = Intent(context, MainActivity::class.java)
                val emptyViewPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    emptyViewIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setPendingIntentTemplate(R.id.widgetOptionsList, emptyViewPendingIntent)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "showMenuState: Error al configurar ListView", e)
                e.printStackTrace()
            }
            
            // Notificar cambios en la lista
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetOptionsList)
            
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
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)
            android.util.Log.d(TAG, "configureClickIntent: Click intent configurado correctamente")
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
        
        if (menuId == null || optionId == null) return
        
        widgetScope.launch {
            try {
                val app = context.applicationContext as? MorfipoloApplication ?: return@launch
                val userId = app.sessionManager.getCurrentUserId() ?: return@launch
                
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    app.voteRepository.createVoteOrReplace(optionId, menuId, userId)
                }
                
                if (result.isSuccess) {
                    // Actualizar todos los widgets
                    updateAllWidgets(context)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error al seleccionar opción", e)
            }
        }
    }

    /**
     * Maneja la eliminación de un voto.
     */
    private fun handleDeleteVote(context: Context, intent: Intent) {
        val voteId = intent.getStringExtra(EXTRA_VOTE_ID) ?: return
        
        widgetScope.launch {
            try {
                val app = context.applicationContext as? MorfipoloApplication ?: return@launch
                
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    app.voteRepository.deleteVote(voteId)
                }
                
                if (result.isSuccess) {
                    // Actualizar todos los widgets
                    updateAllWidgets(context)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error al eliminar voto", e)
            }
        }
    }

    /**
     * Actualiza todos los widgets instalados.
     */
    private fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, MenuWidgetProvider::class.java)
            )
            // Notificar cambios en las listas de todos los widgets
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetOptionsList)
            }
            // Actualizar todos los widgets
            appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al actualizar widgets", e)
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

