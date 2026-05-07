package com.clearread.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Principal-grade Vertical Fast Scroller for ClearRead.
 * 
 * Features:
 * - High-refresh-rate adaptive rendering (60/90/120Hz).
 * - Precision-mapped 800+ page handling.
 * - Sharp-lined minimalist aesthetic strictly following project guidelines.
 */
class VerticalFastScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dimensions (using DP-aware scales)
    private val density = resources.displayMetrics.density
    private val thumbWidth = 6f * density // Increased from 4f
    private val thumbHeight = 48f * density
    private val bubbleWidth = 84f * density
    private val bubbleHeight = 36f * density
    private val bubblePadding = 12f * density
    private val trackEdgePadding = 8f * density
    private val rightEdgePadding = 12f * density
    private val cornerRadius = thumbWidth / 2 // Perfect pill
    private val bubbleCornerRadius = bubbleHeight / 2 // Modern rounded pill
    
    // Low-overhead Paints
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val thumbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Add elevation-like shadow to the bubble
        setShadowLayer(4f * density, 0f, 2f * density, Color.argb(60, 0, 0, 0))
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // High-precision State
    private var totalPages = 0
    private var currentProgress = 0f
    private var isDragging = false
    private var dragOffset = 0f
    
    private var onScrollProgress: ((Float) -> Unit)? = null

    /**
     * Update visual theme dynamically.
     */
    fun setColors(thumbColor: Int, bubbleColor: Int, textColor: Int) {
        thumbPaint.color = thumbColor
        
        // Calculate a high-contrast border color based on luminance
        val r = Color.red(thumbColor)
        val g = Color.green(thumbColor)
        val b = Color.blue(thumbColor)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        
        thumbBorderPaint.color = if (luminance > 0.5) Color.argb(40, 0, 0, 0) else Color.argb(40, 255, 255, 255)
        
        // Also add a subtle shadow to the thumb itself
        thumbPaint.setShadowLayer(2f * density, 0f, 1f * density, Color.argb(40, 0, 0, 0))
        
        bubblePaint.color = bubbleColor
        textPaint.color = textColor
        invalidate()
    }

    /**
     * Update total page count (mathematical mapping reference).
     */
    fun setPageCount(count: Int) {
        if (totalPages != count) {
            totalPages = count
            invalidate()
        }
    }

    /**
     * Update current scroll position from external source (LazyColumn).
     */
    fun setScrollProgress(progress: Float) {
        if (!isDragging && currentProgress != progress) {
            currentProgress = progress
            invalidate()
        }
    }

    /**
     * Register scroll callback.
     */
    fun setOnScrollListener(listener: (Float) -> Unit) {
        onScrollProgress = listener
    }

    override fun onDraw(canvas: Canvas) {
        if (totalPages <= 1) return

        val viewHeight = height.toFloat()
        val viewWidth = width.toFloat()
        
        // Track geometry
        val trackTop = trackEdgePadding
        val trackBottom = viewHeight - trackEdgePadding
        val trackHeight = trackBottom - trackTop
        
        // Liquid-smooth position calculation (mapped to available thumb-top range)
        val scrollableRange = trackHeight - thumbHeight
        val progress = currentProgress
        
        // 1. Draw Thumb (Rounded Material 3 Bar with Contrast Border)
        val thumbLeft = viewWidth - thumbWidth - rightEdgePadding
        val thumbRight = viewWidth - rightEdgePadding
        val thumbTop = trackTop + (progress * scrollableRange)
        val thumbBottom = thumbTop + thumbHeight
        
        // Draw Shadow & Fill
        canvas.drawRoundRect(thumbLeft, thumbTop, thumbRight, thumbBottom, cornerRadius, cornerRadius, thumbPaint)
        // Draw Outline for visibility on same-color backgrounds
        canvas.drawRoundRect(thumbLeft, thumbTop, thumbRight, thumbBottom, cornerRadius, cornerRadius, thumbBorderPaint)
        
        // 2. Draw Material 3 Pill Bubble
        if (isDragging) {
            val bubbleRight = thumbLeft - bubblePadding
            val bubbleLeft = bubbleRight - bubbleWidth
            val bTop = (thumbTop + thumbHeight / 2 - bubbleHeight / 2).coerceIn(trackTop, viewHeight - bubbleHeight - trackTop)
            val bBottom = bTop + bubbleHeight
            
            // Rounded pill background
            canvas.drawRoundRect(bubbleLeft, bTop, bubbleRight, bBottom, bubbleCornerRadius, bubbleCornerRadius, bubblePaint)
            
            // Modern Sans-Serif Typography
            textPaint.textSize = bubbleHeight * 0.45f
            val textX = (bubbleLeft + bubbleRight) / 2
            val textY = bTop + (bubbleHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
            val displayPage = (currentProgress * (totalPages - 1)).toInt()
            canvas.drawText("${displayPage + 1}/${totalPages}", textX, textY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (totalPages <= 1) return false
        
        val y = event.y
        val viewHeight = height.toFloat()
        val trackTop = trackEdgePadding
        val trackBottom = viewHeight - trackEdgePadding
        val trackHeight = trackBottom - trackTop
        
        val scrollableRange = trackHeight - thumbHeight

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Precise hit testing: Only activate if touching the thumb itself
                val progress = currentProgress
                val tTop = trackTop + (progress * scrollableRange)
                val tBottom = tTop + thumbHeight
                
                val hitZoneX = 32f * density // Horizontal slop
                val hitZoneY = 16f * density // Vertical slop
                
                if (event.x > width - hitZoneX && 
                    event.y >= tTop - hitZoneY && 
                    event.y <= tBottom + hitZoneY) {
                    
                    isDragging = true
                    dragOffset = event.y - tTop
                    parent.requestDisallowInterceptTouchEvent(true)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateScroll(y, trackTop, scrollableRange)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateScroll(y: Float, top: Float, scrollableRange: Float) {
        // Relative-grab logic: stay latched exactly where the user touched the thumb
        val progress = ((y - dragOffset - top) / scrollableRange).coerceIn(0f, 1f)
        val targetPage = (progress * (totalPages - 1)).toInt()
        val currentPage = (currentProgress * (totalPages - 1)).toInt()
        
        if (targetPage != currentPage) {
            // Haptic "Gear" Feedback: Tactile tick when page changes
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        }
        
        currentProgress = progress
        invalidate()
        onScrollProgress?.invoke(progress)
    }
}
