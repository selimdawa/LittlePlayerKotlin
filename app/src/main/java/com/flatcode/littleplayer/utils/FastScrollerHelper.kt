package com.flatcode.littleplayer.utils

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class FastScrollerHelper(
    private val recyclerView: RecyclerView, private val thumb: View, private val bubble: TextView
) {
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        thumb.animate().alpha(0f).setDuration(300).withEndAction {
            thumb.visibility = View.INVISIBLE
        }.start()
        bubble.visibility = View.GONE
    }

    init {
        // Ensure initial state
        thumb.visibility = View.INVISIBLE
        thumb.alpha = 0f
        bubble.visibility = View.GONE

        setupScrollListener()
        setupTouchListener()
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                    showThumb()
                    updateThumbPosition()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    hideThumbDelayed()
                } else {
                    showThumb()
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        thumb.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val relativeY = event.rawY - getRecyclerViewTopOnScreen()
                    scrollTo(relativeY)

                    showThumb()
                    updateThumbPosition()

                    bubble.visibility = View.VISIBLE
                    updateBubbleText()
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    bubble.visibility = View.GONE
                    hideThumbDelayed()
                    true
                }

                else -> false
            }
        }
    }

    private fun getRecyclerViewTopOnScreen(): Int {
        val location = IntArray(2)
        recyclerView.getLocationOnScreen(location)
        return location[1]
    }

    private fun updateThumbPosition() {
        val offset = recyclerView.computeVerticalScrollOffset()
        val extent = recyclerView.computeVerticalScrollExtent()
        val range = recyclerView.computeVerticalScrollRange()

        if (range > extent) {
            val percentage = offset.toFloat() / (range - extent).toFloat()
            val availableHeight = recyclerView.height - thumb.height
            val newY = (percentage * availableHeight).coerceIn(0f, availableHeight.toFloat())
            thumb.translationY = newY

            val thumbCenter = newY + (thumb.height / 2)
            val bubbleY = thumbCenter - (bubble.height / 2)
            bubble.translationY =
                bubbleY.coerceIn(0f, (recyclerView.height - bubble.height).toFloat())
        }
    }

    private fun showThumb() {
        hideHandler.removeCallbacks(hideRunnable)
        if (thumb.visibility != View.VISIBLE) {
            thumb.visibility = View.VISIBLE
            thumb.animate().alpha(1f).setDuration(150).start()
        }
        hideThumbDelayed()
    }

    private fun hideThumbDelayed() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 500)
    }

    private fun scrollTo(y: Float) {
        val percentage = (y / recyclerView.height).coerceIn(0f, 1f)
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        if (itemCount > 0) {
            val targetPos = (percentage * itemCount).toInt().coerceIn(0, itemCount - 1)
            when (val lm = recyclerView.layoutManager) {
                is LinearLayoutManager -> lm.scrollToPositionWithOffset(targetPos, 0)
                is StaggeredGridLayoutManager -> lm.scrollToPositionWithOffset(targetPos, 0)
            }
        }
    }

    private fun updateBubbleText() {
        val firstPos = when (val lm = recyclerView.layoutManager) {
            is LinearLayoutManager -> lm.findFirstVisibleItemPosition()
            is StaggeredGridLayoutManager -> {
                val into = IntArray(lm.spanCount)
                lm.findFirstVisibleItemPositions(into)
                into.minOrNull() ?: RecyclerView.NO_POSITION
            }

            else -> RecyclerView.NO_POSITION
        }

        if (firstPos != RecyclerView.NO_POSITION) {
            val adapter = recyclerView.adapter as? FastScrollableAdapter
            val text = adapter?.getPopupText(firstPos) ?: ""
            bubble.text = text
        }
    }
}