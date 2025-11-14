package com.cocido.morfipolo.util.widget

import android.app.PendingIntent
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.domain.model.MenuOption
import com.cocido.morfipolo.domain.model.Vote
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Servicio para proveer datos dinámicos al widget usando una lista.
 */
class MenuWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        android.util.Log.d("MenuWidgetService", "═══════════════════════════════════════════════════════════")
        android.util.Log.d("MenuWidgetService", "onGetViewFactory: 🏭 SERVICIO LLAMADO")
        android.util.Log.d("MenuWidgetService", "onGetViewFactory:   -> Intent action: ${intent.action}")
        android.util.Log.d("MenuWidgetService", "onGetViewFactory:   -> Intent data URI: ${intent.data}")
        android.util.Log.d("MenuWidgetService", "onGetViewFactory:   -> Intent extras: ${intent.extras?.keySet()?.joinToString(", ")}")
        val appWidgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        android.util.Log.d("MenuWidgetService", "onGetViewFactory:   -> appWidgetId: $appWidgetId")
        android.util.Log.d("MenuWidgetService", "onGetViewFactory:   -> Creando MenuWidgetFactory...")
        val factory = MenuWidgetFactory(applicationContext, intent)
        android.util.Log.d("MenuWidgetService", "onGetViewFactory: ✅ MenuWidgetFactory creado")
        android.util.Log.d("MenuWidgetService", "═══════════════════════════════════════════════════════════")
        return factory
    }
}

/**
 * Factory que crea las vistas remotas para cada item de la lista del widget.
 */
class MenuWidgetFactory(
    private val context: android.content.Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var options: List<MenuOption> = emptyList()
    private var menuId: String = ""
    private var userVote: Vote? = null
    private var canVote: Boolean = false
    private var appWidgetId: Int = -1

    override fun onCreate() {
        android.util.Log.d("MenuWidgetFactory", "═══════════════════════════════════════════════════════════")
        android.util.Log.d("MenuWidgetFactory", "onCreate: 🎬 RemoteViewsFactory CREADO")
        android.util.Log.d("MenuWidgetFactory", "═══════════════════════════════════════════════════════════")
    }

    override fun onDataSetChanged() {
        // Cargar datos cuando se actualiza el widget
        android.util.Log.d("MenuWidgetFactory", "onDataSetChanged: Iniciando carga de datos...")
        try {
            // Obtener appWidgetId del intent
            appWidgetId = intent.getIntExtra(
                android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
                android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
            )
            android.util.Log.d("MenuWidgetFactory", "onDataSetChanged: appWidgetId=$appWidgetId")
            
            val app = context.applicationContext as? MorfipoloApplication
            if (app == null || !app.sessionManager.isLoggedIn()) {
                android.util.Log.w("MenuWidgetFactory", "onDataSetChanged: App null o usuario no logueado")
                options = emptyList()
                userVote = null
                menuId = ""
                canVote = false
                return
            }

            android.util.Log.d("MenuWidgetFactory", "onDataSetChanged: Usuario logueado, obteniendo menú...")

            // Obtener menú del día
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val menu = runBlocking {
                try {
                    app.menuRepository.getMenuByDate(today.time)
                } catch (e: Exception) {
                    android.util.Log.e("MenuWidgetFactory", "Error al obtener menú", e)
                    null
                }
            }

            if (menu == null) {
                android.util.Log.w("MenuWidgetFactory", "onDataSetChanged: No hay menú disponible")
                options = emptyList()
                userVote = null
                menuId = ""
                canVote = false
                return
            }

            menuId = menu.id
            options = menu.getOptionsOrEmpty()
            android.util.Log.d("MenuWidgetFactory", "onDataSetChanged: Menú cargado: id=$menuId, opciones=${options.size}")

            // Verificar si puede votar - el menú está realmente abierto solo si el status es "open" Y está dentro del horario Y es el menú de hoy
            val isToday = isMenuToday(menu)
            val isWithinTime = isWithinSelectionTime(menu)
            val isActuallyOpen = menu.status == "open" && isWithinTime && isToday
            canVote = isActuallyOpen
            android.util.Log.d("MenuWidgetFactory", "onDataSetChanged: canVote=$canVote (isToday=$isToday, isWithinTime=$isWithinTime, status=${menu.status})")

            // CRÍTICO: Obtener voto del usuario - recargar siempre datos frescos
            val userId = app.sessionManager.getCurrentUserId()
            if (userId != null) {
                android.util.Log.d("MenuWidgetFactory", "onDataSetChanged: Obteniendo voto del usuario para menú $menuId...")
                userVote = runBlocking {
                    try {
                        // CRÍTICO: Siempre recargar desde el repositorio para obtener datos frescos
                        // No confiar en caché - el voto puede haber cambiado después de una acción
                        val vote = app.voteRepository.getUserVoteForMenu(menu.id, userId)
                        android.util.Log.d("MenuWidgetFactory", "onDataSetChanged: ✅ Voto del usuario: ${if (vote != null) "Sí (voteId=${vote.id}, optionId=${vote.option.id}, optionName=${vote.option.name})" else "No hay voto"}")
                        
                        // Log detallado para diagnóstico
                        if (vote != null) {
                            android.util.Log.d("MenuWidgetFactory", "onDataSetChanged:   -> vote.id: ${vote.id}")
                            android.util.Log.d("MenuWidgetFactory", "onDataSetChanged:   -> vote.option.id: ${vote.option.id}")
                            android.util.Log.d("MenuWidgetFactory", "onDataSetChanged:   -> vote.option.name: ${vote.option.name}")
                        }
                        
                        vote
                    } catch (e: Exception) {
                        android.util.Log.e("MenuWidgetFactory", "Error al obtener voto", e)
                        e.printStackTrace()
                        null
                    }
                }
            } else {
                android.util.Log.w("MenuWidgetFactory", "onDataSetChanged: userId es null")
                userVote = null
            }
            
            android.util.Log.d("MenuWidgetFactory", "onDataSetChanged: ✅ Datos cargados exitosamente")
        } catch (e: Exception) {
            android.util.Log.e("MenuWidgetFactory", "Error en onDataSetChanged", e)
            e.printStackTrace()
            options = emptyList()
            userVote = null
            menuId = ""
            canVote = false
        }
    }

    override fun onDestroy() {
        options = emptyList()
    }

    override fun getCount(): Int {
        val count = options.size
        android.util.Log.d("MenuWidgetFactory", "getCount: Devolviendo $count opciones")
        return count
    }

    override fun getViewAt(position: Int): RemoteViews {
        android.util.Log.d("MenuWidgetFactory", "═══════════════════════════════════════════════════════════")
        android.util.Log.d("MenuWidgetFactory", "getViewAt: 🎬 LLAMADO - position=$position")
        val option = options.getOrNull(position) ?: run {
            android.util.Log.w("MenuWidgetFactory", "getViewAt: ⚠️ Opción nula en posición $position")
            return RemoteViews(context.packageName, R.layout.widget_menu_item)
        }
        
        val views = RemoteViews(context.packageName, R.layout.widget_menu_item)
        
        // Configurar nombre de la opción
        views.setTextViewText(R.id.widgetItemName, option.name)
        
        // CRÍTICO: Verificar si está seleccionada - comparar option.id con userVote.option.id
        val currentVote = userVote // Guardar en variable local para smart cast
        val isSelected = currentVote?.option?.id == option.id
        
        android.util.Log.d("MenuWidgetFactory", "getViewAt: position=$position, option.id=${option.id}, option.name=${option.name}")
        android.util.Log.d("MenuWidgetFactory", "getViewAt: currentVote=${if (currentVote != null) "Sí (voteId=${currentVote.id}, option.id=${currentVote.option.id}, option.name=${currentVote.option.name})" else "No"}")
        android.util.Log.d("MenuWidgetFactory", "getViewAt: isSelected=$isSelected (currentVote?.option?.id=${currentVote?.option?.id} == option.id=${option.id})")
        android.util.Log.d("MenuWidgetFactory", "getViewAt: canVote=$canVote, menuId=$menuId")
        
        // Configurar botón
        if (isSelected && currentVote != null) {
            // Botón para quitar selección
            views.setTextViewText(R.id.widgetItemButton, "Quitar selección")
            try {
                views.setInt(R.id.widgetItemButton, "setBackgroundResource", R.drawable.button_red)
            } catch (e: Exception) {
                views.setInt(R.id.widgetItemButton, "setBackgroundColor", 0xFFC85A5A.toInt())
            }
            
            // CRÍTICO: Para listas dinámicas en widgets, DEBEMOS usar FillInIntent con Template
            // El FillInIntent NO debe tener action ni data URI - solo extras
            // Android fusionará estos extras con el template intent (que tiene el data URI)
            val voteId = currentVote.id // Usar variable local para evitar smart cast issue
            val deleteFillInIntent = Intent().apply {
                // FillInIntent NO debe tener action ni data URI - solo extras
                // Android fusionará estos extras con el template intent
                putExtra("action", MenuWidgetProvider.ACTION_DELETE_VOTE)
                putExtra(MenuWidgetProvider.EXTRA_VOTE_ID, voteId)
                putExtra(MenuWidgetProvider.EXTRA_OPTION_INDEX, position)
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            android.util.Log.d("MenuWidgetFactory", "═══════════════════════════════════════════════════════════")
            android.util.Log.d("MenuWidgetFactory", "getViewAt: 🗑️ CONFIGURANDO botón 'Quitar selección' (FillInIntent)")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> position: $position")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> voteId: '$voteId'")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> option: ${option.name}")
            android.util.Log.d("MenuWidgetFactory", "getViewAt: FillInIntent creado:")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Action: ${deleteFillInIntent.action} (debe ser null)")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Data URI: ${deleteFillInIntent.data} (debe ser null)")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra 'action': ${deleteFillInIntent.getStringExtra("action")}")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra '${MenuWidgetProvider.EXTRA_VOTE_ID}': ${deleteFillInIntent.getStringExtra(MenuWidgetProvider.EXTRA_VOTE_ID)}")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra '${MenuWidgetProvider.EXTRA_OPTION_INDEX}': ${deleteFillInIntent.getIntExtra(MenuWidgetProvider.EXTRA_OPTION_INDEX, -1)}")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra 'appWidgetId': ${deleteFillInIntent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)}")
            android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Todos los extras: ${deleteFillInIntent.extras?.keySet()?.joinToString(", ")}")
            views.setOnClickFillInIntent(R.id.widgetItemButton, deleteFillInIntent)
            android.util.Log.d("MenuWidgetFactory", "getViewAt: ✅ FillInIntent configurado en botón")
            android.util.Log.d("MenuWidgetFactory", "═══════════════════════════════════════════════════════════")
        } else {
            // Botón para seleccionar
            views.setTextViewText(R.id.widgetItemButton, if (canVote) "Elegir esta opción" else "No disponible")
            try {
                if (canVote) {
                    views.setInt(R.id.widgetItemButton, "setBackgroundResource", R.drawable.button_primary_solid)
                } else {
                    views.setInt(R.id.widgetItemButton, "setBackgroundColor", 0xFFA1887F.toInt())
                }
            } catch (e: Exception) {
                val buttonColor = if (canVote) 0xFF8B6F47.toInt() else 0xFFA1887F.toInt()
                views.setInt(R.id.widgetItemButton, "setBackgroundColor", buttonColor)
            }
            
            if (canVote) {
                // CRÍTICO: Para listas dinámicas en widgets, DEBEMOS usar FillInIntent con Template
                // El FillInIntent NO debe tener action ni data URI - solo extras
                // Android fusionará estos extras con el template intent (que tiene el data URI)
                val selectFillInIntent = Intent().apply {
                    // FillInIntent NO debe tener action ni data URI - solo extras
                    // Android fusionará estos extras con el template intent
                    putExtra("action", MenuWidgetProvider.ACTION_SELECT_OPTION)
                    putExtra(MenuWidgetProvider.EXTRA_MENU_ID, menuId)
                    putExtra(MenuWidgetProvider.EXTRA_OPTION_ID, option.id)
                    putExtra(MenuWidgetProvider.EXTRA_OPTION_INDEX, position)
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                android.util.Log.d("MenuWidgetFactory", "═══════════════════════════════════════════════════════════")
                android.util.Log.d("MenuWidgetFactory", "getViewAt: 🎯 CONFIGURANDO botón 'Elegir esta opción' (FillInIntent)")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> position: $position")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> menuId: '$menuId'")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> optionId: '${option.id}'")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> option: ${option.name}")
                android.util.Log.d("MenuWidgetFactory", "getViewAt: FillInIntent creado:")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Action: ${selectFillInIntent.action} (debe ser null)")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Data URI: ${selectFillInIntent.data} (debe ser null)")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra 'action': ${selectFillInIntent.getStringExtra("action")}")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra '${MenuWidgetProvider.EXTRA_MENU_ID}': ${selectFillInIntent.getStringExtra(MenuWidgetProvider.EXTRA_MENU_ID)}")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra '${MenuWidgetProvider.EXTRA_OPTION_ID}': ${selectFillInIntent.getStringExtra(MenuWidgetProvider.EXTRA_OPTION_ID)}")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra '${MenuWidgetProvider.EXTRA_OPTION_INDEX}': ${selectFillInIntent.getIntExtra(MenuWidgetProvider.EXTRA_OPTION_INDEX, -1)}")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Extra 'appWidgetId': ${selectFillInIntent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)}")
                android.util.Log.d("MenuWidgetFactory", "getViewAt:   -> Todos los extras: ${selectFillInIntent.extras?.keySet()?.joinToString(", ")}")
                views.setOnClickFillInIntent(R.id.widgetItemButton, selectFillInIntent)
                android.util.Log.d("MenuWidgetFactory", "getViewAt: ✅ FillInIntent configurado en botón")
                android.util.Log.d("MenuWidgetFactory", "═══════════════════════════════════════════════════════════")
            } else {
                // Si no se puede votar, no configurar FillInIntent (botón inactivo)
                android.util.Log.d("MenuWidgetFactory", "getViewAt: ⚠️ No se puede votar, botón sin FillInIntent")
                views.setOnClickFillInIntent(R.id.widgetItemButton, null)
            }
        }
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun isWithinSelectionTime(menu: com.cocido.morfipolo.domain.model.Menu): Boolean {
        if (menu.status != "open") return false
        
        return try {
            // Horario fijo: 08:00 - 11:00
            val now = java.util.Calendar.getInstance()
            val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(java.util.Calendar.MINUTE)

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
    
    private fun isMenuToday(menu: com.cocido.morfipolo.domain.model.Menu): Boolean {
        return try {
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val menuDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date)
            
            menuDate?.let {
                val menuCalendar = java.util.Calendar.getInstance().apply {
                    time = it
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                menuCalendar.timeInMillis == today.timeInMillis
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
