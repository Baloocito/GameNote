package cl.topingames.gamenote.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cl.topingames.gamenote.MainActivity
import cl.topingames.gamenote.data.repository.GameRepository
import cl.topingames.gamenote.data.repository.NoteRepository
import android.view.inputmethod.InputMethodManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import javax.inject.Inject
import kotlin.math.abs

/** EntryPoint para obtener repositorios en un contexto no-Activity (Service). */
@EntryPoint
@dagger.hilt.InstallIn(SingletonComponent::class)
interface OverlayEntryPoint {
    fun noteRepository(): NoteRepository
    fun gameRepository(): GameRepository
}

@AndroidEntryPoint
class OverlayService : Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    @Inject lateinit var overlayManager: OverlayManager

    // Lifecycle para ComposeView fuera de Activity
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager

    // ViewModel creado manualmente con factory — SIN Hilt en el Composable
    private lateinit var overlayViewModel: OverlayViewModel

    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var dismissView: android.view.View? = null  // capa transparente para cerrar panel

    private var bubbleX = 20
    private var bubbleY = 300

    companion object {
        const val ACTION_SHOW   = "cl.topingames.gamenote.OVERLAY_SHOW"
        const val ACTION_HIDE   = "cl.topingames.gamenote.OVERLAY_HIDE"
        const val EXTRA_GAME_ID = "cl.topingames.gamenote.GAME_ID"
        private const val CHANNEL_ID = "gamenote_overlay"
        private const val NOTIF_ID   = 1
        private const val DRAG_THRESHOLD = 8f
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Obtener repositorios via EntryPoint (applicationContext ya tiene el grafo de Hilt)
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            OverlayEntryPoint::class.java
        )
        val factory = OverlayViewModel.Factory(
            noteRepository = entryPoint.noteRepository(),
            gameRepository = entryPoint.gameRepository()
        )
        overlayViewModel = ViewModelProvider(this, factory)[OverlayViewModel::class.java]
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> { hideOverlay(); stopSelf() }
            else        -> {
                val gameId = intent?.getLongExtra(EXTRA_GAME_ID, -1L)?.takeIf { it != -1L }
                overlayViewModel.setActiveGame(gameId)
                showOverlay()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        hideOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        _viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─── Overlay principal ───────────────────────────────────────────────────

    private fun showOverlay() {
        startForeground(NOTIF_ID, buildNotification())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        addBubble()
    }

    private fun hideOverlay() {
        removeDismissLayer()
        removeBubble()
        removePanel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    // ─── Burbuja ─────────────────────────────────────────────────────────────

    private fun addBubble() {
        if (bubbleView != null) return

        val params = bubbleLayoutParams()
        val view = ComposeView(this).also { bubbleView = it }
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setContent {
            val uiState by overlayViewModel.uiState.collectAsState()
            OverlayBubble(state = uiState.bubbleState)
        }

        var downRawX = 0f; var downRawY = 0f
        var downParamX = 0; var downParamY = 0
        var dragged = false; var downTime = 0L

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX; downRawY = event.rawY
                    downParamX = params.x; downParamY = params.y
                    dragged = false; downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragged && (abs(dx) > DRAG_THRESHOLD || abs(dy) > DRAG_THRESHOLD))
                        dragged = true
                    if (dragged) {
                        params.x = (downParamX + dx).toInt()
                        params.y = (downParamY + dy).toInt()
                        bubbleX = params.x; bubbleY = params.y
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = System.currentTimeMillis() - downTime
                    when {
                        // Si el panel está abierto, cualquier tap en la burbuja lo cierra
                        !dragged && panelView != null -> hidePanel()
                        !dragged && elapsed < 400     -> showPanel()
                        !dragged && elapsed >= 700    -> stopSelf()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
    }

    private fun bubbleLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = bubbleX; y = bubbleY
    }

    private fun removeBubble() {
        bubbleView?.let { windowManager.removeView(it); bubbleView = null }
    }

    // ─── Panel ───────────────────────────────────────────────────────────────

    private fun showPanel() {
        if (panelView != null) return
        overlayViewModel.reset()

        // Capa transparente que cubre el resto de la pantalla — tocarla cierra el panel
        // Es la forma más confiable en MIUI porque ACTION_OUTSIDE no siempre dispara
        addDismissLayer()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity       = Gravity.BOTTOM
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val view = ComposeView(this).also { panelView = it }
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)

        view.setContent {
            val uiState by overlayViewModel.uiState.collectAsState()

            OverlayPanel(
                content         = uiState.content,
                saved           = uiState.bubbleState is BubbleState.Saved,
                error           = uiState.error,
                activeGameName  = uiState.activeGameName,
                onContentChange = overlayViewModel::onContentChange,
                onSave          = { overlayViewModel.saveNote { hidePanel() } },
                onDismiss       = { hidePanel() },
                onOpenApp       = {
                    hidePanel()
                    startActivity(
                        Intent(this@OverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                }
            )
        }

        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) hidePanel()
            false
        }

        windowManager.addView(view, params)

        // MIUI requiere toggleSoftInput en lugar de showSoftInput para overlays
        view.post {
            view.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            @Suppress("DEPRECATION")
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    private fun hidePanel() {
        removeDismissLayer()
        removePanel()
        overlayViewModel.reset()
        if (bubbleView == null) addBubble()
    }

    private fun removePanel() {
        panelView?.let { v ->
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            windowManager.removeView(v)
            panelView = null
        }
    }

    // Capa transparente que cubre la pantalla excepto el panel (abajo)
    // Tocarla es la salida de emergencia más confiable en MIUI
    private fun addDismissLayer() {
        if (dismissView != null) return
        val view = android.view.View(this).also { dismissView = it }
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        view.setOnClickListener { hidePanel() }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP }
        windowManager.addView(view, params)
    }

    private fun removeDismissLayer() {
        dismissView?.let { windowManager.removeView(it); dismissView = null }
    }

    // ─── Notificación ────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "GameNote Overlay", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Burbuja flotante activa" }
            )
        }
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply { action = ACTION_HIDE },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GameNote activo")
            .setContentText("Toca la burbuja para anotar · mantén presionada para cerrar")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desactivar", stopPi)
            .build()
    }
}
