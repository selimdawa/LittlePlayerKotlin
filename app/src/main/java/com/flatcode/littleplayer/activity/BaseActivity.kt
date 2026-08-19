package com.flatcode.littleplayer.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import io.selimdawa.multicolors.MultiColorManager

abstract class BaseActivity<VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater) -> VB
) : AppCompatActivity() {

    protected lateinit var binding: VB
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        MultiColorManager.applyTheme(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = bindingInflater(layoutInflater)
        setContentView(binding.root)

        applyInitialTheme()
        setupViews()
        observeViewModel()
    }

    /**
     * Override this to apply theme colors to views immediately after setContentView.
     * This helps prevent flickering when using dynamic themes (Palette/White).
     */
    open fun applyInitialTheme() {}

    /**
     * Apply edge-to-edge padding to the specified views.
     */
    protected fun applyEdgeToEdge(topView: View? = null, bottomView: View? = null) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topView?.updatePadding(top = systemBars.top)
            bottomView?.updatePadding(bottom = systemBars.bottom)
            // If no specific views are provided, apply padding to the root or ensure it's handled
            if (topView == null && bottomView == null) {
                v.updatePadding(bottom = systemBars.bottom)
            } else if (bottomView == null) {
                v.updatePadding(bottom = systemBars.bottom)
            }
            insets
        }
    }

    /**
     * Override this to setup your UI components
     */
    open fun setupViews() {}

    /**
     * Override this to observe LiveData or Flow from ViewModel
     */
    open fun observeViewModel() {}

    override fun onResume() {
        super.onResume()
        // Re-apply theme if needed when returning to activity
        MultiColorManager.applyTheme(this)
    }
}
