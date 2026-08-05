package com.flatcode.littleplayer.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.withStyledAttributes

class PlayPauseView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    init {
        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.foregroundTint)) {
            getColorStateList(0)?.let {
                paint.color = it.defaultColor
            }
        }
    }

    private var progress = 0f // 0 is playing, 1 is paused
    private var isPlaying = false
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 300
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    fun setPlaying(playing: Boolean, animate: Boolean = true) {
        if (isPlaying == playing) return
        isPlaying = playing
        
        if (animate) {
            if (playing) animator.start() else animator.reverse()
        } else {
            progress = if (playing) 1f else 0f
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val availableW = (width - paddingLeft - paddingRight).toFloat()
        val availableH = (height - paddingTop - paddingBottom).toFloat()
        
        if (availableW <= 0 || availableH <= 0) return

        val s = minOf(availableW, availableH)
        val left = paddingLeft + (availableW - s) / 2
        val top = paddingTop + (availableH - s) / 2

        if (progress < 0.5f) {
            val p = progress * 2
            drawPlay(canvas, left, top, s, p)
        } else {
            drawPause(canvas, left, top, s)
        }
    }

    private fun drawPlay(canvas: Canvas, l: Float, t: Float, s: Float, p: Float) {
        val path = Path()
        val inset = s * 0.1f * (1 - p)
        path.moveTo(l + inset, t)
        path.lineTo(l + s, t + s / 2)
        path.lineTo(l + inset, t + s)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawPause(canvas: Canvas, l: Float, t: Float, s: Float) {
        val barW = s * 0.35f
        val gap = s * 0.3f
        
        // Left bar
        canvas.drawRect(l, t, l + barW, t + s, paint)
        // Right bar
        canvas.drawRect(l + barW + gap, t, l + barW + gap + barW, t + s, paint)
    }
}