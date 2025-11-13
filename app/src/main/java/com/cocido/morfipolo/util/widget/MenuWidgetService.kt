package com.cocido.morfipolo.util.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.domain.model.MenuOption
import com.cocido.morfipolo.domain.model.Vote
import kotlinx.coroutines.runBlocking
import java.util.Calendar

/**
 * Servicio para proveer datos dinámicos al widget usando una lista.
 */
class MenuWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MenuWidgetFactory(applicationContext, intent)
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

    override fun onCreate() {
        // Inicialización si es necesaria
    }

    override fun onDataSetChanged() {
        // Cargar datos cuando se actualiza el widget
        try {
            val app = context.applicationContext as? MorfipoloApplication
            if (app == null || !app.sessionManager.isLoggedIn()) {
                options = emptyList()
                return
            }

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
                options = emptyList()
                return
            }

            menuId = menu.id
            options = menu.getOptionsOrEmpty()

            // Verificar si puede votar - el menú está realmente abierto solo si el status es "open" Y está dentro del horario
            val isWithinTime = isWithinSelectionTime(menu)
            val isActuallyOpen = menu.status == "open" && isWithinTime
            canVote = isActuallyOpen

            // Obtener voto del usuario
            val userId = app.sessionManager.getCurrentUserId()
            if (userId != null) {
                userVote = runBlocking {
                    try {
                        app.voteRepository.getUserVoteForMenu(menu.id, userId)
                    } catch (e: Exception) {
                        android.util.Log.e("MenuWidgetFactory", "Error al obtener voto", e)
                        null
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MenuWidgetFactory", "Error en onDataSetChanged", e)
            options = emptyList()
        }
    }

    override fun onDestroy() {
        options = emptyList()
    }

    override fun getCount(): Int = options.size

    override fun getViewAt(position: Int): RemoteViews {
        val option = options.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_menu_item)
        
        val views = RemoteViews(context.packageName, R.layout.widget_menu_item)
        
        // Configurar nombre de la opción
        views.setTextViewText(R.id.widgetItemName, option.name)
        
        // Verificar si está seleccionada
        val isSelected = userVote?.option?.id == option.id
        
        // Configurar botón
        if (isSelected && userVote != null) {
            // Botón para quitar selección
            views.setTextViewText(R.id.widgetItemButton, "Quitar selección")
            try {
                views.setInt(R.id.widgetItemButton, "setBackgroundResource", R.drawable.button_red)
            } catch (e: Exception) {
                views.setInt(R.id.widgetItemButton, "setBackgroundColor", 0xFFC85A5A.toInt())
            }
            
            // Configurar PendingIntent para quitar selección
            val deleteIntent = Intent(context, MenuWidgetProvider::class.java).apply {
                action = MenuWidgetProvider.ACTION_DELETE_VOTE
                putExtra(MenuWidgetProvider.EXTRA_VOTE_ID, userVote!!.id)
                putExtra(MenuWidgetProvider.EXTRA_OPTION_INDEX, position)
            }
            val deletePendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                (userVote!!.id + position).hashCode(),
                deleteIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetItemButton, deletePendingIntent)
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
                // Configurar PendingIntent para seleccionar
                val selectIntent = Intent(context, MenuWidgetProvider::class.java).apply {
                    action = MenuWidgetProvider.ACTION_SELECT_OPTION
                    putExtra(MenuWidgetProvider.EXTRA_MENU_ID, menuId)
                    putExtra(MenuWidgetProvider.EXTRA_OPTION_ID, option.id)
                    putExtra(MenuWidgetProvider.EXTRA_OPTION_INDEX, position)
                }
                val selectPendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    (menuId + option.id + position).hashCode(),
                    selectIntent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widgetItemButton, selectPendingIntent)
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
}
