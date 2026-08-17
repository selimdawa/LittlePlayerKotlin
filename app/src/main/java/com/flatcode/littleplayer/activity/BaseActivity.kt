package com.flatcode.littleplayer.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import io.selimdawa.multicolors.MultiColorManager

abstract class BaseActivity<VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater) -> VB
) : AppCompatActivity() {

    protected lateinit var binding: VB
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        MultiColorManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = bindingInflater(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        observeViewModel()
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
