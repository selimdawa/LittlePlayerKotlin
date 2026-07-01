package com.flatcode.littleplayer.unit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class AudioWaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var amplitudes: List<Int> = ArrayList()
    private var progress = 0f

    private val wavePaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val progressPaint = Paint().apply {
        color = Color.parseColor("#8A47EB")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    var onProgressChangedListener: ((Float) -> Unit)? = null

    fun setWaveformData(amplitudesList: List<Int>) {
        this.amplitudes = amplitudesList
        invalidate()
    }

    fun setProgress(progressPercentage: Float) {
        this.progress = progressPercentage / 100f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (amplitudes.isEmpty()) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val totalBars = amplitudes.size

        val barWidth = viewWidth / totalBars
        val maxAmplitude = amplitudes.maxOrNull() ?: 1

        for (i in 0 until totalBars) {
            val currentProgressX = i.toFloat() / totalBars
            val paintToUse = if (currentProgressX <= progress) progressPaint else wavePaint

            val barHeight = (amplitudes[i].toFloat() / maxAmplitude) * viewHeight
            val left = i * barWidth
            val top = (viewHeight - barHeight) / 2
            val right = left + barWidth - 2f
            val bottom = top + barHeight

            canvas.drawRect(left, top, right, bottom, paintToUse)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                progress = event.x / width.toFloat()
                progress = progress.coerceIn(0f, 1f)
                invalidate()
                onProgressChangedListener?.invoke(progress * 100)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}