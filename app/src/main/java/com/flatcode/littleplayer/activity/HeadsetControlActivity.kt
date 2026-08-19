package com.flatcode.littleplayer.activity

import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityHeadsetControlBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class HeadsetControlActivity : BaseActivity<ActivityHeadsetControlBinding>(ActivityHeadsetControlBinding::inflate) {

    private val viewModel: SettingsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    private val actions = listOf(
        DATA.ACTION_NONE,
        DATA.ACTION_PLAY_PAUSE_TOGGLE,
        DATA.ACTION_NEXT_TRACK,
        DATA.ACTION_PREV_TRACK,
        DATA.ACTION_FAST_FORWARD,
        DATA.ACTION_REWIND,
        DATA.ACTION_FAVORITE_TOGGLE
    )

    override fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            binding.customToolbar.root.updatePadding(top = systemBars.top)
            insets
        }

        initToolbar(getString(R.string.headset_controls))
        setupSpinners()
    }

    private fun setupSpinners() {
        val displayNames = actions.map { action ->
            when (action) {
                DATA.ACTION_NONE -> getString(R.string.none)
                DATA.ACTION_PLAY_PAUSE_TOGGLE -> getString(R.string.play_pause)
                DATA.ACTION_NEXT_TRACK -> getString(R.string.next)
                DATA.ACTION_PREV_TRACK -> getString(R.string.previous)
                DATA.ACTION_FAST_FORWARD -> getString(R.string.fast_forward)
                DATA.ACTION_REWIND -> getString(R.string.rewind)
                DATA.ACTION_FAVORITE_TOGGLE -> getString(R.string.toggle_favorite)
                else -> action
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerDoubleAction.adapter = adapter
        binding.spinnerTripleAction.adapter = adapter

        binding.spinnerDoubleAction.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                viewModel.setHeadsetDoubleClickAction(actions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        binding.spinnerTripleAction.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                viewModel.setHeadsetTripleClickAction(actions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    override fun observeViewModel() {
        viewModel.doubleClickActionFlow.collectWithLifecycle(this) { action ->
            val index = actions.indexOf(action)
            if (index != -1 && binding.spinnerDoubleAction.selectedItemPosition != index) {
                binding.spinnerDoubleAction.setSelection(index)
            }
        }

        viewModel.tripleClickActionFlow.collectWithLifecycle(this) { action ->
            val index = actions.indexOf(action)
            if (index != -1 && binding.spinnerTripleAction.selectedItemPosition != index) {
                binding.spinnerTripleAction.setSelection(index)
            }
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }
}