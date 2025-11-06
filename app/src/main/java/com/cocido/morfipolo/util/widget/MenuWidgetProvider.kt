package com.cocido.morfipolo.util.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MenuWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val app = context.applicationContext as MorfipoloApplication
        val menuRepository = app.menuRepository

        val views = RemoteViews(context.packageName, R.layout.widget_menu)

        scope.launch {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val menu = menuRepository.getMenuByDate(today.time)
            // Obtener userId desde sessionManager (no es suspend)
            val userId = app.sessionManager.getCurrentUserId()

            if (menu != null && userId != null) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                views.setTextViewText(R.id.widgetDateTextView, dateFormat.format(menu.fecha))
                views.setTextViewText(R.id.widgetMenuTextView, menu.descripcion)

                val hasSelected = menuRepository.hasUserSelectedMenu(userId, menu.id)

                if (hasSelected) {
                    views.setTextViewText(R.id.widgetActionButton, context.getString(R.string.widget_remove))
                    views.setOnClickPendingIntent(
                        R.id.widgetActionButton,
                        getPendingIntent(context, ACTION_REMOVE_MENU, menu.id)
                    )
                } else {
                    views.setTextViewText(R.id.widgetActionButton, context.getString(R.string.widget_join))
                    views.setOnClickPendingIntent(
                        R.id.widgetActionButton,
                        getPendingIntent(context, ACTION_SELECT_MENU, menu.id)
                    )
                }
            } else {
                views.setTextViewText(R.id.widgetDateTextView, context.getString(R.string.widget_title))
                views.setTextViewText(R.id.widgetMenuTextView, context.getString(R.string.no_menu_available))
                views.setTextViewText(R.id.widgetActionButton, "")
            }

            // Intent para abrir la app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun getPendingIntent(context: Context, action: String, menuId: Long): PendingIntent {
        val intent = Intent(context, MenuWidgetProvider::class.java).apply {
            this.action = action
            putExtra(EXTRA_MENU_ID, menuId)
        }
        return PendingIntent.getBroadcast(
            context,
            menuId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_SELECT_MENU || intent.action == ACTION_REMOVE_MENU) {
            val app = context.applicationContext as MorfipoloApplication
            val menuRepository = app.menuRepository
            val menuId = intent.getLongExtra(EXTRA_MENU_ID, -1)
            
            // Obtener userId desde sessionManager (no es suspend)
            val userId = app.sessionManager.getCurrentUserId()

            if (userId != null && menuId != -1L) {
                scope.launch {
                    if (intent.action == ACTION_SELECT_MENU) {
                        menuRepository.selectMenu(userId, menuId)
                    } else {
                        menuRepository.deselectMenu(userId, menuId)
                    }

                    // Actualizar todos los widgets
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(context, MenuWidgetProvider::class.java)
                    )
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
        }
    }

    companion object {
        private const val ACTION_SELECT_MENU = "com.cocido.morfipolo.SELECT_MENU"
        private const val ACTION_REMOVE_MENU = "com.cocido.morfipolo.REMOVE_MENU"
        private const val EXTRA_MENU_ID = "menu_id"
    }
}

