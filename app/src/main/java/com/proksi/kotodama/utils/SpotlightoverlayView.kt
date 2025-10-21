package com.proksi.kotodama.utils
// SpotlightOverlayView.kt
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import kotlin.math.max

class SpotlightOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30000000") // yarı saydam siyah
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1) karartma
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        if (hasHole) {
            // 2) deliği CLEAR ile aç
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.CLEAR }
                canvas.drawRoundRect(holeRect, holeRadius, holeRadius, p)
            } else {
                canvas.drawRoundRect(holeRect, holeRadius, holeRadius, clearPaint)
            }
            // 3) beyaz halo
            canvas.drawRoundRect(holeRect, holeRadius, holeRadius, haloPaint)
        }
    }


    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
    }

    private val holeRect = RectF()
    private var holeRadius = 24f
    private var hasHole = false

    init {
        // Eskiden: setLayerType(LAYER_TYPE_HARDWARE, null)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
    }


    /** Hedef view için spotlight aç */
    fun highlight(target: View, paddingPx: Int = 12, radiusPx: Float = 24f) {
        holeRadius = radiusPx
        // target'ın global konumunu al
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val overlayScreenX = loc[0]
        val overlayScreenY = loc[1]

        val r = Rect()
        target.getGlobalVisibleRect(r)
        holeRect.set(
            (r.left - overlayScreenX - paddingPx).toFloat(),
            (r.top - overlayScreenY - paddingPx).toFloat(),
            (r.right - overlayScreenX + paddingPx).toFloat(),
            (r.bottom - overlayScreenY + paddingPx).toFloat()
        )
        hasHole = true
        invalidate()
    }

    fun clearHighlight() {
        hasHole = false
        invalidate()
    }

}
