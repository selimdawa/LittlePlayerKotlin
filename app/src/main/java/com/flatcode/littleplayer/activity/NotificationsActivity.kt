package com.flatcode.littleplayer.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityNotificationsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.notifications))
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnShowSongToast.setOnClickListener {
            binding.switchShowSongToast.isChecked = !binding.switchShowSongToast.isChecked
        }

        binding.switchShowSongToast.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowSongToast(isChecked)
        }

        binding.btnSystemNotificationSettings.setOnClickListener {
            val intent = Intent().apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                } else {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", packageName)
                    putExtra("app_uid", applicationInfo.uid)
                }
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.showSongToastFlow.collectWithLifecycle(this) { isEnabled ->
            if (binding.switchShowSongToast.isChecked != isEnabled) {
                binding.switchShowSongToast.isChecked = isEnabled
            }
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }
}