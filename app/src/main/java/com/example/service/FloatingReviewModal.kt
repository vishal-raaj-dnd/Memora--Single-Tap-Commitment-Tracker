package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.ai.ExtractedMemory
import com.example.data.model.Category
import com.example.data.model.MemoryType
import com.example.util.DateUtils

/**
 * Neo-Brutalist Floating Overlay Review & Clarification Modal.
 * Renders directly over WhatsApp, Chrome, or any active app via WindowManager (TYPE_APPLICATION_OVERLAY).
 * Allows reviewing AI extraction, resolving date/time ambiguities (e.g. 3:30 AM vs PM),
 * and saving directly to Room DB without opening the full application.
 */
class FloatingReviewModal(
    private val context: Context,
    private val windowManager: WindowManager,
    private val memory: ExtractedMemory,
    private val screenshotBitmap: Bitmap?,
    private val categories: List<Category>,
    private val onSave: (title: String, categoryId: String, type: MemoryType, date: String?, time: String?) -> Unit,
    private val onOpenInApp: () -> Unit,
    private val onDismiss: () -> Unit
) {

    private var modalView: View? = null
    private val density = context.resources.displayMetrics.density

    private fun dp(value: Float): Int = (value * density).toInt()

    fun show() {
        dismiss()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = context.resources.displayMetrics
        val maxWidth = (displayMetrics.widthPixels * 0.92f).toInt()
        val targetWidth = dp(360f).coerceAtMost(maxWidth)

        val params = WindowManager.LayoutParams(
            targetWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        }

        var currentTitle = memory.title
        var selectedCatId = memory.suggestedCategoryId
        var selectedType = memory.type
        var selectedDate = memory.date
        var selectedTime = memory.time

        val rootCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(16f), dp(18f), dp(18f))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFFDF6")) // Warm White
                cornerRadius = dp(20f).toFloat()
                setStroke(dp(2.5f), Color.parseColor("#111111"))
            }
            elevation = dp(8f).toFloat()

            setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    dismiss()
                    onDismiss()
                    true
                } else {
                    false
                }
            }
        }

        // 1. Header: Pill Badge + Dismiss (X)
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            weightSum = 1f
        }

        val badge = TextView(context).apply {
            text = "MEMORA CAPTURE"
            setTextColor(Color.parseColor("#111111"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFD92F")) // Main Yellow
                cornerRadius = dp(10f).toFloat()
                setStroke(dp(1.5f), Color.parseColor("#111111"))
            }
        }
        headerRow.addView(badge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            weight = 1f
        })

        val closeButton = TextView(context).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#111111"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
            setOnClickListener { dismiss() }
        }
        headerRow.addView(closeButton)
        rootCard.addView(headerRow)

        // 2. Screenshot thumbnail + Editable Title Row
        val contentRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12f), 0, dp(10f))
        }

        if (screenshotBitmap != null) {
            val thumb = ImageView(context).apply {
                setImageBitmap(screenshotBitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    cornerRadius = dp(12f).toFloat()
                    setStroke(dp(2f), Color.parseColor("#111111"))
                }
                clipToOutline = true
            }
            val thumbParams = LinearLayout.LayoutParams(dp(54f), dp(54f)).apply {
                marginEnd = dp(12f)
            }
            contentRow.addView(thumb, thumbParams)
        }

        val titleInput = EditText(context).apply {
            setText(currentTitle)
            setTextColor(Color.parseColor("#111111"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10f), dp(8f), dp(10f), dp(8f))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F5F3EB")) // Cream
                cornerRadius = dp(12f).toFloat()
                setStroke(dp(1.5f), Color.parseColor("#111111"))
            }
            maxLines = 2
        }
        contentRow.addView(titleInput, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rootCard.addView(contentRow)

        // 3. Category & Date/Time Badge Row
        val metaRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2f), 0, dp(10f))
        }

        val categoryObj = categories.find { it.id == selectedCatId }
        val catBadge = TextView(context).apply {
            text = categoryObj?.name ?: memory.categoryName
            setTextColor(Color.parseColor("#111111"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10f), dp(5f), dp(10f), dp(5f))
            val catColor = try {
                Color.parseColor(categoryObj?.colorHex ?: "#FFE885")
            } catch (_: Exception) {
                Color.parseColor("#FFE885")
            }
            background = GradientDrawable().apply {
                setColor(catColor)
                cornerRadius = dp(10f).toFloat()
                setStroke(dp(1.5f), Color.parseColor("#111111"))
            }
        }
        metaRow.addView(catBadge)

        val dateText = TextView(context).apply {
            val dateLabel = DateUtils.getDisplayDateLabel(selectedDate)
            val timeLabel = selectedTime ?: ""
            text = if (timeLabel.isNotBlank()) "  $dateLabel · $timeLabel" else "  $dateLabel"
            setTextColor(Color.parseColor("#444444"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        metaRow.addView(dateText)
        rootCard.addView(metaRow)

        // 4. Ambiguity Clarification Section (Dynamic)
        if (memory.hasAmbiguity && !memory.ambiguityQuestion.isNullOrBlank() && memory.ambiguityOptions.isNotEmpty()) {
            val ambBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFF4BF")) // Soft Yellow alert
                    cornerRadius = dp(14f).toFloat()
                    setStroke(dp(1.5f), Color.parseColor("#111111"))
                }
            }

            val ambTitle = TextView(context).apply {
                text = memory.ambiguityQuestion
                setTextColor(Color.parseColor("#111111"))
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }
            ambBox.addView(ambTitle)

            val chipsScroll = HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(0, dp(6f), 0, 0)
            }
            val chipsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val chipViews = mutableListOf<TextView>()

            memory.ambiguityOptions.forEachIndexed { index, option ->
                val isSelected = index == 0
                val chip = TextView(context).apply {
                    text = option
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(12f), dp(6f), dp(12f), dp(6f))

                    fun updateChipStyle(selected: Boolean) {
                        setTextColor(Color.parseColor("#111111"))
                        background = GradientDrawable().apply {
                            setColor(if (selected) Color.parseColor("#FFD92F") else Color.WHITE)
                            cornerRadius = dp(10f).toFloat()
                            setStroke(if (selected) dp(2f) else dp(1.25f), Color.parseColor("#111111"))
                        }
                    }

                    updateChipStyle(isSelected)

                    setOnClickListener {
                        chipViews.forEach { it.background = GradientDrawable().apply {
                            setColor(Color.WHITE)
                            cornerRadius = dp(10f).toFloat()
                            setStroke(dp(1.25f), Color.parseColor("#111111"))
                        }}
                        updateChipStyle(true)

                        // Update current state based on ambiguity field
                        if (memory.ambiguityField == "time" || option.contains("AM") || option.contains("PM")) {
                            selectedTime = option
                        } else if (memory.ambiguityField == "date" || option.contains("Today") || option.contains("Tomorrow") || option.contains(",")) {
                            selectedDate = option
                        }
                        val dLabel = DateUtils.getDisplayDateLabel(selectedDate)
                        val tLabel = selectedTime ?: ""
                        dateText.text = if (tLabel.isNotBlank()) "  $dLabel · $tLabel" else "  $dLabel"
                    }
                }
                chipViews.add(chip)
                chipsRow.addView(chip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(8f)
                })
            }
            chipsScroll.addView(chipsRow)
            ambBox.addView(chipsScroll)

            val ambParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4f)
                bottomMargin = dp(12f)
            }
            rootCard.addView(ambBox, ambParams)
        }

        // 5. Action Buttons: [ Save Memory ] (Primary Yellow)
        val saveBtn = Button(context).apply {
            text = "Save Memory"
            setTextColor(Color.parseColor("#111111"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFD92F")) // Main Yellow
                cornerRadius = dp(14f).toFloat()
                setStroke(dp(2.5f), Color.parseColor("#111111"))
            }
            setOnClickListener {
                val finalTitle = titleInput.text.toString().trim().ifBlank { memory.title }
                onSave(finalTitle, selectedCatId, selectedType, selectedDate, selectedTime)
                Toast.makeText(context, "Memory saved to ${categoryObj?.name ?: "Personal"}!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
        val saveParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48f)).apply {
            topMargin = dp(4f)
        }
        rootCard.addView(saveBtn, saveParams)

        // 6. Secondary Links: [ Open in App ] and [ Discard ]
        val secondaryRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10f), 0, 0)
        }

        val openAppBtn = TextView(context).apply {
            text = "Open in Memora →"
            setTextColor(Color.parseColor("#333333"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12f), dp(4f), dp(12f), dp(4f))
            setOnClickListener {
                dismiss()
                onOpenInApp()
            }
        }
        secondaryRow.addView(openAppBtn)

        val discardBtn = TextView(context).apply {
            text = "Discard"
            setTextColor(Color.parseColor("#888888"))
            textSize = 13f
            setPadding(dp(12f), dp(4f), dp(12f), dp(4f))
            setOnClickListener {
                dismiss()
                onDismiss()
            }
        }
        secondaryRow.addView(discardBtn)
        rootCard.addView(secondaryRow)

        modalView = rootCard

        try {
            android.util.Log.i("FloatingReviewModal", "Showing review modal for memory: ${memory.title}")
            windowManager.addView(rootCard, params)
        } catch (e: Exception) {
            android.util.Log.e("FloatingReviewModal", "Failed to add review modal to window manager", e)
        }
    }

    fun dismiss() {
        modalView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            modalView = null
        }
    }
}
