package com.flatcode.littleplayer.activity

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivitySettingsBinding
import com.flatcode.littleplayer.databinding.DialogAboutBinding
import com.flatcode.littleplayer.databinding.DialogLanguageBinding
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
import io.selimdawa.multicolors.MultiColorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@AndroidEntryPoint
class SettingsActivity : BaseActivity<ActivitySettingsBinding>(ActivitySettingsBinding::inflate) {

    private val viewModel: SettingsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun setupViews() {
        applyEdgeToEdge(topView = binding.customToolbar.root)

        initToolbar(getString(R.string.settings))
        binding.customToolbar.btnNightModeToolbar.isVisible = true
        setupListeners()
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
        binding.settingHeadsetControls.setOnClickListener {
            launchActivity<HeadsetControlActivity>()
        }
        binding.settingDataStorage.setOnClickListener {
            launchActivity<DataStorageActivity>()
        }
        binding.settingLanguage.setOnClickListener { showLanguageDialog() }
        binding.settingPrivacy.setOnClickListener { showPrivacyDialog() }
        binding.settingAbout.setOnClickListener { showAboutDialog() }
        binding.settingAddWidget.setOnClickListener { showAddWidgetDialog() }
    }

    private fun showAddWidgetDialog() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val appWidgetManager = getSystemService(AppWidgetManager::class.java)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val options = arrayOf(
                    getString(R.string.widget_tiny),
                    getString(R.string.widget_square),
                    getString(R.string.widget_compact),
                    getString(R.string.widget_modern),
                    getString(R.string.widget_large)
                )
                MaterialAlertDialogBuilder(this).setTitle(R.string.add_widget_to_home)
                    .setItems(options) { dialog, which ->
                        dialog.dismiss()

                        val providerClass = when (which) {
                            0 -> "com.flatcode.littleplayer.widget.MusicWidget2x1"
                            1 -> "com.flatcode.littleplayer.widget.MusicWidget2x2"
                            2 -> "com.flatcode.littleplayer.widget.MusicWidget4x1"
                            3 -> "com.flatcode.littleplayer.widget.MusicWidget4x2"
                            4 -> "com.flatcode.littleplayer.widget.MusicWidget4x4"
                            else -> "com.flatcode.littleplayer.widget.MusicWidget4x2"
                        }
                        val provider = ComponentName(this, providerClass)

                        // Check if widget already exists
                        val existingIds = appWidgetManager.getAppWidgetIds(provider)
                        if (existingIds.isNotEmpty()) {
                            binding.root.snackbar(getString(R.string.widget_already_exists))
                            return@setItems
                        }

                        try {
                            val success = appWidgetManager.requestPinAppWidget(provider, null, null)
                            if (success) {
                                Toast.makeText(
                                    this,
                                    getString(R.string.widget_added_success),
                                    Toast.LENGTH_SHORT
                                ).show()

                                lifecycleScope.launch {
                                    delay(500.milliseconds)
                                    val intent = Intent(Intent.ACTION_MAIN)
                                    intent.addCategory(Intent.CATEGORY_HOME)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.show()
            } else {
                binding.root.snackbar(getString(R.string.widget_pinning_not_supported))
            }
        } else {
            binding.root.snackbar(getString(R.string.widget_pinning_not_supported))
        }
    }

    private fun showPrivacyDialog() {
        showDialog(R.string.privacy_policy_title, R.string.privacy_policy_content)
    }

    private fun showLanguageDialog() {
        val dialogBinding = DialogLanguageBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)
        val currentTag = currentLocale?.language ?: ""

        dialogBinding.checkSystem.isVisible = currentTag == ""
        dialogBinding.checkEnglish.isVisible = currentTag == "en"
        dialogBinding.checkArabic.isVisible = currentTag == "ar"
        dialogBinding.checkSpanish.isVisible = currentTag == "es"

        val setLanguage = { tag: String ->
            val appLocale: LocaleListCompat = if (tag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(appLocale)
            alertDialog.dismiss()
        }

        dialogBinding.langSystem.setOnClickListener { setLanguage("") }
        dialogBinding.langEnglish.setOnClickListener { setLanguage("en") }
        dialogBinding.langArabic.setOnClickListener { setLanguage("ar") }
        dialogBinding.langSpanish.setOnClickListener { setLanguage("es") }

        alertDialog.show()
    }

    private fun showAboutDialog() {
        val aboutBinding = DialogAboutBinding.inflate(layoutInflater)
        aboutBinding.tvVersion.text = getString(R.string.version_format, appVersionName)
        val dialog = MaterialAlertDialogBuilder(this).setView(aboutBinding.root)
            .setPositiveButton(R.string.ok, null).create()

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

    override fun observeViewModel() {
        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }

        lifecycleScope.launch {
            MultiColorManager.currentThemeId.collect { _ ->
                MultiColorManager.applyTheme(this@SettingsActivity)
            }
        }
    }
}