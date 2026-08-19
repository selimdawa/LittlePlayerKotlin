package com.flatcode.littleplayer.activity

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityNotificationsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsActivity : BaseActivity<ActivityNotificationsBinding>(ActivityNotificationsBinding::inflate) {

    private val viewModel: SettingsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    override fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            binding.customToolbar.root.updatePadding(top = systemBars.top)
            insets
        }

        initToolbar(getString(R.string.notifications))
        setupListeners()
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    override fun observeViewModel() {
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