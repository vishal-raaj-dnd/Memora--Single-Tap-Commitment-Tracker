package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Destinations
import com.example.ui.navigation.MemoraApp
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MemoraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MemoraViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MemoraViewModel(application) as T
            }
        }
    }

    private var activeIntentState by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activeIntentState = intent

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CreamBackground
                ) {
                    val navController = rememberNavController()

                    MemoraApp(
                        viewModel = viewModel,
                        navController = navController
                    )

                    val curIntent = activeIntentState
                    LaunchedEffect(curIntent) {
                        if (curIntent == null) return@LaunchedEffect

                        val capturedScreenshotPath = curIntent.getStringExtra("captured_screenshot_path")
                        val triggerCapture = curIntent.getBooleanExtra("trigger_screen_capture", false)
                        val targetMemoryId = curIntent.getLongExtra("memory_id", -1L)
                        val sharedImageUri = if (curIntent.action == Intent.ACTION_SEND && curIntent.type?.startsWith("image/") == true) {
                            @Suppress("DEPRECATION")
                            curIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        } else null

                        if (!capturedScreenshotPath.isNullOrBlank()) {
                            // Direct system-wide floating capture
                            viewModel.startCaptureAndAnalysis(
                                existingScreenshotPath = capturedScreenshotPath,
                                onNavigateToProcessing = { navController.navigate(Destinations.PROCESSING) },
                                onNavigateToReview = {
                                    navController.navigate(Destinations.REVIEW_SAVE) {
                                        popUpTo(Destinations.PROCESSING) { inclusive = true }
                                    }
                                }
                            )
                        } else if (sharedImageUri != null) {
                            try {
                                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val source = ImageDecoder.createSource(contentResolver, sharedImageUri)
                                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                        decoder.isMutableRequired = true
                                    }
                                } else {
                                    contentResolver.openInputStream(sharedImageUri)?.use { stream ->
                                        BitmapFactory.decodeStream(stream)
                                    }
                                }
                                if (bitmap != null) {
                                    viewModel.startCaptureAndAnalysis(
                                        providedBitmap = bitmap,
                                        onNavigateToProcessing = { navController.navigate(Destinations.PROCESSING) },
                                        onNavigateToReview = {
                                            navController.navigate(Destinations.REVIEW_SAVE) {
                                                popUpTo(Destinations.PROCESSING) { inclusive = true }
                                            }
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else if (triggerCapture) {
                            navController.navigate(Destinations.CAPTURE)
                        } else if (targetMemoryId != -1L) {
                            navController.navigate(Destinations.itemDetailRoute(targetMemoryId))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFloatingServiceState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeIntentState = intent
    }
}
