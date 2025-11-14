# Add project specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# CRÍTICO: Mantener atributos de tipos genéricos (Signature) - necesario para Retrofit Response<T>
-keepattributes Signature, Exceptions, InnerClasses
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes *Annotation*

# Preserve line numbers for stack traces (útil para debugging de crashes en producción)
-keepattributes SourceFile,LineNumberTable

# Hide original source file name in stack traces
-renamesourcefileattribute SourceFile

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Room entities
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.Entity { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Retrofit interfaces and preserve generic type information
-keep interface com.cocido.morfipolo.data.remote.api.** { *; }
-keepclassmembers class com.cocido.morfipolo.data.remote.api.** {
    <methods>;
}

# Retrofit - Keep Response and Call classes with generic type information
-keep class retrofit2.Response { *; }
-keep class retrofit2.Call { *; }
-keep class retrofit2.Callback { *; }

# Retrofit - Keep generic signatures for methods returning Response<T>
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep all data classes used with Moshi (with all their fields and constructors)
-keep class com.cocido.morfipolo.domain.model.** { *; }
-keepclassmembers class com.cocido.morfipolo.domain.model.** {
    <init>(...);
    <fields>;
    <methods>;
}

# Keep Moshi and its generated adapters
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class com.squareup.moshi.** {
    <init>(...);
}

# Keep Moshi generated adapters (Kapt generates these)
-keep class com.cocido.morfipolo.domain.model.**$$JsonAdapter { *; }
-keepclassmembers class com.cocido.morfipolo.domain.model.**$$JsonAdapter {
    <init>(...);
}

# Keep generic signatures for Moshi adapters
-keep class * extends com.squareup.moshi.JsonAdapter {
    <init>(...);
}

# Kotlin - Keep metadata for reflection (ya se configuró arriba con Signature)
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep widget providers and services - CRÍTICO para widgets
# IMPORTANTE: Preservar TODAS las clases del widget sin ofuscar
-keep class com.cocido.morfipolo.util.widget.** { *; }
-keepclassmembers class com.cocido.morfipolo.util.widget.** {
    *;
    <init>(...);
    <fields>;
    <methods>;
    private *;
    protected *;
    public *;
}

# Keep AppWidgetProvider
-keep class * extends android.appwidget.AppWidgetProvider {
    *;
}

# Keep RemoteViewsService - CRÍTICO para widgets
-keep class * extends android.widget.RemoteViewsService {
    *;
    <init>(...);
    public android.widget.RemoteViewsFactory onGetViewFactory(android.content.Intent);
}
-keep class com.cocido.morfipolo.util.widget.MenuWidgetService {
    *;
    public android.widget.RemoteViewsFactory onGetViewFactory(android.content.Intent);
    <init>(...);
}

# Keep RemoteViewsFactory (inner class)
-keep class * implements android.widget.RemoteViewsService$RemoteViewsFactory {
    *;
}
-keep class com.cocido.morfipolo.util.widget.MenuWidgetFactory implements android.widget.RemoteViewsService$RemoteViewsFactory {
    *;
    <init>(...);
    public void onCreate();
    public void onDestroy();
    public void onDataSetChanged();
    public android.widget.RemoteViews getViewAt(int);
    public android.widget.RemoteViews getLoadingView();
    public int getCount();
    public int getViewTypeCount();
    public long getItemId(int);
    public boolean hasStableIds();
}

# Keep widget factory implementations - CRÍTICO
# IMPORTANTE: NO ofuscar nombres de clases del widget
-keepnames class com.cocido.morfipolo.util.widget.MenuWidgetFactory
-keepnames class com.cocido.morfipolo.util.widget.MenuWidgetService
-keepnames class com.cocido.morfipolo.util.widget.MenuWidgetProvider
-keep class com.cocido.morfipolo.util.widget.MenuWidgetFactory { *; }
-keep class com.cocido.morfipolo.util.widget.MenuWidgetService { *; }
-keep class com.cocido.morfipolo.util.widget.MenuWidgetProvider { *; }

# Keep widget factory methods
-keepclassmembers class com.cocido.morfipolo.util.widget.MenuWidgetFactory {
    public <methods>;
    public <fields>;
    <init>(...);
}

# Keep all constructors for widget classes
-keepclassmembers class com.cocido.morfipolo.util.widget.** {
    <init>(...);
}

# Keep WorkManager workers
-keep class com.cocido.morfipolo.util.work.** { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# Keep Application class - CRÍTICO para widgets
# IMPORTANTE: Preservar completamente la clase Application y sus lazy properties
-keep class com.cocido.morfipolo.MorfipoloApplication { *; }
-keepclassmembers class com.cocido.morfipolo.MorfipoloApplication {
    *;
    <init>(...);
    <fields>;
    <methods>;
    public void onCreate();
    private void setupWorkManager();
    private void scheduleMenuPollingWork(...);
}

# Keep repositories - necesarios para widgets
-keep class com.cocido.morfipolo.data.repository.** { *; }
-keepclassmembers class com.cocido.morfipolo.data.repository.** {
    *;
}

# Keep repositories methods used by widgets
# IMPORTANTE: ProGuard usa sintaxis Java, no Kotlin - no usar "suspend fun"
-keepclassmembers class com.cocido.morfipolo.data.repository.MenuRepository {
    public *** getMenuByDate(...);
    public *** getWeeklyMenus();
}

-keepclassmembers class com.cocido.morfipolo.data.repository.VoteRepository {
    public *** createVoteOrReplace(...);
    public *** deleteVote(...);
    public *** getUserVoteForMenu(...);
}

# Keep auth manager - necesario para widgets
-keep class com.cocido.morfipolo.data.remote.AuthManager { *; }
-keepclassmembers class com.cocido.morfipolo.data.remote.AuthManager {
    *;
}

# Keep session manager - necesario para widgets
-keep class com.cocido.morfipolo.data.local.preferences.SessionManager { *; }
-keepclassmembers class com.cocido.morfipolo.data.local.preferences.SessionManager {
    *;
}

# Remove all debug logging (optimización)
# IMPORTANTE: Mantener Log.w y Log.e para errores en producción
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep R class and resource references for widgets
-keep class com.cocido.morfipolo.R$* {
    *;
}

# Keep enum classes used in widgets
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# CRÍTICO: Keep coroutines and scopes used in widgets
# IMPORTANTE: Preservar TODAS las clases de coroutines sin ofuscar
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** {
    *;
    <init>(...);
    <methods>;
    <fields>;
}

# Keep coroutine scopes used in widget - CRÍTICO
-keepclassmembers class com.cocido.morfipolo.util.widget.MenuWidgetProvider {
    private kotlinx.coroutines.CoroutineScope widgetScope;
}

# Keep all coroutines launch methods used in widgets
-keepclassmembers class kotlinx.coroutines.CoroutineScope {
    public kotlinx.coroutines.Job launch(...);
}

# Keep Dispatchers used in widgets
-keepclassmembers class kotlinx.coroutines.Dispatchers {
    public static final kotlinx.coroutines.CoroutineDispatcher Main;
    public static final kotlinx.coroutines.CoroutineDispatcher IO;
}

# Keep withContext used in widgets
# IMPORTANTE: ProGuard usa sintaxis Java, no Kotlin
-keepclassmembers class kotlinx.coroutines.** {
    public static *** withContext(...);
}

# CRÍTICO: Keep SupervisorJob used in widget scope
-keep class kotlinx.coroutines.SupervisorJob { *; }
-keepclassmembers class kotlinx.coroutines.SupervisorJob {
    public static kotlinx.coroutines.CompletableJob SupervisorJob();
}

# CRÍTICO: Keep CoroutineScope creation (SupervisorJob() + Dispatchers.Main)
-keep class kotlinx.coroutines.CoroutineScope { *; }
-keep interface kotlinx.coroutines.CoroutineScope { *; }
-keepclassmembers class kotlinx.coroutines.CoroutineScope {
    *;
    public kotlinx.coroutines.CoroutineContext coroutineContext;
}
-keepclassmembers interface kotlinx.coroutines.CoroutineScope {
    public abstract kotlinx.coroutines.CoroutineContext getCoroutineContext();
}

# Keep coroutine context operations
-keep class kotlinx.coroutines.CoroutineContext { *; }

# Keep suspend functions used by widgets
# IMPORTANTE: ProGuard procesa el bytecode Java - los métodos suspend se convierten en métodos normales con Continuation
-keepclassmembers class com.cocido.morfipolo.util.widget.** {
    public *** *(...);
    private *** *(...);
}

# Keep all methods in widget classes - CRÍTICO
# Preservar TODOS los métodos del widget (incluyendo privados y suspend)
-keepclassmembers class com.cocido.morfipolo.util.widget.MenuWidgetProvider {
    *;
    public void onUpdate(...);
    public void onReceive(...);
    public void onEnabled(...);
    public void onDisabled(...);
    private void updateWidget(...);
    private void updateAllWidgets(...);
    private void showMenuState(...);
    private void showLoadingState(...);
    private void showNotLoggedInState(...);
    private void showNoMenuState(...);
    private void showNoOptionsState(...);
    private void showErrorState(...);
    private void showTemporaryMessage(...);
    private void showTemporaryMessageForWidget(...);
    private void handleSelectOption(...);
    private void handleDeleteVote(...);
    private *** formatDate(...);
    private *** isWithinSelectionTime(...);
    private *** configureClickIntent(...);
    private *** configureOption(...);
    private *** createPendingIntent(...);
    private *** *(...);
}

# Keep all methods in widget factory - CRÍTICO
# Preservar TODOS los métodos del factory (incluyendo privados y suspend)
-keepclassmembers class com.cocido.morfipolo.util.widget.MenuWidgetFactory {
    *;
    public void onCreate();
    public void onDestroy();
    public void onDataSetChanged();
    public android.widget.RemoteViews getViewAt(...);
    public android.widget.RemoteViews getLoadingView();
    public int getCount();
    public int getViewTypeCount();
    public long getItemId(...);
    public boolean hasStableIds();
    private *** isWithinSelectionTime(...);
    private *** isMenuToday(...);
    private *** *(...);
    <init>(...);
    <fields>;
}

# Keep runBlocking used in widget factory
# IMPORTANTE: ProGuard no acepta sintaxis de genéricos Java como <T>
-keepclassmembers class kotlinx.coroutines.** {
    public static *** runBlocking(...);
}

# Keep reflection calls used in widgets - CRÍTICO
# Preserve Application class and its getters used by widgets
-keep class com.cocido.morfipolo.MorfipoloApplication {
    public com.cocido.morfipolo.data.repository.MenuRepository menuRepository;
    public com.cocido.morfipolo.data.repository.VoteRepository voteRepository;
    public com.cocido.morfipolo.data.remote.AuthManager authManager;
    public com.cocido.morfipolo.data.local.preferences.SessionManager sessionManager;
    public com.cocido.morfipolo.data.repository.UserRepository userRepository;
}

# Keep Application getters (lazy properties) - CRÍTICO para widgets
-keepclassmembers class com.cocido.morfipolo.MorfipoloApplication {
    public com.cocido.morfipolo.data.repository.MenuRepository getMenuRepository();
    public com.cocido.morfipolo.data.repository.VoteRepository getVoteRepository();
    public com.cocido.morfipolo.data.remote.AuthManager getAuthManager();
    public com.cocido.morfipolo.data.local.preferences.SessionManager getSessionManager();
    public com.cocido.morfipolo.data.repository.UserRepository getUserRepository();
    public com.cocido.morfipolo.data.local.database.AppDatabase getDatabase();
}
# CRÍTICO: Preservar propiedades lazy de Application usadas por widgets
-keepclassmembers class com.cocido.morfipolo.MorfipoloApplication {
    public final com.cocido.morfipolo.data.repository.MenuRepository menuRepository;
    public final com.cocido.morfipolo.data.repository.VoteRepository voteRepository;
    public final com.cocido.morfipolo.data.remote.AuthManager authManager;
    public final com.cocido.morfipolo.data.local.preferences.SessionManager sessionManager;
    public final com.cocido.morfipolo.data.repository.UserRepository userRepository;
}

# Keep RemoteViews construction
-keepclassmembers class android.widget.RemoteViews {
    public <init>(...);
    public void setTextViewText(...);
    public void setViewVisibility(...);
    public void setOnClickFillInIntent(...);
    public void setPendingIntentTemplate(...);
    public void setRemoteAdapter(...);
    public void setInt(...);
}

# Keep PendingIntent creation
-keepclassmembers class android.app.PendingIntent {
    public static android.app.PendingIntent getBroadcast(...);
    public static android.app.PendingIntent getActivity(...);
}

# Keep Intent classes used in widgets
-keepclassmembers class android.content.Intent {
    public <init>(...);
    public android.content.Intent putExtra(...);
    public java.lang.String getStringExtra(...);
    public int getIntExtra(...);
}

# Keep layout resource IDs used in widgets
-keepclassmembers class com.cocido.morfipolo.R$layout {
    public static final int widget_menu_simple;
    public static final int widget_menu_item;
}

-keepclassmembers class com.cocido.morfipolo.R$id {
    public static final int widgetDateTextView;
    public static final int widgetMenuDescriptionTextView;
    public static final int widgetStatusTextView;
    public static final int widgetOptionsList;
    public static final int widgetNoMenuTextView;
    public static final int widgetItemName;
    public static final int widgetItemButton;
}

# Keep drawable resources used in widgets
-keepclassmembers class com.cocido.morfipolo.R$drawable {
    public static final int button_red;
    public static final int button_primary_solid;
    public static final int badge_success_modern;
    public static final int badge_error_modern;
}

# CRÍTICO: Keep Calendar and Date classes used in widgets
-keep class java.util.Calendar { *; }
-keep class java.util.Date { *; }
-keepclassmembers class java.util.Calendar {
    public static java.util.Calendar getInstance();
    public void set(int, int);
    public long getTimeInMillis();
}

# Keep SimpleDateFormat used in widgets
-keep class java.text.SimpleDateFormat { *; }
-keepclassmembers class java.text.SimpleDateFormat {
    public <init>(...);
    public java.util.Date parse(...);
    public java.lang.String format(...);
}

# Keep Locale used in widgets
-keep class java.util.Locale { *; }
-keepclassmembers class java.util.Locale {
    public static java.util.Locale getDefault();
}

# Keep TimeZone used in widgets
-keep class java.util.TimeZone { *; }
-keepclassmembers class java.util.TimeZone {
    public static java.util.TimeZone getTimeZone(...);
}

# CRÍTICO: Keep Context casting used in widgets (as? MorfipoloApplication)
-keep class android.content.Context { *; }
-keep class android.app.Application { *; }
-dontwarn android.content.Context
-dontwarn android.app.Application

# Keep AppWidgetManager used in widgets
-keep class android.appwidget.AppWidgetManager { *; }
-keepclassmembers class android.appwidget.AppWidgetManager {
    public static android.appwidget.AppWidgetManager getInstance(...);
    public void updateAppWidget(...);
    public void notifyAppWidgetViewDataChanged(...);
    public int[] getAppWidgetIds(...);
}

# CRÍTICO: Keep Companion object constants in MenuWidgetProvider
-keepclassmembers class com.cocido.morfipolo.util.widget.MenuWidgetProvider$Companion {
    *;
    public static final java.lang.String ACTION_SELECT_OPTION;
    public static final java.lang.String ACTION_DELETE_VOTE;
    public static final java.lang.String EXTRA_MENU_ID;
    public static final java.lang.String EXTRA_OPTION_ID;
    public static final java.lang.String EXTRA_VOTE_ID;
    public static final java.lang.String EXTRA_OPTION_INDEX;
    public static final java.lang.String TAG;
}

# CRÍTICO: Preserve all companion objects with constants
-keepclassmembers class **$Companion {
    public static final java.lang.String *;
}

# Keep Uri parsing used in widgets
-keep class android.net.Uri { *; }
-keepclassmembers class android.net.Uri {
    public static android.net.Uri parse(java.lang.String);
}

# CRÍTICO: Preservar uso de runBlocking en MenuWidgetFactory
-keepclassmembers class com.cocido.morfipolo.util.widget.MenuWidgetFactory {
    public void onDataSetChanged();
}
-dontwarn kotlinx.coroutines.runBlocking

# CRÍTICO: Preservar todas las extensiones de Kotlin usadas en widgets
-keepclassmembers class com.cocido.morfipolo.domain.model.Menu {
    public java.util.List getOptionsOrEmpty();
    public java.util.List getOptions();
    public java.lang.String getId();
    public java.lang.String getDate();
    public java.lang.String getDescription();
    public java.lang.String getStatus();
    public java.lang.String getCreated_at();
    public java.lang.String getUpdated_at();
}
-keep class com.cocido.morfipolo.domain.model.MenuOption { *; }
-keep class com.cocido.morfipolo.domain.model.Vote { *; }
-keepclassmembers class com.cocido.morfipolo.domain.model.Vote {
    public java.lang.String getId();
    public com.cocido.morfipolo.domain.model.MenuOption getOption();
}
-keepclassmembers class com.cocido.morfipolo.domain.model.MenuOption {
    public java.lang.String getId();
    public java.lang.String getName();
}

# CRÍTICO: No eliminar clases que puedan estar siendo usadas por reflection
-keep class com.cocido.morfipolo.domain.model.** { *; }