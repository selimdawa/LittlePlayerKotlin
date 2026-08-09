package com.flatcode.littleplayer.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
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
import com.flatcode.littleplayer.utils.ensureBrightColor
import com.flatcode.littleplayer.utils.extractPalette
import com.flatcode.littleplayer.utils.extractVibrantColor
import com.flatcode.littleplayer.utils.formatAsTime
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.getSongImageModel
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.onProgressChanged
import com.flatcode.littleplayer.utils.setGradientBackground
import com.flatcode.littleplayer.utils.setSolidBackground
import com.flatcode.littleplayer.utils.togglePlayPause
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
import kotlin.math.abs
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
    private var currentDominantColor: Int = Color.BLACK // Will be initialized in onCreate
    private var currentMode: Int = DATA.MODE_BASIC
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
        currentDominantColor = getLibraryColor("mc_track")

        getIntentMethod()
        setupListeners()
        observeViewModel()
    }

    private fun updatePlayerUIColors(color: Int) {
        val brightColor = color.ensureBrightColor()
        val colorStateList = ColorStateList.valueOf(brightColor)
        val backgroundColorStateList =
            ColorStateList.valueOf("#66FFFFFF".toColorInt()) // 40% White for better visibility

        binding.seekBar.progressTintList = colorStateList
        binding.seekBar.thumbTintList = colorStateList
        binding.seekBar.secondaryProgressTintList = colorStateList
        binding.seekBar.progressBackgroundTintList = backgroundColorStateList

        if (currentMode == DATA.MODE_BASIC) {
            binding.playPauseBtn.setGradientBackground(
                getLibraryColor("mc_track"),
                getLibraryColor("mc_tick"),
            )
        } else {
            binding.playPauseBtn.setSolidBackground(brightColor)
        }

        binding.waveformSeekBar.waveProgressColor = brightColor
        binding.waveformSeekBar.waveBackgroundColor = "#4DFFFFFF".toColorInt() // 30% White

        binding.basicColor.strokeWidth = if (currentMode == DATA.MODE_BASIC) 4 else 1
        binding.paletteColor.strokeWidth = if (currentMode == DATA.MODE_PALETTE) 4 else 1
        binding.whiteColor.strokeWidth = if (currentMode == DATA.MODE_WHITE) 4 else 1
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
            mediaController?.let { controller ->
                when {
                    (!controller.shuffleModeEnabled) && (controller.repeatMode != Player.REPEAT_MODE_ONE) -> {
                        controller.repeatMode = Player.REPEAT_MODE_ONE
                        controller.shuffleModeEnabled = false
                    }

                    (!controller.shuffleModeEnabled) && (controller.repeatMode == Player.REPEAT_MODE_ONE) -> {
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

        binding.prev.setOnClickListener { prevBtn() }
        binding.next.setOnClickListener { nextBtn() }
        binding.playPauseBtn.setOnClickListener { playPauseBtn() }
        binding.favorite.setOnClickListener { viewModel.toggleFavorite() }

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

        nowPlayerViewModel.currentThemeColor.collectWithLifecycle(this) { color ->
            color?.let {
                currentDominantColor = it
                binding.paletteColor.setCardBackgroundColor(it.ensureBrightColor())
                if (currentMode == DATA.MODE_PALETTE) {
                    applyCurrentModeColors()
                }
            }
        }

        nowPlayerViewModel.marqueeEnabled.collectWithLifecycle(this) { enabled ->
            binding.songName.isSelected = enabled
        }

        viewModel.currentSong.collectWithLifecycle(this) { song ->
            song?.let {
                updateSongUI(it)
                lifecycleScope.launch {
                    delay(300.milliseconds)
                    it.path?.let { path -> it.id?.let { id -> loadWaveform(id, path) } }
                }
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
        animateTextChange(binding.songName, song.title ?: DATA.UNKNOWN)
        animateTextChange(binding.songArtist, song.artist ?: DATA.UNKNOWN)
        animateTextChange(binding.durationTotal, song.durationDuration.formatAsTime())

        binding.image.loadSongImage(song.albumId, song.path, song.cachedImagePath)
        binding.imageBlur.loadSongImageBlur(song.albumId, 100, song.path, song.cachedImagePath)

        val model = getSongImageModel(song.albumId, song.path, song.cachedImagePath)

        extractPalette(model) { palette ->
            val defaultColor = getLibraryColor("mc_track")
            currentDominantColor = palette.extractVibrantColor(defaultColor)

            binding.paletteColor.setCardBackgroundColor(currentDominantColor.ensureBrightColor())
            nowPlayerViewModel.updateThemeColor(currentDominantColor)

            if (currentMode == DATA.MODE_PALETTE) {
                applyCurrentModeColors()
            }
        }
    }

    private fun animateTextChange(textView: TextView, newText: String) {
        if (textView.text == newText) return
        textView.animate().alpha(0f).setDuration(150).withEndAction {
            textView.text = newText
            textView.animate().alpha(1f).setDuration(150).start()
        }.start()
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

    private fun prevBtn(animate: Boolean = true) {
        if (animate) {
            animateSkip(toNext = false)
            return
        }
        mediaController?.let { controller ->
            val count = controller.mediaItemCount
            if (count > 0) {
                val prevIndex = ((controller.currentMediaItemIndex - 1) + count) % count
                controller.seekToDefaultPosition(prevIndex)
                if (!controller.playWhenReady) {
                    controller.play()
                }
            }
        }
    }

    private fun nextBtn(animate: Boolean = true) {
        if (animate) {
            animateSkip(toNext = true)
            return
        }
        mediaController?.let { controller ->
            val count = controller.mediaItemCount
            if (count > 0) {
                val nextIndex = (controller.currentMediaItemIndex + 1) % count
                controller.seekToDefaultPosition(nextIndex)
                if (!controller.playWhenReady) {
                    controller.play()
                }
            }
        }
    }

    private fun animateSkip(toNext: Boolean) {
        if (isAnimating || (mediaController == null)) return

        val controller = mediaController!!
        val itemCount = controller.mediaItemCount
        if (itemCount <= 0) return

        isAnimating = true
        val nextIndex =
            if (toNext) controller.nextMediaItemIndex else controller.previousMediaItemIndex

        val nextSong =
            if (nextIndex in viewModel.listSongs.indices) viewModel.listSongs[nextIndex] else null

        nextSong?.let {
            binding.songName.text = it.title ?: DATA.UNKNOWN
            binding.songArtist.text = it.artist ?: DATA.UNKNOWN
        }

        val width = binding.card.width.toFloat()
        val outX = if (toNext) -width else width
        val inX = if (toNext) width else -width

        binding.card.animate().translationX(outX).alpha(0f).setDuration(200)
            .setInterpolator(AccelerateInterpolator()).withEndAction {
                if (nextSong != null) {
                    val model =
                        getSongImageModel(nextSong.albumId, nextSong.path, nextSong.cachedImagePath)
                    val request =
                        ImageRequest.Builder(this).data(model).allowHardware(enable = false)
                            .listener(
                                onError = { _, _ ->
                                    currentDominantColor = getLibraryColor("mc_track")
                                    performSkipAndSlideIn(toNext, inX)
                                },
                                onCancel = {
                                    currentDominantColor = getLibraryColor("mc_track")
                                    performSkipAndSlideIn(toNext, inX)
                                },
                            ).target { result ->
                                val bitmap = if (result is BitmapDrawable) {
                                    result.bitmap
                                } else {
                                    val width =
                                        if (result.intrinsicWidth > 0) result.intrinsicWidth else 200
                                    val height =
                                        if (result.intrinsicHeight > 0) result.intrinsicHeight else 200
                                    val bmp = createBitmap(width, height)
                                    val canvas = Canvas(bmp)
                                    result.setBounds(0, 0, canvas.width, canvas.height)
                                    result.draw(canvas)
                                    bmp
                                }

                                Palette.from(bitmap).generate { palette ->
                                    val defaultColor = getLibraryColor("mc_track")
                                    currentDominantColor = palette.extractVibrantColor(defaultColor)
                                    performSkipAndSlideIn(toNext, inX)
                                }
                            }.build()
                    imageLoader.enqueue(request)
                } else {
                    currentDominantColor = getLibraryColor("mc_track")
                    performSkipAndSlideIn(toNext, inX)
                }
            }.start()
    }

    private fun performSkipAndSlideIn(toNext: Boolean, inX: Float) {
        if (toNext) nextBtn(animate = false) else prevBtn(animate = false)
        binding.card.translationX = inX
        binding.card.animate().translationX(0f).alpha(1f).setDuration(200)
            .setInterpolator(DecelerateInterpolator()).withEndAction { isAnimating = false }.start()
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

                        if (binding.durationTotal.text == "0:00" || binding.durationTotal.text == DATA.UNKNOWN) {
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
                    } else {
                        lifecycleScope.launch {
                            while (viewModel.listSongs.isEmpty()) {
                                delay(30.milliseconds)
                            }
                            if (index in viewModel.listSongs.indices) {
                                viewModel.updatePositionAndSong(index)
                            }
                        }
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
            else -> R.drawable.ic_repeat_on
        }
        binding.repeat.setImageResource(cycleIcon)
    }
}