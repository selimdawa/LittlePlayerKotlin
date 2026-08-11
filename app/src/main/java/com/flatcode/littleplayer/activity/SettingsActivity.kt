package com.flatcode.littleplayer.activity

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivitySettingsBinding
import com.flatcode.littleplayer.databinding.DialogAboutBinding
import com.flatcode.littleplayer.fragment.SleepTimerBottomSheet
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.appVersionName
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.showDialog
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
            try {
                if (!isFinishing && !isDestroyed) {
                    mediaController = controllerFuture?.get()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    private fun setupListeners() {
        binding.switchNightMode.setOnClickListener {
            val isChecked = binding.switchNightMode.isChecked
            val mode =
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            if (AppCompatDelegate.getDefaultNightMode() != mode) {
                viewModel.setDarkMode(mode)
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        binding.btnNightMode.setOnClickListener {
            binding.switchNightMode.performClick()
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

        binding.settingNotifications.setOnClickListener {
            launchActivity<NotificationsActivity>()
        }
        binding.settingDataStorage.setOnClickListener {
            launchActivity<DataStorageActivity>()
        }
        binding.settingLanguage.setOnClickListener { showLanguageDialog() }
        binding.settingPrivacy.setOnClickListener { showPrivacyDialog() }
        binding.settingAbout.setOnClickListener { showAboutDialog() }
    }

    private fun showPrivacyDialog() {
        showDialog(R.string.privacy_policy_title, R.string.privacy_policy_content)
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.system_default),
            getString(R.string.english),
            getString(R.string.arabic),
            getString(R.string.spanish)
        )
        val languageTags = arrayOf("", "en", "ar", "es")

        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
        val currentTag = currentLocale?.language ?: ""
        val checkedItem = languageTags.indexOf(currentTag).let { if (it == -1) 0 else it }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language)
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val tag = languageTags[which]
                val appLocale: LocaleListCompat = if (tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
                AppCompatDelegate.setApplicationLocales(appLocale)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAboutDialog() {
        val aboutBinding = DialogAboutBinding.inflate(layoutInflater)
        aboutBinding.tvVersion.text = getString(R.string.version_format, appVersionName)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(aboutBinding.root)
            .setPositiveButton(R.string.ok, null)
            .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun showSleepTimerDialog() {
        SleepTimerBottomSheet { minutes, status ->
            setSleepTimer(minutes)
            binding.tvSleepTimerStatus.text = status
        }.show(supportFragmentManager, "SleepTimer")
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
        viewModel.darkModeFlow.collectWithLifecycle(this) { mode ->
            binding.switchNightMode.isChecked = mode == AppCompatDelegate.MODE_NIGHT_YES
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }
}