package com.flatcode.littleplayer.activity

import android.os.Bundle
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.DATA
import com.google.common.util.concurrent.ListenableFuture
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivitySettingsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.snackbar
import com.flatcode.littleplayer.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

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
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            viewModel.setDarkMode(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.settingScanMedia.setOnClickListener {
            viewModel.rescanMedia()
            binding.root.snackbar("Library Rescan Started")
        }

        binding.settingSleepTimer.setOnClickListener {
            showSleepTimerDialog()
        }

        binding.settingEqualizer.setOnClickListener {
            launchActivity<EqualizerActivity>()
        }
    }

    private fun showSleepTimerDialog() {
        val options = arrayOf("Off", "15 minutes", "30 minutes", "60 minutes")
        val values = intArrayOf(0, 15, 30, 60)

        AlertDialog.Builder(this)
            .setTitle("Set Sleep Timer")
            .setItems(options) { _, which ->
                val minutes = values[which]
                setSleepTimer(minutes)
                binding.tvSleepTimerStatus.text = options[which]
            }
            .show()
    }

    private fun setSleepTimer(minutes: Int) {
        mediaController?.let { controller ->
            val bundle = Bundle().apply { putInt("MINUTES", minutes) }
            controller.sendCustomCommand(
                androidx.media3.session.SessionCommand(MusicService.COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY),
                bundle
            )
        }
    }

    private fun observeViewModel() {
        viewModel.darkModeFlow.collectWithLifecycle(this) { mode ->
            binding.switchDarkMode.isChecked = (mode == AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}