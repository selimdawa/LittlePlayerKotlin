package com.flatcode.littleplayer.activity

import android.content.ComponentName
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityPlayerBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.formatAsTime
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.getLyrics
import com.flatcode.littleplayer.utils.gone
import com.flatcode.littleplayer.utils.isVisible
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.togglePlayPause
import com.flatcode.littleplayer.utils.visible
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
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
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : AppCompatActivity(), Player.Listener {

    private lateinit var binding: ActivityPlayerBinding

    private val viewModel: PlayerViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    private var progressJob: Job? = null
    private var waveformJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private lateinit var amplituda: Amplituda
    private var currentDominantColor: Int = Color.GRAY
    private var currentMode: Int = DATA.MODE_BASIC
    private var lyricsJob: Job? = null
    private var isIntentProcessed = false
    private var isAnimating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isIntentProcessed = savedInstanceState?.getBoolean("intent_processed") ?: false
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        amplituda = Amplituda(this)

        getIntentMethod()
        setupListeners()
        observeViewModel()
    }

    private fun updatePlayerUIColors(color: Int) {
        val colorStateList = ColorStateList.valueOf(color)

        binding.seekBar.progressTintList = colorStateList
        binding.seekBar.thumbTintList = colorStateList
        binding.seekBar.secondaryProgressTintList = colorStateList
        binding.seekBar.progressBackgroundTintList = colorStateList

        val background = binding.buttonPanel.playPauseBtn.background.mutate()
        if (background is GradientDrawable) {
            if (currentMode == DATA.MODE_BASIC) {
                background.colors =
                    intArrayOf(getLibraryColor("mc_track"), getLibraryColor("mc_tick"))
            } else {
                background.colors = intArrayOf(color, color)
            }
            binding.buttonPanel.playPauseBtn.background = background
        }

        binding.waveformSeekBar.waveProgressColor = color

        binding.basicColor.strokeWidth = if (currentMode == DATA.MODE_BASIC) 4 else 1
        binding.paletteColor.strokeWidth = if (currentMode == DATA.MODE_PALETTE) 4 else 1
        binding.whiteColor.strokeWidth = if (currentMode == DATA.MODE_WHITE) 4 else 1
    }

    private fun setupListeners() {
        binding.back.setOnClickListener { supportFinishAfterTransition() }

        binding.basicColor.setOnClickListener {
            nowPlayerViewModel.setThemeColorMode(DATA.MODE_BASIC)
        }
        binding.paletteColor.setOnClickListener {
            nowPlayerViewModel.setThemeColorMode(DATA.MODE_PALETTE)
        }
        binding.whiteColor.setOnClickListener {
            nowPlayerViewModel.setThemeColorMode(DATA.MODE_WHITE)
        }

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
                                (progress.toFloat() * 1000f / duration.toFloat()) * 100f
                            binding.waveformSeekBar.progress = progressPercentage
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            },
        )

        binding.buttonPanel.repeat.setOnClickListener {
            mediaController?.let { controller ->
                when {
                    !controller.shuffleModeEnabled && controller.repeatMode != Player.REPEAT_MODE_ONE -> {
                        controller.repeatMode = Player.REPEAT_MODE_ONE
                        controller.shuffleModeEnabled = false
                    }

                    !controller.shuffleModeEnabled && controller.repeatMode == Player.REPEAT_MODE_ONE -> {
                        controller.repeatMode = Player.REPEAT_MODE_ALL
                        controller.shuffleModeEnabled = true
                    }

                    else -> {
                        controller.repeatMode = Player.REPEAT_MODE_ALL
                        controller.shuffleModeEnabled = false
                    }
                }
            }
        }

        binding.buttonPanel.prev.setOnClickListener { prevBtn() }
        binding.buttonPanel.next.setOnClickListener { nextBtn() }
        binding.buttonPanel.playPauseBtn.setOnClickListener { playPauseBtn() }
        binding.buttonPanel.favorite.setOnClickListener { viewModel.toggleFavorite() }

        binding.lyricsBtn.setOnClickListener { toggleLyrics() }

        val gestureDetector = GestureDetector(this, SwipeGestureListener())
        binding.card.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                true
            } else {
                if (event.action == MotionEvent.ACTION_UP) {
                    v.performClick()
                }
                true
            }
        }
    }

    private fun toggleLyrics() {
        if (binding.lyricsContainer.visibility == android.view.View.VISIBLE) {
            binding.lyricsContainer.gone()
        } else {
            binding.lyricsContainer.visible()
        }
    }


    private fun observeViewModel() {
        viewModel.isFavorite.collectWithLifecycle(this) { isFavorite ->
            val icon = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            binding.buttonPanel.favorite.setImageResource(icon)
        }

        nowPlayerViewModel.themeColorMode.collectWithLifecycle(this) { mode ->
            currentMode = mode
            applyCurrentModeColors()
        }

        viewModel.currentSong.collectWithLifecycle(this) { song ->
            song?.let {
                binding.songName.text = it.title
                binding.songArtist.text = it.artist
                updateSongUI(it)
                loadLyrics(it)
                lifecycleScope.launch {
                    delay(300.milliseconds)
                    it.path?.let { path -> it.id?.let { id -> loadWaveform(id, path) } }
                }
            }
        }
    }

    private fun loadLyrics(song: MusicFiles) {
        lyricsJob?.cancel()
        if (song.lyrics != null) {
            binding.lyricsText.text = song.lyrics
            return
        }

        binding.lyricsText.setText(R.string.no_lyrics)

        lyricsJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lyrics = getLyrics(song.path)
                if (isActive && lyrics != null) {
                    runOnUiThread {
                        binding.lyricsText.text = lyrics
                    }
                    song.id?.let { viewModel.updateLyrics(it, lyrics) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun applyCurrentModeColors() {
        when (currentMode) {
            DATA.MODE_BASIC -> updatePlayerUIColors(getLibraryColor("mc_track"))
            DATA.MODE_PALETTE -> updatePlayerUIColors(currentDominantColor)
            DATA.MODE_WHITE -> updatePlayerUIColors(Color.WHITE)
        }
    }

    private fun updateSongUI(song: MusicFiles) {
        binding.durationTotal.text = song.durationDuration.formatAsTime()
        binding.image.loadSongImage(song.albumId, song.path, song.cachedImagePath)
        binding.imageBlur.loadSongImageBlur(song.albumId, 100, song.path, song.cachedImagePath)

        val request =
            ImageRequest.Builder(this).data(song.cachedImagePath ?: song.path).allowHardware(false)
                .target { result ->
                    val bitmap = (result as? BitmapDrawable)?.bitmap
                    bitmap?.let {
                        Palette.from(it).generate { palette ->
                            currentDominantColor = palette?.getVibrantColor(Color.GRAY)
                                ?: palette?.getLightVibrantColor(
                                    Color.GRAY
                                ) ?: palette?.getDominantColor(Color.GRAY) ?: Color.GRAY

                            binding.paletteColor.setCardBackgroundColor(currentDominantColor)
                            nowPlayerViewModel.updateThemeColor(currentDominantColor)

                            if (currentMode == DATA.MODE_PALETTE) {
                                applyCurrentModeColors()
                            }
                        }
                    }
                }.build()
        imageLoader.enqueue(request)
    }

    private fun loadWaveform(songId: String, path: String) {
        waveformJob?.cancel()
        waveformJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cachedSong = viewModel.getSongById(songId)
                if (cachedSong?.waveform != null) {
                    val amplitudes =
                        cachedSong.waveform.split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
                    if (isActive && amplitudes.isNotEmpty()) {
                        runOnUiThread {
                            binding.waveformSeekBar.setSampleFrom(amplitudes)
                        }
                        return@launch
                    }
                }

                val result = amplituda.processAudio(path).get()
                val amplitudesArray = result.amplitudesAsList().toIntArray()

                if (amplitudesArray.isNotEmpty()) {
                    val waveformString = amplitudesArray.joinToString(",")
                    viewModel.updateWaveform(songId, waveformString)
                }

                if (isActive) {
                    runOnUiThread {
                        binding.waveformSeekBar.setSampleFrom(amplitudesArray)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getIntentMethod() {
        val position = intent.getIntExtra(DATA.POSITION, -1)

        if (position != -1 && !isIntentProcessed) {
            binding.buttonPanel.playPause.setImageResource(R.drawable.ic_pause)
            viewModel.updatePositionAndSong(position)
        }
    }

    private fun playPauseBtn() {
        mediaController?.togglePlayPause(
            binding.buttonPanel.playPause, { stopProgressUpdater() }) { startProgressUpdater() }
    }

    private fun prevBtn(animate: Boolean = true) {
        if (animate) {
            animateSkip(false)
            return
        }
        mediaController?.let { controller ->
            controller.seekToPreviousMediaItem()
            if (!controller.playWhenReady) {
                controller.play()
            }
        }
    }

    private fun nextBtn(animate: Boolean = true) {
        if (animate) {
            animateSkip(true)
            return
        }
        mediaController?.let { controller ->
            controller.seekToNextMediaItem()
            if (!controller.playWhenReady) {
                controller.play()
            }
        }
    }

    private fun animateSkip(toNext: Boolean) {
        if (isAnimating) return
        isAnimating = true
        val width = binding.card.width.toFloat()
        val outX = if (toNext) -width else width
        val inX = if (toNext) width else -width

        binding.card.animate()
            .translationX(outX)
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                if (toNext) nextBtn(animate = false) else prevBtn(animate = false)
                binding.card.translationX = inX
                binding.card.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(150)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction { isAnimating = false }
                    .start()
            }
            .start()
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        prevBtn()
                    } else {
                        nextBtn()
                    }
                    return true
                }
            }
            return false
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
                        if (binding.seekBar.max != duration.toInt() / 1000) {
                            binding.seekBar.max = duration.toInt() / 1000
                        }

                        binding.seekBar.progress = (currentPos / 1000).toInt()

                        val progressPercentage = (currentPos.toFloat() / duration.toFloat()) * 100
                        binding.waveformSeekBar.progress = progressPercentage

                        binding.durationPlayed.text = currentPos.milliseconds.formatAsTime()
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

            if (intentPosition != -1 && !isIntentProcessed) {
                isIntentProcessed = true
                intent.removeExtra(DATA.POSITION)
                if (viewModel.listSongs.isNotEmpty()) {
                    forcePlaySong(controller, intentPosition)
                } else {
                    lifecycleScope.launch {
                        while (viewModel.listSongs.isEmpty()) {
                            delay(30.milliseconds)
                        }
                        forcePlaySong(controller, intentPosition)
                    }
                }
            } else {
                if (controller.currentMediaItem != null) {
                    val index = controller.currentMediaItemIndex
                    if (index in viewModel.listSongs.indices) {
                        viewModel.updatePositionAndSong(index)
                    }
                } else {
                    lifecycleScope.launch {
                        var count = 0
                        while (viewModel.listSongs.isEmpty() || (viewModel.position == -1 && count < 50)) {
                            delay(30.milliseconds)
                            count++
                        }
                        if (viewModel.position != -1 && viewModel.listSongs.isNotEmpty()) {
                            setupMediaItems(controller)
                        }
                    }
                }
            }

            val duration = (controller.duration / 1000).toInt()
            if (duration > 0) {
                binding.seekBar.max = duration
            }

            resetProgressLoop()
            updatePlayPauseButton(controller.isPlaying)
            updateRepeatShuffleIcons(controller)
        }
    }

    private fun forcePlaySong(controller: MediaController, pos: Int) {
        viewModel.updatePositionAndSong(pos, forceUpdate = true)
        
        val mediaItems: List<MediaItem> = viewModel.listSongs.map { song ->
            val uri = song.path?.toUri() ?: "".toUri()
            val metadata = MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist)
                .setExtras(Bundle().apply {
                    putString("ALBUM_ID", song.albumId)
                    putString("CACHED_IMAGE_PATH", song.cachedImagePath)
                }).build()
            MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).setMediaId(song.id ?: "")
                .build()
        }
        
        controller.setMediaItems(mediaItems, pos, 0L)
        controller.prepare()
        controller.play()
        
        val song = viewModel.listSongs[pos]
        binding.songName.text = song.title
        binding.songArtist.text = song.artist
        updateSongUI(song)
        lifecycleScope.launch {
            delay(300.milliseconds)
            song.path?.let { path -> song.id?.let { id -> loadWaveform(id, path) } }
        }
    }

    private fun setupMediaItems(controller: MediaController) {
        val mediaItems: List<MediaItem> = viewModel.listSongs.map { song ->
            val uri = song.path?.toUri() ?: "".toUri()
            val metadata = MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist)
                .setExtras(Bundle().apply {
                    putString("ALBUM_ID", song.albumId)
                    putString("CACHED_IMAGE_PATH", song.cachedImagePath)
                }).build()
            MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).setMediaId(song.id ?: "")
                .build()
        }
        controller.setMediaItems(mediaItems, viewModel.position, 0L)
        controller.prepare()
    }

    override fun onStop() {
        super.onStop()
        mediaController?.removeListener(this)
        MediaController.releaseFuture(controllerFuture!!)
        mediaController = null
        stopProgressUpdater()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("intent_processed", isIntentProcessed)
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

        mediaController?.let { controller ->
            val currentPos = controller.currentPosition
            val duration = controller.duration
            if (duration > 0) {
                binding.seekBar.progress = (currentPos / 1000).toInt()
                binding.waveformSeekBar.progress = (currentPos.toFloat() / duration.toFloat()) * 100
                binding.durationPlayed.text = currentPos.milliseconds.formatAsTime()
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaController?.let { controller ->
            val index = controller.currentMediaItemIndex
            if (index != -1 && index in viewModel.listSongs.indices) {
                if (index != viewModel.position) {
                    viewModel.updatePositionAndSong(index)
                }
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED
            )
        ) {
            updateRepeatShuffleIcons(player)
        }
    }

    private fun updateRepeatShuffleIcons(player: Player) {
        val cycleIcon = when {
            player.shuffleModeEnabled -> R.drawable.ic_shuffle_on
            player.repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
            else -> R.drawable.ic_repeat_on
        }
        binding.buttonPanel.repeat.setImageResource(cycleIcon)
    }
}