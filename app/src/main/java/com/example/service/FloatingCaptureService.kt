package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ai.ExtractedMemory
import com.example.ai.GeminiVisionAnalyzer
import com.example.data.local.MemoraDatabase
import com.example.data.model.Category
import com.example.data.model.MemoryItem
import com.example.data.model.MemoryType
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Foreground service that displays a small draggable floating capture button
 * outside the Memora app (e.g. over WhatsApp, Chrome, Gmail).
 * Tapping it immediately hides the button, captures the real Android screen via
 * MediaProjection, saves it to internal storage, runs Gemini VLM in the background
 * with a loader on the floating button, and displays a floating review modal without
 * interrupting other applications.
 */
class FloatingCaptureService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var spinnerView: ProgressBar? = null
    private var buttonImageView: ImageView? = null
    private var dismissTargetView: View? = null
    private var activeModal: FloatingReviewModal? = null
    private var activeMediaProjection: MediaProjection? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "memora_floating_capture_service"
        const val NOTIFICATION_ID = 901

        const val ACTION_PROJECTION_DATA = "com.example.ACTION_PROJECTION_DATA"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        var isServiceRunning = false
            private set

        private var savedResultCode: Int = Activity.RESULT_CANCELED
        private var savedResultData: Intent? = null

        fun start(context: Context) {
            if (savedResultData != null && savedResultCode == Activity.RESULT_OK) {
                startWithProjection(context, savedResultCode, savedResultData!!)
            } else {
                MediaProjectionPermissionActivity.launch(context)
            }
        }

        fun startWithProjection(context: Context, resultCode: Int, resultData: Intent) {
            savedResultCode = resultCode
            savedResultData = resultData

            val intent = Intent(context, FloatingCaptureService::class.java).apply {
                action = ACTION_PROJECTION_DATA
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingCaptureService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        setupFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PROJECTION_DATA) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            @Suppress("DEPRECATION")
            val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)

            if (resultCode == Activity.RESULT_OK && resultData != null) {
                initMediaProjection(resultCode, resultData)
            }
        }
        return START_STICKY
    }

    private fun initMediaProjection(resultCode: Int, resultData: Intent) {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            activeMediaProjection?.stop()
            val projection = projectionManager.getMediaProjection(resultCode, resultData)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                projection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        activeMediaProjection = null
                    }
                }, mainHandler)
            }
            activeMediaProjection = projection
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createForegroundNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Memora Floating Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Memora Capture is active")
            .setContentText("Tap floating button anytime to capture screen")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupFloatingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val density = metrics.density
        val sizePx = (38 * density).toInt()

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (12 * density).toInt()
            y = (240 * density).toInt()
        }

        val frame = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Compact tactile circle with Memora Logo
        val button = ImageView(this).apply {
            setImageResource(com.example.R.drawable.ic_memora_floating_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            elevation = 6 * density
        }

        val spinner = ProgressBar(this).apply {
            val pad = (6 * density).toInt()
            setPadding(pad, pad, pad, pad)
            indeterminateDrawable?.setColorFilter(
                android.graphics.Color.parseColor("#111111"),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
            visibility = View.GONE
        }

        frame.addView(button)
        frame.addView(spinner)
        buttonImageView = button
        spinnerView = spinner
        floatingView = frame

        // Bottom-center ✕ Dismiss Target for drag-to-remove
        val targetSizePx = (56 * density).toInt()
        val dismissParams = WindowManager.LayoutParams(
            targetSizePx,
            targetSizePx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (48 * density).toInt()
        }

        val dismissFrame = FrameLayout(this).apply {
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor("#1E1E1E"))
                setStroke((2 * density).toInt(), android.graphics.Color.parseColor("#FFFFFF"))
            }
            background = bg
            visibility = View.GONE
            elevation = 12 * density

            val xText = android.widget.TextView(this@FloatingCaptureService).apply {
                text = "✕"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            addView(xText, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }

        try {
            windowManager?.addView(dismissFrame, dismissParams)
            dismissTargetView = dismissFrame
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true
        var isOverTarget = false
        val loc = IntArray(2)

        frame.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    isOverTarget = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                        if (dismissFrame.visibility != View.VISIBLE) {
                            dismissFrame.visibility = View.VISIBLE
                            dismissFrame.alpha = 0f
                            dismissFrame.animate().alpha(1f).setDuration(120).start()
                        }
                    }
                    params.x = (initialX + dx).coerceIn(0, screenWidth - sizePx)
                    params.y = (initialY + dy).coerceIn(0, screenHeight - sizePx)
                    windowManager?.updateViewLayout(frame, params)

                    // Check proximity to bottom-center dismiss target using physical coordinates
                    dismissFrame.getLocationOnScreen(loc)
                    val targetCenterX = if (loc[0] > 0 || loc[1] > 0) loc[0] + (dismissFrame.width / 2f) else screenWidth / 2f
                    val targetCenterY = if (loc[0] > 0 || loc[1] > 0) loc[1] + (dismissFrame.height / 2f) else screenHeight - (48 * density) - (targetSizePx / 2f)
                    val dist = Math.hypot((event.rawX - targetCenterX).toDouble(), (event.rawY - targetCenterY).toDouble())
                    val wasOver = isOverTarget
                    isOverTarget = dist < (70 * density)

                    val targetBg = dismissFrame.background as? android.graphics.drawable.GradientDrawable
                    if (isOverTarget) {
                        if (!wasOver) {
                            dismissFrame.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            dismissFrame.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120).start()
                        }
                        frame.alpha = 0.5f
                        targetBg?.setColor(android.graphics.Color.parseColor("#FF4D4D")) // Vivid Red
                        targetBg?.setStroke((3 * density).toInt(), android.graphics.Color.WHITE)
                    } else {
                        if (wasOver) {
                            dismissFrame.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                        }
                        frame.alpha = 1.0f
                        targetBg?.setColor(android.graphics.Color.parseColor("#1E1E1E"))
                        targetBg?.setStroke((2 * density).toInt(), android.graphics.Color.WHITE)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    dismissFrame.visibility = View.GONE
                    dismissFrame.scaleX = 1.0f
                    dismissFrame.scaleY = 1.0f
                    frame.alpha = 1.0f

                    if (isClick) {
                        triggerScreenCapture()
                    } else if (isOverTarget) {
                        // Dropped on dismiss target -> close service!
                        android.widget.Toast.makeText(this@FloatingCaptureService, "Memora floating button removed", android.widget.Toast.LENGTH_SHORT).show()
                        stopSelf()
                    } else {
                        // Snap to nearest screen edge
                        val snapToLeft = params.x < (screenWidth / 2)
                        params.x = if (snapToLeft) (12 * density).toInt() else (screenWidth - sizePx - (12 * density).toInt())
                        windowManager?.updateViewLayout(frame, params)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(frame, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        mainHandler.post {
            if (isLoading) {
                buttonImageView?.alpha = 0.25f
                spinnerView?.visibility = View.VISIBLE
            } else {
                buttonImageView?.alpha = 1.0f
                spinnerView?.visibility = View.GONE
            }
        }
    }

    private fun triggerScreenCapture() {
        val projection = activeMediaProjection
        if (projection == null) {
            // Need permission token
            MediaProjectionPermissionActivity.launch(this)
            return
        }

        // 1. Instantly hide floating overlay so it does NOT appear in the screenshot
        floatingView?.visibility = View.GONE

        // 2. Wait 60ms for compositor to redraw without the button, then capture
        mainHandler.postDelayed({
            captureScreenWithProjection(projection)
        }, 60)
    }

    private fun captureScreenWithProjection(projection: MediaProjection) {
        try {
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val densityDpi = metrics.densityDpi

            val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR

            var virtualDisplay: VirtualDisplay? = null

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)

                    // Save original screenshot to permanent internal storage
                    val memoriesDir = File(filesDir, "memories").apply { mkdirs() }
                    val file = File(memoriesDir, "screenshot_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    // Restore floating button overlay in LOADING state
                    mainHandler.post {
                        floatingView?.visibility = View.VISIBLE
                        setLoadingState(true)
                    }

                    // Background non-intrusive Gemini VLM analysis
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            android.util.Log.i("FloatingCaptureService", "Starting background VLM analysis on screenshot: ${file.absolutePath}")
                            val database = MemoraDatabase.getDatabase(applicationContext, serviceScope)
                            val categories = database.categoryDao().getAllCategoriesSync()
                            val corrections = database.categoryCorrectionDao().getRecentCorrectionsSync()
                            val prefs = getSharedPreferences("memora_prefs", Context.MODE_PRIVATE)
                            val apiKey = prefs.getString("custom_api_key", null)

                            val analyzer = GeminiVisionAnalyzer()
                            val result = analyzer.analyzeScreenshot(
                                bitmap = croppedBitmap,
                                categories = categories,
                                userCorrections = corrections,
                                apiKeyOverride = apiKey
                            ).copy(originalScreenshotUri = file.absolutePath)

                            android.util.Log.i("FloatingCaptureService", "VLM analysis complete: '${result.title}' [Date: ${result.date}, Time: ${result.time}, Ambiguity: ${result.hasAmbiguity}]")

                            withContext(Dispatchers.Main) {
                                setLoadingState(false)
                                activeModal?.dismiss()
                                activeModal = FloatingReviewModal(
                                    context = this@FloatingCaptureService,
                                    windowManager = windowManager ?: return@withContext,
                                    memory = result,
                                    screenshotBitmap = croppedBitmap,
                                    categories = categories,
                                    onSave = { title, catId, type, date, time ->
                                        serviceScope.launch(Dispatchers.IO) {
                                            val memoryItem = MemoryItem(
                                                title = title,
                                                description = result.description,
                                                type = type,
                                                categoryId = catId,
                                                date = date,
                                                time = time,
                                                isDeadline = type == MemoryType.DEADLINE,
                                                source = "Screenshot",
                                                sourceApp = "Screen Capture",
                                                originalScreenshotUri = file.absolutePath,
                                                aiSummary = result.summary,
                                                aiConfidence = result.confidence
                                            )
                                            database.memoryDao().insertMemory(memoryItem)
                                            android.util.Log.i("FloatingCaptureService", "Saved memory to database: $title")
                                        }
                                    },
                                    onOpenInApp = {
                                        val launchIntent = Intent(this@FloatingCaptureService, MainActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            putExtra("captured_screenshot_path", file.absolutePath)
                                        }
                                        startActivity(launchIntent)
                                    },
                                    onDismiss = {
                                        activeModal = null
                                    }
                                )
                                activeModal?.show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FloatingCaptureService", "Error during VLM analysis or modal display", e)
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                setLoadingState(false)
                                val fallbackMemory = ExtractedMemory(
                                    title = "Captured Memory",
                                    description = "Screen captured at ${DateUtils.getDisplayDateLabel(java.time.LocalDate.now().toString())}",
                                    type = MemoryType.NOTE,
                                    suggestedCategoryId = "personal",
                                    categoryName = "Personal",
                                    date = DateUtils.getDisplayDateLabel(java.time.LocalDate.now().toString()),
                                    time = null,
                                    originalScreenshotUri = file.absolutePath,
                                    confidence = 0.6f
                                )
                                activeModal?.dismiss()
                                activeModal = FloatingReviewModal(
                                    context = this@FloatingCaptureService,
                                    windowManager = windowManager ?: return@withContext,
                                    memory = fallbackMemory,
                                    screenshotBitmap = croppedBitmap,
                                    categories = listOf(Category(id = "personal", name = "Personal", icon = "target", colorHex = "#FFE885")),
                                    onSave = { title, catId, type, date, time ->
                                        serviceScope.launch(Dispatchers.IO) {
                                            val database = MemoraDatabase.getDatabase(applicationContext, serviceScope)
                                            val memoryItem = MemoryItem(
                                                title = title,
                                                description = "Screen captured",
                                                type = type,
                                                categoryId = catId,
                                                date = date,
                                                time = time,
                                                source = "Screenshot",
                                                originalScreenshotUri = file.absolutePath
                                            )
                                            database.memoryDao().insertMemory(memoryItem)
                                        }
                                    },
                                    onOpenInApp = {
                                        val launchIntent = Intent(this@FloatingCaptureService, MainActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            putExtra("captured_screenshot_path", file.absolutePath)
                                        }
                                        startActivity(launchIntent)
                                    },
                                    onDismiss = {
                                        activeModal = null
                                    }
                                )
                                activeModal?.show()
                            }
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    mainHandler.post {
                        floatingView?.visibility = View.VISIBLE
                        setLoadingState(false)
                    }
                } finally {
                    image.close()
                    virtualDisplay?.release()
                    imageReader.close()
                }
            }, mainHandler)

            virtualDisplay = projection.createVirtualDisplay(
                "MemoraCaptureDisplay",
                width,
                height,
                densityDpi,
                flags,
                imageReader.surface,
                null,
                mainHandler
            )

        } catch (e: Exception) {
            e.printStackTrace()
            floatingView?.visibility = View.VISIBLE
            setLoadingState(false)
            // If token expired or was single-use on Android 14, request again
            MediaProjectionPermissionActivity.launch(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        activeMediaProjection?.stop()
        activeMediaProjection = null
        activeModal?.dismiss()
        activeModal = null
        serviceScope.cancel()
        dismissTargetView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dismissTargetView = null
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
