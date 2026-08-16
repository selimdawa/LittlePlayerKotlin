package com.flatcode.littleplayer.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.DeviceInfo
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
import com.flatcode.littleplayer.fragment.PlayerOptionsBottomSheet
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.applySimpleGradient
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.extractDynamicColors
import com.flatcode.littleplayer.utils.formatAsTime
import com.flatcode.littleplayer.utils.getCurrentThemeColors
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.getSongArtwork
import com.flatcode.littleplayer.utils.getSongImageModel
import com.flatcode.littleplayer.utils.loadBitmap
import com.flatcode.littleplayer.utils.onProgressChanged
import com.flatcode.littleplayer.utils.setHaloBackground
import com.flatcode.littleplayer.utils.toMiddleColor
import com.flatcode.littleplayer.utils.togglePlayPause
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
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
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : AppCompatActivity(), Player.Listener {

    private lateinit var binding: ActivityPlayerBinding

    private val viewModel: PlayerViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()

    private var progressJob: Job? = null
    private var waveformJob: Job? = null
    private var updateSongJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private lateinit var amplituda: Amplituda
    private var currentDominantColor: Pair<Int, Int> = Pair(Color.BLACK, Color.BLACK)
    private var currentMode: Int = DATA.MODE_BASIC
    private var isIntentProcessed = false
    private var isTransitionStarted = false
    private var isAnimating = false
    private var lastSongId: String? = null
    private var preloadedBitmap: android.graphics.Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        isIntentProcessed = savedInstanceState?.getBoolean("intent_processed") ?: false
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        amplituda = Amplituda(this)
        val track = getLibraryColor("mc_track")
        val tick = getLibraryColor("mc_tick")
        currentDominantColor = Pair(track, tick)

        getIntentMethod()
        setupListeners()
        observeViewModel()
    }

    private fun updatePlayerUIColors(startColor: Int, endColor: Int) {
        val brightStart = startColor.toMiddleColor()
        val brightEnd = endColor.toMiddleColor()

        val colorStateList = ColorStateList.valueOf(brightStart)
        val backgroundColorStateList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.white_66)
        )

        binding.seekBar.progressTintList = colorStateList
        binding.seekBar.thumbTintList = colorStateList
        binding.seekBar.secondaryProgressTintList = colorStateList
        binding.seekBar.progressBackgroundTintList = backgroundColorStateList

        if (currentMode == DATA.MODE_BASIC) {
            val track = getLibraryColor("mc_track")
            val tick = getLibraryColor("mc_tick")
            binding.playPauseBtn.setHaloBackground(track, tick)
            binding.imageBorder.setHaloBackground(track, tick)
        } else {
            binding.playPauseBtn.setHaloBackground(brightStart, brightEnd)
            binding.imageBorder.setHaloBackground(brightStart, brightEnd)
        }

        binding.waveformSeekBar.waveProgressColor = brightStart
        binding.waveformSeekBar.waveBackgroundColor = ContextCompat.getColor(this, R.color.white_30)

        binding.basicColor.strokeWidth =
            if (currentMode == DATA.MODE_BASIC) resources.getDimensionPixelSize(R.dimen.stroke_width_active)
            else resources.getDimensionPixelSize(R.dimen.stroke_width_inactive)

        binding.paletteColor.strokeWidth =
            if (currentMode == DATA.MODE_PALETTE) resources.getDimensionPixelSize(R.dimen.stroke_width_active)
            else resources.getDimensionPixelSize(R.dimen.stroke_width_inactive)

        binding.whiteColor.strokeWidth =
            if (currentMode == DATA.MODE_WHITE) resources.getDimensionPixelSize(R.dimen.stroke_width_active)
            else resources.getDimensionPixelSize(R.dimen.stroke_width_inactive)
    }

    @SuppressLint("ClickableViewAccessibility")
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

        binding.seekBar.onProgressChanged { progress, fromUser ->
            if ((mediaController != null) && fromUser) {
                mediaController?.seekTo(progress.toLong() * 1000)
                val duration = mediaController?.duration ?: 0L
                if (duration > 0) {
                    val progressPercentage =
                        ((progress.toFloat() * 1000f) / duration.toFloat()) * 100f
                    binding.waveformSeekBar.progress = progressPercentage
                }
            }
        }

        binding.repeat.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.repeat.startAnimation(animation)
            mediaController?.let { controller ->
                when {
                    (!controller.shuffleModeEnabled) && (controller.repeatMode != Player.REPEAT_MODE_ONE) -> {
                        controller.repeatMode = Player.REPEAT_MODE_ONE
                        musicViewModel.saveShuffleMode(enabled = false)
                    }

                    (!controller.shuffleModeEnabled) && (controller.repeatMode == Player.REPEAT_MODE_ONE) -> {
                        controller.repeatMode = Player.REPEAT_MODE_ALL
                        musicViewModel.saveShuffleMode(enabled = true)
                    }

                    else -> {
                        controller.repeatMode = Player.REPEAT_MODE_ALL
                        musicViewModel.saveShuffleMode(enabled = false)
                    }
                }
            }
        }

        binding.prev.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.prev.startAnimation(animation)
            prevBtn()
        }
        binding.next.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.next.startAnimation(animation)
            nextBtn()
        }
        binding.playPauseBtn.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.playPause.startAnimation(animation)
            playPauseBtn()
        }
        binding.favorite.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.favorite.startAnimation(animation)
            viewModel.toggleFavorite()
        }

        binding.moreOptions.setOnClickListener {
            val bottomSheet = PlayerOptionsBottomSheet(
                song = viewModel.currentSong.value,
                mediaController = mediaController,
                onDeleteClick = { song ->
                    musicViewModel.deleteSong(song)
                    nextBtn(animate = false)
                }) { /* onCastClick */ }
            bottomSheet.show(supportFragmentManager, "PlayerOptions")
        }

        val gestureDetector = GestureDetector(this, SwipeGestureListener())
        binding.card.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                true
            } else {
                if (event.action == MotionEvent.ACTION_UP) {
                    v.performClick()
                }
                false
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isFavorite.collectWithLifecycle(this) { isFavorite ->
            val icon = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            binding.favorite.setImageResource(icon)
        }

        nowPlayerViewModel.themeColorMode.collectWithLifecycle(this) { mode ->
            currentMode = mode
            applyCurrentModeColors()
        }

        nowPlayerViewModel.currentThemeColor.collectWithLifecycle(this) { colorPair ->
            colorPair?.let {
                currentDominantColor = it
                binding.paletteColorBg.applySimpleGradient(
                    it.first.toMiddleColor(),
                    it.second.toMiddleColor()
                )
                if (currentMode == DATA.MODE_PALETTE) {
                    applyCurrentModeColors()
                }
            } ?: run {
                val track = getLibraryColor("mc_track")
                val tick = getLibraryColor("mc_tick")
                binding.paletteColorBg.applySimpleGradient(
                    track.toMiddleColor(),
                    tick.toMiddleColor()
                )
            }
        }

        nowPlayerViewModel.marqueeEnabled.collectWithLifecycle(this) { enabled ->
            binding.songName.isSelected = enabled
        }

        viewModel.currentSong.collectWithLifecycle(this) { song ->
            if (song != null) {
                if ((song.id != lastSongId) || !isTransitionStarted) {
                    lastSongId = song.id
                    updateSongUI(song)
                    loadWaveform(song.id ?: "", song.path ?: "")
                }
            } else {
                if (!isTransitionStarted) {
                    isTransitionStarted = true
                    startPostponedEnterTransition()
                }
            }
        }

        musicViewModel.event.collectWithLifecycle(this) { event ->
            if (event is MusicEvent.PlaySong) {
                mediaController?.let { controller ->
                    forcePlaySong(controller, event.position, event.keepProgress)
                }
            }
        }
    }


    private fun applyCurrentModeColors() {
        val colors = getCurrentThemeColors(currentMode, currentDominantColor)
        updatePlayerUIColors(colors.first, colors.second)

        // Always ensure the palette button shows the extracted colors
        binding.paletteColorBg.applySimpleGradient(
            currentDominantColor.first.toMiddleColor(),
            currentDominantColor.second.toMiddleColor()
        )
    }

    private fun updateSongUI(song: MusicFiles) {
        if ((binding.songName.text == song.title) && (binding.songArtist.text == song.artist) && isTransitionStarted) {
            return
        }
        updateSongJob?.cancel()

        animateTextChange(binding.songName, song.title ?: getString(R.string.unknown))
        animateTextChange(binding.songArtist, song.artist ?: getString(R.string.unknown))
        animateTextChange(binding.durationTotal, song.durationDuration.formatAsTime())

        updateSongJob = lifecycleScope.launch {
            launch(Dispatchers.IO) {
                val bitrate = getBitrate(song.path)
                withContext(Dispatchers.Main) {
                    binding.bitrate.text =
                        bitrate?.let { getString(R.string.kbps_format, it) } ?: ""
                    binding.bitrate.isVisible = bitrate != null
                }
            }

            launch {
                val bitmap = withContext(Dispatchers.Default) {
                    getSongArtwork(song.albumId, song.path, song.cachedImagePath, song.album, 600)
                }
                applyArtworkAndPalette(bitmap, song)
            }
        }
    }

    private fun applyArtworkAndPalette(bitmap: android.graphics.Bitmap?, song: MusicFiles) {
        var imageLoaded = false
        var blurLoaded = false

        val checkReady = {
            if (imageLoaded && blurLoaded && !isTransitionStarted) {
                isTransitionStarted = true
                startPostponedEnterTransition()
            }
        }

        binding.image.loadBitmap(bitmap) {
            imageLoaded = true
            checkReady()
        }

        binding.imageBlur.loadBitmap(
            bitmap, blurRadius = 100f, fallback = R.drawable.ic_cover_song_blur
        ) {
            blurLoaded = true
            checkReady()
        }

        // Extract Palette
        if (bitmap != null) {
            val track = getLibraryColor("mc_track")
            val tick = getLibraryColor("mc_tick")

            if (song.dominantColor != null && song.vibrantColor != null) {
                // Use cached colors from Room
                val colors = Pair(song.vibrantColor, song.dominantColor)
                currentDominantColor = colors
                binding.paletteColorBg.applySimpleGradient(
                    colors.first.toMiddleColor(),
                    colors.second.toMiddleColor()
                )
                nowPlayerViewModel.updateThemeColor(song.id, colors.first, colors.second)
                if (currentMode == DATA.MODE_PALETTE) applyCurrentModeColors()
            } else if (nowPlayerViewModel.colorSongId.value != song.id) {
                // Extract and save
                Palette.from(bitmap).generate { palette ->
                    val colors = palette.extractDynamicColors(track, tick)
                    currentDominantColor = colors

                    binding.paletteColorBg.applySimpleGradient(
                        colors.first.toMiddleColor(),
                        colors.second.toMiddleColor()
                    )
                    nowPlayerViewModel.updateThemeColor(song.id, colors.first, colors.second)
                    
                    // Save to Room
                    song.id?.let { id ->
                        nowPlayerViewModel.updateSongColors(id, colors.second, colors.first)
                    }

                    if (currentMode == DATA.MODE_PALETTE) {
                        applyCurrentModeColors()
                    }
                }
            }
        } else {
            val track = getLibraryColor("mc_track")
            val tick = getLibraryColor("mc_tick")
            currentDominantColor = Pair(track, tick)
            binding.paletteColorBg.applySimpleGradient(
                track.toMiddleColor(),
                tick.toMiddleColor()
            )
            nowPlayerViewModel.updateThemeColor(null, track, tick)
            if (currentMode == DATA.MODE_PALETTE) applyCurrentModeColors()
        }
    }

    private fun animateTextChange(textView: TextView, newText: String) {
        if (textView.text == newText) return
        val duration = resources.getInteger(R.integer.anim_duration_short).toLong()
        textView.animate().alpha(0f).setDuration(duration).withEndAction {
            textView.text = newText
            textView.isSelected = true
            textView.animate().alpha(1f).setDuration(duration).start()
        }.start()
    }

    private fun getBitrate(path: String?): String? {
        if (path == null) return null
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            retriever.release()
            bitrate?.let { (it.toInt() / 1000).toString() }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadWaveform(songId: String, path: String) {
        if (songId.isEmpty() || path.isEmpty()) return
        waveformJob?.cancel()
        waveformJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(500.milliseconds)
            try {
                val cachedSong = viewModel.getSongById(songId)
                if (cachedSong?.waveform != null) {
                    val amplitudes =
                        cachedSong.waveform.split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
                    if (isActive && amplitudes.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            binding.waveformSeekBar.setSampleFrom(amplitudes)
                        }
                        return@launch
                    }
                }

                amplituda.processAudio(path).get({ result ->
                    val amplitudesArray = result.amplitudesAsList().toIntArray()
                    if (amplitudesArray.isNotEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val waveformString = amplitudesArray.joinToString(",")
                            viewModel.updateWaveform(songId, waveformString)
                        }
                    }
                    if (isActive) {
                        runOnUiThread {
                            binding.waveformSeekBar.setSampleFrom(amplitudesArray)
                        }
                    }
                }, { error ->
                    error.printStackTrace()
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getIntentMethod() {
        val position = intent.getIntExtra(DATA.POSITION, -1)

        if ((position != -1) && (!isIntentProcessed)) {
            binding.playPause.setImageResource(R.drawable.ic_pause)
            viewModel.updatePositionAndSong(position)
        }
    }

    private fun playPauseBtn() {
        mediaController?.togglePlayPause(
            binding.playPause,
            { stopProgressUpdater() },
        ) { startProgressUpdater() }
    }

    private fun prevBtn(animate: Boolean = true, forceIndex: Int = -1) {
        if (animate) {
            animateSkip(toNext = false)
            return
        }
        mediaController?.let { controller ->
            if (forceIndex != -1) {
                controller.seekToDefaultPosition(forceIndex)
            } else {
                controller.seekToPreviousMediaItem()
            }
            if (!controller.playWhenReady) {
                controller.play()
            }
        }
    }

    private fun nextBtn(animate: Boolean = true, forceIndex: Int = -1) {
        if (animate) {
            animateSkip(toNext = true)
            return
        }
        mediaController?.let { controller ->
            if (forceIndex != -1) {
                controller.seekToDefaultPosition(forceIndex)
            } else {
                controller.seekToNextMediaItem()
            }
            if (!controller.playWhenReady) {
                controller.play()
            }
        }
    }

    private fun animateSkip(toNext: Boolean) {
        if (isAnimating || (mediaController == null)) return

        val controller = mediaController!!
        val itemCount = controller.mediaItemCount
        if (itemCount <= 0) return

        isAnimating = true

        lifecycleScope.launch {
            // 0. Wait 500ms as requested before starting the transition
            delay(500.milliseconds)

            var targetIndex =
                if (toNext) controller.nextMediaItemIndex else controller.previousMediaItemIndex

            if (targetIndex == -1) {
                targetIndex = if (toNext) 0 else itemCount - 1
            }

            val nextSong =
                if (targetIndex in viewModel.listSongs.indices) viewModel.listSongs[targetIndex] else null

            val width = binding.card.width.toFloat()
            val outX = if (toNext) -width else width
            val inX = if (toNext) width else -width

            var animationDone = false
            var dataReady = false

            val onReady = {
                if (animationDone && dataReady) {
                    performSkipAndSlideIn(toNext, inX, targetIndex, nextSong)
                }
            }

            // 1. Start Out Animation
            binding.card.animate().translationX(outX).alpha(0f)
                .setDuration(resources.getInteger(R.integer.anim_duration_skip).toLong())
                .setInterpolator(AccelerateInterpolator()).withEndAction {
                    animationDone = true
                    onReady()
                }.start()

            // 2. Pre-load Data in parallel
            if (nextSong != null) {
                val model = getSongImageModel(
                    nextSong.albumId, nextSong.path, nextSong.cachedImagePath, nextSong.album
                )

                // Pre-load Bitmap and Palette
                val request = ImageRequest.Builder(this@PlayerActivity).data(model)
                    .allowHardware(enable = false).precision(coil.size.Precision.INEXACT).size(600)
                    .listener(onError = { _, _ ->
                        val track = getLibraryColor("mc_track")
                        val tick = getLibraryColor("mc_tick")
                        currentDominantColor = Pair(track, tick)
                        preloadedBitmap = null
                        dataReady = true
                        onReady()
                    }, onCancel = {
                        val track = getLibraryColor("mc_track")
                        val tick = getLibraryColor("mc_tick")
                        currentDominantColor = Pair(track, tick)
                        preloadedBitmap = null
                        dataReady = true
                        onReady()
                    }).target { result ->
                        val bitmap = (result as? BitmapDrawable)?.bitmap
                        preloadedBitmap = bitmap

                        if (bitmap != null) {
                            Palette.from(bitmap).generate { palette ->
                                val track = getLibraryColor("mc_track")
                                val tick = getLibraryColor("mc_tick")
                                currentDominantColor = palette.extractDynamicColors(track, tick)
                                nowPlayerViewModel.updateThemeColor(nextSong.id, currentDominantColor.first, currentDominantColor.second)
                                dataReady = true
                                onReady()
                            }
                        } else {
                            val track = getLibraryColor("mc_track")
                            val tick = getLibraryColor("mc_tick")
                            currentDominantColor = Pair(track, tick)
                            dataReady = true
                            onReady()
                        }
                    }.build()
                imageLoader.enqueue(request)
            } else {
                val track = getLibraryColor("mc_track")
                val tick = getLibraryColor("mc_tick")
                currentDominantColor = Pair(track, tick)
                preloadedBitmap = null
                dataReady = true
                onReady()
            }
        }
    }

    private fun performSkipAndSlideIn(
        toNext: Boolean, inX: Float, targetIndex: Int, nextSong: MusicFiles?
    ) {
        // Update UI manually before sliding in to ensure it's ready
        nextSong?.let { song ->
            lastSongId = song.id
            binding.songName.text = song.title ?: getString(R.string.unknown)
            binding.songArtist.text = song.artist ?: getString(R.string.unknown)
            binding.durationTotal.text = song.durationDuration.formatAsTime()

            // Apply preloaded bitmap immediately
            preloadedBitmap?.let { bmp ->
                binding.image.loadBitmap(bmp, crossfade = false)
                binding.imageBlur.loadBitmap(bmp, blurRadius = 100f, crossfade = false)
            } ?: run {
                binding.image.setImageResource(R.drawable.ic_cover_song)
                binding.imageBlur.setImageResource(R.drawable.ic_cover_song_blur)
            }

            applyCurrentModeColors()
            loadWaveform(song.id ?: "", song.path ?: "")

            // Update bitrate in background
            lifecycleScope.launch(Dispatchers.IO) {
                val bitrate = getBitrate(song.path)
                withContext(Dispatchers.Main) {
                    binding.bitrate.text =
                        bitrate?.let { getString(R.string.kbps_format, it) } ?: ""
                    binding.bitrate.isVisible = bitrate != null
                }
            }
        }

        if (toNext) nextBtn(animate = false, forceIndex = targetIndex)
        else prevBtn(animate = false, forceIndex = targetIndex)

        binding.card.translationX = inX
        binding.card.animate().translationX(0f).alpha(1f)
            .setDuration(resources.getInteger(R.integer.anim_duration_skip).toLong())
            .setInterpolator(DecelerateInterpolator()).withEndAction {
                isAnimating = false
                preloadedBitmap = null // Clear after use
            }.start()
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            if (abs(diffX) > abs(diffY)) {
                if ((abs(diffX) > swipeThreshold) && (abs(velocityX) > swipeVelocityThreshold)) {
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
                        if (binding.seekBar.max != (duration.toInt() / 1000)) {
                            binding.seekBar.max = duration.toInt() / 1000
                        }

                        if ((binding.durationTotal.text == "0:00") || (binding.durationTotal.text == DATA.UNKNOWN)) {
                            binding.durationTotal.text = duration.milliseconds.formatAsTime()
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

            if ((intentPosition != -1) && (!isIntentProcessed)) {
                isIntentProcessed = true
                intent.removeExtra(DATA.POSITION)

                val currentItem = controller.currentMediaItem
                val targetSong = viewModel.listSongs.getOrNull(intentPosition)
                val isAlreadyPlaying =
                    currentItem != null && controller.currentMediaItemIndex == intentPosition && currentItem.mediaId == targetSong?.id

                if (isAlreadyPlaying) {
                    viewModel.updatePositionAndSong(intentPosition)
                } else {
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
                }
            } else {
                val index = controller.currentMediaItemIndex
                if (index != -1 && index in viewModel.listSongs.indices) {
                    if (viewModel.position != index) {
                        viewModel.updatePositionAndSong(index)
                    }
                } else if (controller.currentMediaItem == null && viewModel.position != -1) {
                    setupMediaItems(controller)
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

    private fun forcePlaySong(
        controller: MediaController, pos: Int, keepProgress: Boolean = false
    ) {
        val currentProgress = if (keepProgress) controller.currentPosition else 0L
        viewModel.updatePositionAndSong(pos, forceUpdate = true)

        val mediaItems: List<MediaItem> = viewModel.listSongs.map { song ->
            val uri = song.path?.toUri() ?: "".toUri()
            val metadata =
                MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setExtras(
                    Bundle().apply {
                        putString("ALBUM_ID", song.albumId)
                        putString("CACHED_IMAGE_PATH", song.cachedImagePath)
                    },
                ).build()
            MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).setMediaId(song.id ?: "")
                .build()
        }

        controller.setMediaItems(mediaItems, pos, currentProgress)
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
            val metadata =
                MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setExtras(
                    Bundle().apply {
                        putString("ALBUM_ID", song.albumId)
                        putString("CACHED_IMAGE_PATH", song.cachedImagePath)
                    },
                ).build()
            MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).setMediaId(song.id ?: "")
                .build()
        }
        controller.setMediaItems(mediaItems, viewModel.position, viewModel.lastProgress)
        controller.prepare()
    }

    override fun onStop() {
        super.onStop()
        mediaController?.removeListener(this)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        stopProgressUpdater()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("intent_processed", isIntentProcessed)
    }

    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        binding.waveformSeekBar.alpha = when (deviceInfo.playbackType) {
            DeviceInfo.PLAYBACK_TYPE_LOCAL -> 1f
            DeviceInfo.PLAYBACK_TYPE_REMOTE -> 0.5f
            else -> binding.waveformSeekBar.alpha
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            binding.playPause.setImageResource(R.drawable.ic_pause)
        } else {
            binding.playPause.setImageResource(R.drawable.ic_play)
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
                Player.EVENT_REPEAT_MODE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            )
        ) {
            updateRepeatShuffleIcons(player)
        }
    }

    private fun updateRepeatShuffleIcons(player: Player) {
        val cycleIcon = when {
            player.shuffleModeEnabled -> R.drawable.ic_shuffle_on
            player.repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
            else -> R.drawable.ic_repeat
        }
        binding.repeat.setImageResource(cycleIcon)
    }
}