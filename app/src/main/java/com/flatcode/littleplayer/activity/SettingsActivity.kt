package com.flatcode.littleplayer.activity

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivitySettingsBinding
import com.flatcode.littleplayer.databinding.DialogAboutBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.snackbar
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.settings))
        setupListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    private fun setupListeners() {
        binding.btnNightMode.setOnClickListener {
            binding.root.snackbar(getString(R.string.disabled))
        }

        binding.settingScanMedia.setOnClickListener {
            viewModel.rescanMedia()
            binding.root.snackbar(getString(R.string.library_rescan_started))
        }

        binding.settingSleepTimer.setOnClickListener {
            showSleepTimerDialog()
        }

        binding.settingEqualizer.setOnClickListener {
            launchActivity<EqualizerActivity>()
        }

        binding.settingAccount.setOnClickListener { binding.root.snackbar(getString(R.string.account_settings_coming_soon)) }
        binding.settingNotifications.setOnClickListener {
            binding.root.snackbar(getString(R.string.notifications_managed_by_system))
        }
        binding.settingDataStorage.setOnClickListener {
            launchActivity<DataStorageActivity>()
        }
        binding.settingPrivacy.setOnClickListener { showPrivacyDialog() }
        binding.settingAbout.setOnClickListener { showAboutDialog() }
    }

    private fun showPrivacyDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.privacy_policy_title)
            .setMessage(R.string.privacy_policy_content)
            .show()
    }

    private fun showAboutDialog() {
        val aboutBinding = DialogAboutBinding.inflate(layoutInflater)

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            aboutBinding.tvVersion.text = getString(R.string.version_format, version)
        } catch (_: Exception) {
            aboutBinding.tvVersion.text = getString(R.string.version_format, "1.0.0")
        }

        MaterialAlertDialogBuilder(this)
            .setView(aboutBinding.root)
            .show()
    }

    private fun showSleepTimerDialog() {
        val options = arrayOf(
            getString(R.string.off),
            getString(R.string._15_minutes),
            getString(R.string._30_minutes),
            getString(R.string._60_minutes)
        )
        val values = intArrayOf(0, 15, 30, 60)

        AlertDialog.Builder(this).setTitle(R.string.set_sleep_timer).setItems(options) { _, which ->
            val minutes = values[which]
            setSleepTimer(minutes)
            binding.tvSleepTimerStatus.text = options[which]
        }.show()
    }

    private fun setSleepTimer(minutes: Int) {
        if (mediaController == null) {
            binding.root.snackbar(getString(R.string.music_service_not_connected))
            return
        }
        mediaController?.let { controller ->
            val bundle = Bundle().apply { putInt("MINUTES", minutes) }
            controller.sendCustomCommand(
                SessionCommand(MusicService.COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY), bundle
            )
            binding.root.snackbar(getString(R.string.timer_set_format, minutes))
        }
    }

    private fun observeViewModel() {
        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }
}