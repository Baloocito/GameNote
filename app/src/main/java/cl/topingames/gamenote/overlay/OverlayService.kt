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
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cl.topingames.gamenote.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs

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
    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null

    // Posición persistente de la burbuja entre sesiones
    private var bubbleX = 20
    private var bubbleY = 300

    companion object {
        const val ACTION_SHOW = "cl.topingames.gamenote.OVERLAY_SHOW"
        const val ACTION_HIDE  = "cl.topingames.gamenote.OVERLAY_HIDE"
        private const val CHANNEL_ID = "gamenote_overlay"
        private const val NOTIF_ID   = 1
        private const val DRAG_THRESHOLD = 8f   // px mínimos para considerar drag
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> { hideOverlay(); stopSelf() }
            else        -> showOverlay()
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
            OverlayBubble()   // puramente visual, sin callbacks de Compose
        }

        // ── Touch manejado 100% en View layer ──────────────────────────────
        var downRawX = 0f
        var downRawY = 0f
        var downParamX = 0
        var downParamY = 0
        var dragged = false
        var downTime = 0L

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX   = event.rawX
                    downRawY   = event.rawY
                    downParamX = params.x
                    downParamY = params.y
                    dragged    = false
                    downTime   = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragged && (abs(dx) > DRAG_THRESHOLD || abs(dy) > DRAG_THRESHOLD)) {
                        dragged = true
                    }
                    if (dragged) {
                        params.x = (downParamX + dx).toInt()
                        params.y = (downParamY + dy).toInt()
                        bubbleX  = params.x
                        bubbleY  = params.y
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = System.currentTimeMillis() - downTime
                    when {
                        // Tap corto → abrir panel
                        !dragged && elapsed < 400 -> showPanel()
                        // Long press → cerrar servicio
                        !dragged && elapsed >= 700 -> stopSelf()
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
        x = bubbleX
        y = bubbleY
    }

    private fun removeBubble() {
        bubbleView?.let { windowManager.removeView(it); bubbleView = null }
    }

    // ─── Panel ───────────────────────────────────────────────────────────────

    private fun showPanel() {
        if (panelView != null) return
        removeBubble()

        // El panel necesita foco para el teclado
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity     = Gravity.BOTTOM
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val view = ComposeView(this).also { panelView = it }
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setContent {
            OverlayPanel(
                onSaved   = { hidePanel() },
                onDismiss = { hidePanel() },
                onOpenApp = {
                    hidePanel()
                    startActivity(
                        Intent(this@OverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                }
            )
        }

        windowManager.addView(view, params)
    }

    private fun hidePanel() {
        removePanel()
        addBubble()
    }

    private fun removePanel() {
        panelView?.let { windowManager.removeView(it); panelView = null }
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
