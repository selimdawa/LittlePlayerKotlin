package com.flatcode.littleplayer.activity

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.AlbumDetailsAdapter
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityPlayerBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.loadBitmap
import com.flatcode.littleplayer.utils.loadLogoOrBitmap
import com.flatcode.littleplayer.utils.setPaletteGradient
import com.flatcode.littleplayer.utils.togglePlayPause
import com.flatcode.littleplayer.viewmodel.PlayerViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.linc.amplituda.Amplituda
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.graphics.toColorInt

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : AppCompatActivity(), Player.Listener {

    private lateinit var binding: ActivityPlayerBinding
    private val context: Context = this@PlayerActivity
    private val viewModel: PlayerViewModel by viewModels()

    private var progressJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private lateinit var amplituda: Amplituda

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        amplituda = Amplituda(this)

        getIntentMethod()
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    if ((mediaController != null) && fromUser) {
                        mediaController?.seekTo(progress.toLong() * 1000)
                        val duration = mediaController?.duration ?: 0L
                        if (duration > 0) {
                            val progressPercentage =
                                (progress.toFloat() * 1000 / duration.toFloat()) * 100
                            binding.waveformSeekBar.progress = progressPercentage
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            },
        )

        binding.buttonPanel.shuffle.setOnClickListener { 
            mediaController?.let {
                it.shuffleModeEnabled = !it.shuffleModeEnabled
            }
        }
        
        binding.buttonPanel.repeat.setOnClickListener { 
            mediaController?.let {
                val nextMode = when (it.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                    else -> Player.REPEAT_MODE_OFF
                }
                it.repeatMode = nextMode
            }
        }

        binding.buttonPanel.prev.setOnClickListener { prevBtn() }
        binding.buttonPanel.next.setOnClickListener { nextBtn() }
        binding.buttonPanel.playPauseBtn.setOnClickListener { playPauseBtn() }
        binding.buttonPanel.favorite.setOnClickListener { viewModel.toggleFavorite() }
    }

    private fun observeViewModel() {
        viewModel.isFavorite.observe(this) { isFavorite ->
            val icon = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            binding.buttonPanel.favorite.setImageResource(icon)
        }

        viewModel.currentSong.observe(this) { song ->
            song?.let {
                binding.songName.text = it.title
                binding.songArtist.text = it.artist
                metaData(it.path?.toUri())
                it.path?.let { path -> loadWaveform(path) }
            }
        }
    }

    private fun loadWaveform(path: String) {
        amplituda.processAudio(path).get(
            { result ->
                val amplitudesList = result.amplitudesAsList()
                val amplitudesArray = amplitudesList.toIntArray()
                runOnUiThread {
                    binding.waveformSeekBar.setSampleFrom(amplitudesArray)
                }
            },
            { exception ->
                exception.printStackTrace()
            },
        )
    }

    private fun getIntentMethod() {
        val position = intent.getIntExtra(DATA.POSITION, -1)
        val sender = intent.getStringExtra(DATA.SENDER)

        viewModel.listSongs = if ((sender != null) && (sender == DATA.ALBUM_DETAILS)) {
            AlbumDetailsAdapter.albumFiles ?: ArrayList()
        } else {
            MusicAdapter.mFiles ?: ArrayList()
        }

        if (viewModel.listSongs.isNotEmpty() && position != -1) {
            binding.buttonPanel.playPause.setImageResource(R.drawable.ic_pause)
            viewModel.updatePositionAndSong(position)
        }
    }

    private fun playPauseBtn() {
        mediaController?.togglePlayPause(
            binding.buttonPanel.playPause,
            { stopProgressUpdater() },
            { startProgressUpdater() },
        )
    }

    private fun prevBtn() {
        mediaController?.seekToPreviousMediaItem()
    }

    private fun nextBtn() {
        mediaController?.seekToNextMediaItem()
    }

    private fun playCurrentSong() {
        mediaController?.let { controller ->
            val mediaItems = viewModel.listSongs.map { song ->
                val uri = song.path?.toUri() ?: "".toUri()
                val metadata =
                    MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()
                MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).setMediaId(song.id ?: "")
                    .build()
            }

            // Atomic call to set items AND seek to the correct position immediately
            controller.setMediaItems(mediaItems, viewModel.position, 0L)
            controller.prepare()
            controller.play()

            val song = viewModel.listSongs[viewModel.position]
            binding.songName.text = song.title
            binding.songArtist.text = song.artist
            metaData(song.path?.toUri())
            song.path?.let { loadWaveform(it) }

            resetProgressLoop()
            binding.buttonPanel.playPause.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun resetProgressLoop() {
        stopProgressUpdater()
        startProgressUpdater()
    }

    private fun startProgressUpdater() {
        stopProgressUpdater()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    val currentPos = controller.currentPosition
                    val duration = controller.duration

                    if (duration > 0) {
                        val mCurrentPositionSec = (currentPos / 1000).toInt()
                        val durationSec = (duration / 1000).toInt()

                        if (binding.seekBar.max != durationSec) {
                            binding.seekBar.max = durationSec
                        }

                        binding.seekBar.progress = mCurrentPositionSec

                        val progressPercentage = (currentPos.toFloat() / duration.toFloat()) * 100
                        binding.waveformSeekBar.progress = progressPercentage

                        binding.durationPlayed.text = formattedTime(mCurrentPositionSec)
                    }
                }
                delay(1000.milliseconds)
            }
        }
    }

    private fun stopProgressUpdater() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(this)
                onControllerConnected()
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun onControllerConnected() {
        mediaController?.let { controller ->
            val intentPosition = intent.getIntExtra(DATA.POSITION, -1)
            
            if (intentPosition == -1 && controller.currentMediaItem != null) {
                // Navigating from Mini Player, sync with controller
                val index = controller.currentMediaItemIndex
                if (index in viewModel.listSongs.indices) {
                    viewModel.updatePositionAndSong(index)
                }
            } else if (intentPosition != -1 && controller.currentMediaItem?.mediaId != viewModel.currentSong.value?.id) {
                // Specifically requested a new song
                playCurrentSong()
            }

            val duration = (controller.duration / 1000).toInt()
            if (duration > 0) {
                binding.seekBar.max = duration
            }

            resetProgressLoop()
            updatePlayPauseButton(controller.isPlaying)
            updateRepeatShuffleIcons(controller)
            viewModel.updatePlaybackCycleFromController(controller.repeatMode, controller.shuffleModeEnabled)
        }
    }

    override fun onStop() {
        super.onStop()
        mediaController?.removeListener(this)
        MediaController.releaseFuture(controllerFuture!!)
        mediaController = null
        stopProgressUpdater()
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            binding.buttonPanel.playPause.setImageResource(R.drawable.ic_pause)
        } else {
            binding.buttonPanel.playPause.setImageResource(R.drawable.ic_play)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updatePlayPauseButton(isPlaying)
        if (isPlaying) startProgressUpdater() else stopProgressUpdater()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaController?.let { controller ->
            val index = controller.currentMediaItemIndex
            // Only update position if it's a valid index and not triggered by initial setup
            if (index != -1 && index in viewModel.listSongs.indices) {
                if (index != viewModel.position) {
                    viewModel.updatePositionAndSong(index)
                }
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
            updateRepeatShuffleIcons(player)
            viewModel.updatePlaybackCycleFromController(player.repeatMode, player.shuffleModeEnabled)
        }
    }

    private fun updateRepeatShuffleIcons(player: Player) {
        val repeatIcon = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_on
            else -> R.drawable.ic_repeat_off
        }
        binding.buttonPanel.repeat.setImageResource(repeatIcon)

        val shuffleIcon = if (player.shuffleModeEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off
        binding.buttonPanel.shuffle.setImageResource(shuffleIcon)
    }

    private fun formattedTime(currentPosition: Int): String {
        val seconds = (currentPosition % 60).toString()
        val minutes = (currentPosition / 60).toString()
        return if (seconds.length == 1) "$minutes:0$seconds" else "$minutes:$seconds"
    }

    private fun metaData(uri: Uri?) {
        if (uri == null) return
        lifecycleScope.launch {
            val retriever = MediaMetadataRetriever()
            val art = withContext(Dispatchers.IO) {
                try {
                    retriever.setDataSource(context, uri)
                    retriever.embeddedPicture
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val durationTotal =
                (viewModel.listSongs[viewModel.position].duration?.toLong() ?: 0L) / 1000
            binding.durationTotal.text = formattedTime(durationTotal.toInt())

            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                binding.image.loadLogoOrBitmap(bitmap)
                binding.imageBlur.setPaletteGradient(bitmap)
            } else {
                binding.image.loadBitmap(null)
                binding.imageBlur.setImageDrawable(null)
                binding.imageBlur.setBackgroundColor("#121212".toColorInt())
                binding.songName.setTextColor(Color.WHITE)
                binding.songArtist.setTextColor(Color.DKGRAY)
            }

            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}