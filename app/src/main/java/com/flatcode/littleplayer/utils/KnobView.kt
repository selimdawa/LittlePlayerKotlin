package com.flatcode.littleplayer.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.flatcode.littleplayer.R
import androidx.core.graphics.toColorInt
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class KnobView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var onProgressChanged: ((Int, Boolean) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 4f, ContextCompat.getColor(context, R.color.black_25))
    }

    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = ContextCompat.getColor(context, R.color.black_12)
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val startAngle = 135f
    private val sweepAngle = 270f

    var trackColor: Int = Color.LTGRAY
    var progressColor: Int = Color.BLUE
    var knobColor: Int = Color.WHITE
    var indicatorColor: Int = Color.BLUE

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).toFloat()
        val padding = 20f
        val strokeWidth = size * 0.1f
        rect.set(padding, padding, size - padding, size - padding)

        // Draw Background Track
        trackPaint.color = trackColor
        trackPaint.strokeWidth = strokeWidth
        canvas.drawArc(rect, startAngle, sweepAngle, false, trackPaint)

        // Draw Progress Track
        progressPaint.color = progressColor
        progressPaint.strokeWidth = strokeWidth
        val currentSweep = (progress / 100f) * sweepAngle
        canvas.drawArc(rect, startAngle, currentSweep, false, progressPaint)

        // Draw Inner Knob Circle
        val knobRadius = (size / 2) - strokeWidth - padding - 15f
        knobPaint.color = knobColor
        canvas.drawCircle(size / 2, size / 2, knobRadius, knobPaint)
        canvas.drawCircle(size / 2, size / 2, knobRadius, knobBorderPaint)

        // Draw Indicator
        val angleRad = (startAngle + currentSweep) * PI / 180.0
        val indicatorDist = knobRadius * 0.75f
        val indicatorX = (size / 2) + (indicatorDist * cos(angleRad)).toFloat()
        val indicatorY = (size / 2) + (indicatorDist * sin(angleRad)).toFloat()

        indicatorPaint.color = indicatorColor
        canvas.drawCircle(indicatorX, indicatorY, strokeWidth / 3f, indicatorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x - (width / 2)
        val y = event.y - (height / 2)

        var angle = atan2(y.toDouble(), x.toDouble()) * 180.0 / PI
        angle = (angle + 360) % 360

        val relativeAngle = (angle - startAngle + 360) % 360.0
        if (relativeAngle > sweepAngle + 30) return true // Dead zone

        val newProgress = (relativeAngle / sweepAngle * 100).toInt().coerceIn(0, 100)

        if (newProgress != progress) {
            progress = newProgress
            onProgressChanged?.invoke(progress, true)
        }

        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
            onProgressChanged?.invoke(progress, true)
        }

        parent.requestDisallowInterceptTouchEvent(true)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}