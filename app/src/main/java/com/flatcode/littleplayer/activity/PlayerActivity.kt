package com.flatcode.littleplayer.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.imageLoader
import coil.request.ImageRequest
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityPlayerBinding
import com.flatcode.littleplayer.fragment.PlayerOptionsBottomSheet
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.*
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlayerViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.linc.amplituda.Amplituda
import dagger.hilt.android.AndroidEntryPoint
import io.selimdawa.multicolors.MultiColorManager
import io.selimdawa.multicolors.R as MultiColorR
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
class PlayerActivity : BaseActivity<ActivityPlayerBinding>(ActivityPlayerBinding::inflate), Player.Listener {

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
    private var lastArtworkSongId: String? = null
    private var isProcessingShuffle = false
    private var preloadedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        isIntentProcessed = savedInstanceState?.getBoolean("intent_processed") ?: false
        postponeEnterTransition()
        super.onCreate(savedInstanceState)
    }

    override fun applyInitialTheme() {
        currentMode = ThemeManager.currentMode
        currentDominantColor = ThemeManager.currentColors ?: Pair(getLibraryColor(MultiColorR.attr.mc_track), getLibraryColor(MultiColorR.attr.mc_tick))

        val colors = getCurrentThemeColors(currentMode, currentDominantColor)
        updatePlayerUIColors(colors.first, colors.second)

        val paletteColors = getCurrentThemeColors(DATA.MODE_PALETTE, currentDominantColor)
        binding.paletteColorBg.applySimpleGradient(paletteColors.first, paletteColors.second)
    }

    override fun setupViews() {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        applyEdgeToEdge(topView = binding.toolbar, bottomView = binding.container)

        amplituda = Amplituda(this)
        currentDominantColor = Pair(getLibraryColor(MultiColorR.attr.mc_track), getLibraryColor(MultiColorR.attr.mc_tick))

        getIntentMethod()
        setupListeners()
    }

    private fun updatePlayerUIColors(startColor: Int, endColor: Int) {
        val colorStateList = ColorStateList.valueOf(startColor)
        val backgroundColorStateList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white_66))

        binding.seekBar.apply {
            progressTintList = colorStateList
            thumbTintList = colorStateList
            secondaryProgressTintList = colorStateList
            progressBackgroundTintList = backgroundColorStateList
        }

        val trackColor = if (currentMode == DATA.MODE_BASIC) getLibraryColor(MultiColorR.attr.mc_track) else startColor
        val tickColor = if (currentMode == DATA.MODE_BASIC) getLibraryColor(MultiColorR.attr.mc_tick) else endColor
        
        binding.playPauseBtn.setHaloBackground(trackColor, tickColor)
        binding.imageBorder.setHaloBackground(trackColor, tickColor)

        binding.waveformSeekBar.waveProgressColor = startColor
        binding.waveformSeekBar.waveBackgroundColor = ContextCompat.getColor(this, R.color.white_30)

        binding.basicColor.strokeWidth = if (currentMode == DATA.MODE_BASIC) resources.getDimensionPixelSize(R.dimen.stroke_width_active) else resources.getDimensionPixelSize(R.dimen.stroke_width_inactive)
        binding.paletteColor.strokeWidth = if (currentMode == DATA.MODE_PALETTE) resources.getDimensionPixelSize(R.dimen.stroke_width_active) else resources.getDimensionPixelSize(R.dimen.stroke_width_inactive)
        binding.whiteColor.strokeWidth = if (currentMode == DATA.MODE_WHITE) resources.getDimensionPixelSize(R.dimen.stroke_width_active) else resources.getDimensionPixelSize(R.dimen.stroke_width_inactive)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.back.setOnClickListener { supportFinishAfterTransition() }

        binding.basicColor.setOnClickListener { nowPlayerViewModel.setThemeColorMode(DATA.MODE_BASIC) }
        binding.paletteColor.setOnClickListener { nowPlayerViewModel.setThemeColorMode(DATA.MODE_PALETTE) }
        binding.whiteColor.setOnClickListener { nowPlayerViewModel.setThemeColorMode(DATA.MODE_WHITE) }

        binding.seekBar.onProgressChanged { progress, fromUser ->
            if (mediaController != null && fromUser) {
                mediaController?.seekTo(progress.toLong() * 1000)
                mediaController?.duration?.let { if (it > 0) {
                    if (binding.waveformSeekBar.sample != null) {
                        binding.waveformSeekBar.progress = (progress * 1000f / it) * 100f
                    }
                } }
            }
        }

        binding.repeat.onClickPulse {
            if (isProcessingShuffle) return@onClickPulse
            isProcessingShuffle = true
            mediaController?.let { controller ->
                val isShuffle = musicViewModel.shuffleMode.value
                val repeat = controller.repeatMode
                val (nextRepeat, nextShuffle) = when {
                    !isShuffle && repeat != Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE to false
                    !isShuffle && repeat == Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL to true
                    else -> Player.REPEAT_MODE_ALL to false
                }
                controller.repeatMode = nextRepeat
                controller.shuffleModeEnabled = nextShuffle
                musicViewModel.saveShuffleMode(nextShuffle)
                updateRepeatShuffleIcons(nextRepeat, nextShuffle)
            }
            binding.root.postDelayed({ isProcessingShuffle = false }, 500)
        }

        binding.prev.onClickPulse { prevBtn() }
        binding.next.onClickPulse { nextBtn() }
        binding.playPauseBtn.onClickPulse { playPauseBtn() }
        binding.favorite.onClickPulse { viewModel.toggleFavorite() }

        binding.moreOptions.setOnClickListener {
            PlayerOptionsBottomSheet(viewModel.currentSong.value, mediaController, { musicViewModel.deleteSong(it); nextBtn(animate = false) }, {}).show(supportFragmentManager, "PlayerOptions")
        }

        val gestureDetector = GestureDetector(this, SwipeGestureListener())
        binding.card.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) true else {
                if (event.action == MotionEvent.ACTION_UP) v.performClick()
                false
            }
        }
    }

    override fun observeViewModel() {
        viewModel.isFavorite.collectWithLifecycle(this) { binding.favorite.setImageResource(if (it) R.drawable.ic_favorite else R.drawable.ic_favorite_border) }
        nowPlayerViewModel.themeColorMode.collectWithLifecycle(this) { currentMode = it; applyCurrentModeColors() }
        
        lifecycleScope.launch {
            MultiColorManager.currentThemeId.collect {
                MultiColorManager.applyTheme(this@PlayerActivity)
                applyCurrentModeColors()
            }
        }

        nowPlayerViewModel.currentThemeColor.collectWithLifecycle(this) { colorPair ->
            currentDominantColor = colorPair ?: Pair(0, 0)
            val displayColors = getCurrentThemeColors(DATA.MODE_PALETTE, colorPair)
            binding.paletteColorBg.applySimpleGradient(displayColors.first, displayColors.second)
            if (currentMode == DATA.MODE_PALETTE) applyCurrentModeColors()
        }

        nowPlayerViewModel.marqueeEnabled.collectWithLifecycle(this) { binding.songName.isSelected = it }
        musicViewModel.shuffleMode.collectWithLifecycle(this) { _ -> mediaController?.let { updateRepeatShuffleIcons(it) } }

        viewModel.currentSong.collectWithLifecycle(this) { song ->
            if (song != null) {
                if (song.id != lastSongId || !isTransitionStarted) {
                    lastSongId = song.id
                    updateSongUI(song)
                    loadWaveform(song.id ?: "", song.path ?: "")
                }
            } else if (!isTransitionStarted) {
                isTransitionStarted = true
                startPostponedEnterTransition()
            }
        }

        musicViewModel.event.collectWithLifecycle(this) { event ->
            if (event is MusicEvent.PlaySong) mediaController?.let { forcePlaySong(it, event.position, event.keepProgress) }
        }
    }

    private fun applyCurrentModeColors() {
        val colors = getCurrentThemeColors(currentMode, currentDominantColor)
        updatePlayerUIColors(colors.first, colors.second)
        viewModel.currentSong.value?.let { song ->
            if (lastArtworkSongId != song.id) {
                lastArtworkSongId = song.id
                lifecycleScope.launch {
                    val bitmap = withContext(Dispatchers.Default) {
                        getSongArtwork(
                            song.albumId,
                            song.path,
                            song.cachedImagePath,
                            song.album,
                            600
                        )
                    }
                    applyArtworkAndPalette(bitmap)
                }
            }
        }
        val paletteColors = getCurrentThemeColors(DATA.MODE_PALETTE, currentDominantColor)
        binding.paletteColorBg.applySimpleGradient(paletteColors.first, paletteColors.second)
    }

    private fun updateSongUI(song: MusicFiles) {
        if (binding.songName.text == song.title && binding.songArtist.text == song.artist && isTransitionStarted) return
        updateSongJob?.cancel()
        binding.songName.fadeText(song.title ?: getString(R.string.unknown))
        binding.songArtist.fadeText(song.artist ?: getString(R.string.unknown))
        binding.durationTotal.fadeText(song.durationDuration.formatAsTime())

        updateSongJob = lifecycleScope.launch {
            launch(Dispatchers.IO) {
                val bitrate = getBitrate(song.path)
                withContext(Dispatchers.Main) {
                    binding.bitrate.text = bitrate?.let { getString(R.string.kbps_format, it) } ?: ""
                    binding.bitrate.isVisible = bitrate != null
                }
            }
            launch {
                val bitmap = withContext(Dispatchers.Default) {
                    getSongArtwork(
                        song.albumId,
                        song.path,
                        song.cachedImagePath,
                        song.album,
                        600
                    )
                }
                lastArtworkSongId = song.id
                applyArtworkAndPalette(bitmap)
            }
        }
    }

    private fun applyArtworkAndPalette(bitmap: Bitmap?) {
        var imageLoaded = false
        var blurLoaded = false
        val checkReady = { if (imageLoaded && blurLoaded && !isTransitionStarted) { isTransitionStarted = true; startPostponedEnterTransition() } }

        binding.image.loadBitmap(bitmap) { imageLoaded = true; checkReady() }
        binding.imageBlur.loadBitmap(bitmap, blurRadius = 100f, fallback = R.drawable.ic_cover_song_blur) { blurLoaded = true; checkReady() }
    }

    private fun getBitrate(path: String?): String? {
        if (path == null) return null
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            retriever.release()
            bitrate?.let { (it.toInt() / 1000).toString() }
        } catch (_: Exception) { null }
    }

    private fun loadWaveform(songId: String, path: String) {
        if (songId.isEmpty() || path.isEmpty()) return
        waveformJob?.cancel()
        waveformJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(500.milliseconds)
            try {
                val cachedSong = viewModel.getSongById(songId)
                if (cachedSong?.waveform != null) {
                    val amplitudes = cachedSong.waveform.split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
                    if (isActive && amplitudes.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (!isFinishing) {
                                binding.waveformSeekBar.setSampleFrom(amplitudes)
                                updateWaveformProgress()
                            }
                        }
                        return@launch
                    }
                }
                
                // Process audio with Amplituda
                amplituda.processAudio(path).get({ result ->
                    // Check if this result is still for the current song
                    if (viewModel.currentSong.value?.id != songId) return@get
                    
                    val amplitudesArray = result.amplitudesAsList().toIntArray()
                    if (amplitudesArray.isNotEmpty()) {
                        val downsampled = downsampleAmplitudes(amplitudesArray)
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.updateWaveform(songId, downsampled.joinToString(","))
                        }
                        runOnUiThread {
                            if (!isFinishing && viewModel.currentSong.value?.id == songId) {
                                binding.waveformSeekBar.setSampleFrom(downsampled)
                                updateWaveformProgress()
                            }
                        }
                    }
                }, { e ->
                    e.printStackTrace()
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun downsampleAmplitudes(amplitudes: IntArray, targetSize: Int = 1000): IntArray {
        if (amplitudes.size <= targetSize) return amplitudes
        val result = IntArray(targetSize)
        val sectorSize = amplitudes.size.toFloat() / targetSize
        for (i in 0 until targetSize) {
            val start = (i * sectorSize).toInt()
            val end = ((i + 1) * sectorSize).toInt().coerceAtMost(amplitudes.size)
            var max = 0
            for (j in start until end) {
                if (amplitudes[j] > max) max = amplitudes[j]
            }
            result[i] = max
        }
        return result
    }

    private fun updateWaveformProgress() {
        mediaController?.let { controller ->
            val duration = controller.duration
            if (duration > 0 && binding.waveformSeekBar.sample != null) {
                binding.waveformSeekBar.progress = (controller.currentPosition.toFloat() / duration.toFloat()) * 100
            }
        }
    }

    private fun getIntentMethod() {
        val position = intent.getIntExtra(DATA.POSITION, -1)
        if (position != -1 && !isIntentProcessed) {
            binding.playPause.setImageResource(R.drawable.ic_pause)
            viewModel.updatePositionAndSong(position)
        }
    }

    private fun playPauseBtn() {
        mediaController?.togglePlayPause(binding.playPause, { stopProgressUpdater() }) { startProgressUpdater() }
    }

    private fun prevBtn(animate: Boolean = true, forceIndex: Int = -1) {
        if (animate) { animateSkip(toNext = false); return }
        mediaController?.let { if (forceIndex != -1) it.seekToDefaultPosition(forceIndex) else it.skipToPreviousSafe() }
    }

    private fun nextBtn(animate: Boolean = true, forceIndex: Int = -1) {
        if (animate) { animateSkip(toNext = true); return }
        mediaController?.let { if (forceIndex != -1) it.seekToDefaultPosition(forceIndex) else it.skipToNextSafe() }
    }

    private fun animateSkip(toNext: Boolean) {
        if (isAnimating || mediaController == null) return
        val controller = mediaController!!
        val itemCount = controller.mediaItemCount
        if (itemCount <= 0) return
        isAnimating = true
        lifecycleScope.launch {
            delay(500.milliseconds)
            var targetIndex = if (toNext) controller.nextMediaItemIndex else controller.previousMediaItemIndex
            if (targetIndex == -1) targetIndex = if (toNext) 0 else itemCount - 1
            val nextSong = viewModel.listSongs.getOrNull(targetIndex)
            val width = binding.card.width.toFloat()
            val outX = if (toNext) -width else width
            val inX = if (toNext) width else -width
            var animationDone = false
            var dataReady = false
            val onReady = { if (animationDone && dataReady) performSkipAndSlideIn(toNext, inX, targetIndex, nextSong) }

            binding.card.animate().translationX(outX).alpha(0f).setDuration(resources.getInteger(R.integer.anim_duration_skip).toLong()).withEndAction { animationDone = true; onReady() }.start()

            if (nextSong != null) {
                val request = ImageRequest.Builder(this@PlayerActivity).data(getSongImageModel(nextSong.albumId, nextSong.path, nextSong.cachedImagePath, nextSong.album)).allowHardware(false).size(600)
                    .listener(onSuccess = { _, result -> preloadedBitmap = (result.drawable as? BitmapDrawable)?.bitmap; dataReady = true; onReady() }, onError = { _, _ -> preloadedBitmap = null; dataReady = true; onReady() }).build()
                imageLoader.enqueue(request)
            } else { dataReady = true; onReady() }
        }
    }

    private fun performSkipAndSlideIn(toNext: Boolean, inX: Float, targetIndex: Int, nextSong: MusicFiles?) {
        nextSong?.let { song ->
            lastSongId = song.id
            binding.songName.text = song.title ?: getString(R.string.unknown)
            binding.songArtist.text = song.artist ?: getString(R.string.unknown)
            binding.durationTotal.text = song.durationDuration.formatAsTime()
            preloadedBitmap?.let { binding.image.loadBitmap(it, crossfade = false); binding.imageBlur.loadBitmap(it, blurRadius = 100f, crossfade = false) } ?: run { binding.image.setImageResource(R.drawable.ic_cover_song); binding.imageBlur.setImageResource(R.drawable.ic_cover_song_blur) }
            applyCurrentModeColors()
            loadWaveform(song.id ?: "", song.path ?: "")
            lifecycleScope.launch(Dispatchers.IO) {
                val bitrate = getBitrate(song.path)
                withContext(Dispatchers.Main) { binding.bitrate.text = bitrate?.let { getString(R.string.kbps_format, it) } ?: ""; binding.bitrate.isVisible = bitrate != null }
            }
        }
        if (toNext) nextBtn(animate = false, forceIndex = targetIndex) else prevBtn(animate = false, forceIndex = targetIndex)
        binding.card.translationX = inX
        binding.card.animate().translationX(0f).alpha(1f).setDuration(resources.getInteger(R.integer.anim_duration_skip).toLong()).withEndAction { isAnimating = false; preloadedBitmap = null }.start()
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            if (abs(diffX) > abs(e2.y - e1.y) && abs(diffX) > 100 && abs(velocityX) > 100) {
                if (diffX > 0) prevBtn() else nextBtn()
                return true
            }
            return false
        }
    }

    private fun startProgressUpdater() {
        stopProgressUpdater()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    val currentPos = controller.currentPosition
                    val duration = controller.duration
                    if (duration > 0) {
                        if (binding.seekBar.max != (duration.toInt() / 1000)) binding.seekBar.max = duration.toInt() / 1000
                        if (binding.durationTotal.text == "0:00" || binding.durationTotal.text == DATA.UNKNOWN) binding.durationTotal.text = duration.milliseconds.formatAsTime()
                        binding.seekBar.progress = (currentPos / 1000).toInt()
                        if (binding.waveformSeekBar.sample != null) {
                            binding.waveformSeekBar.progress = (currentPos.toFloat() / duration.toFloat()) * 100
                        }
                        binding.durationPlayed.text = currentPos.milliseconds.formatAsTime()
                    }
                }
                delay(1000.milliseconds)
            }
        }
    }

    private fun stopProgressUpdater() { progressJob?.cancel(); progressJob = null }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({ mediaController = controllerFuture?.get(); mediaController?.addListener(this); onControllerConnected() }, MoreExecutors.directExecutor())
    }

    private fun onControllerConnected() {
        mediaController?.let { controller ->
            val intentPosition = intent.getIntExtra(DATA.POSITION, -1)
            if (intentPosition != -1 && !isIntentProcessed) {
                isIntentProcessed = true; intent.removeExtra(DATA.POSITION)
                val currentItem = controller.currentMediaItem
                val targetSong = viewModel.listSongs.getOrNull(intentPosition)
                if (currentItem != null && controller.currentMediaItemIndex == intentPosition && currentItem.mediaId == targetSong?.id) {
                    viewModel.updatePositionAndSong(intentPosition)
                } else {
                    if (viewModel.listSongs.isNotEmpty()) forcePlaySong(controller, intentPosition)
                    else lifecycleScope.launch { while (viewModel.listSongs.isEmpty()) delay(30.milliseconds); forcePlaySong(controller, intentPosition) }
                }
            } else {
                val index = controller.currentMediaItemIndex
                if (index != -1 && index in viewModel.listSongs.indices) {
                    if (controller.currentMediaItem != null) {
                        if (viewModel.position != index) viewModel.updatePositionAndSong(index)
                        nowPlayerViewModel.saveAndBroadcastNextSong(viewModel.listSongs[index])
                    }
                } else if (controller.currentMediaItem == null && viewModel.position != -1) setupMediaItems(controller)
            }
            if ((controller.duration / 1000).toInt() > 0) binding.seekBar.max = (controller.duration / 1000).toInt()
            stopProgressUpdater(); startProgressUpdater()
            binding.playPause.setImageResource(if (controller.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            updateRepeatShuffleIcons(controller)
        }
    }

    private fun forcePlaySong(controller: MediaController, pos: Int, keepProgress: Boolean = false) {
        val currentProgress = if (keepProgress) controller.currentPosition else 0L
        viewModel.updatePositionAndSong(pos, forceUpdate = true)
        val mediaItems = viewModel.listSongs.map { song ->
            val metadata = MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setExtras(Bundle().apply { putString("ALBUM_ID", song.albumId); putString("CACHED_IMAGE_PATH", song.cachedImagePath) }).build()
            MediaItem.Builder().setUri(song.path?.toUri() ?: "".toUri()).setMediaMetadata(metadata).setMediaId(song.id ?: "").build()
        }
        controller.setMediaItems(mediaItems, pos, currentProgress); controller.prepare(); controller.play()
        val song = viewModel.listSongs[pos]
        binding.songName.text = song.title; binding.songArtist.text = song.artist
        updateSongUI(song)
        lifecycleScope.launch { delay(300.milliseconds); song.path?.let { path -> song.id?.let { id -> loadWaveform(id, path) } } }
    }

    private fun setupMediaItems(controller: MediaController) {
        val mediaItems = viewModel.listSongs.map { song ->
            val metadata = MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setExtras(Bundle().apply { putString("ALBUM_ID", song.albumId); putString("CACHED_IMAGE_PATH", song.cachedImagePath) }).build()
            MediaItem.Builder().setUri(song.path?.toUri() ?: "".toUri()).setMediaMetadata(metadata).setMediaId(song.id ?: "").build()
        }
        controller.setMediaItems(mediaItems, viewModel.position, viewModel.lastProgress); controller.prepare()
    }

    override fun onStop() {
        super.onStop()
        mediaController?.removeListener(this)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null; stopProgressUpdater()
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putBoolean("intent_processed", isIntentProcessed) }
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) { binding.waveformSeekBar.alpha = if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) 0.5f else 1f }
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        binding.playPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        if (isPlaying) startProgressUpdater() else stopProgressUpdater()
        mediaController?.let { val duration = it.duration; if (duration > 0) {
            binding.seekBar.progress = (it.currentPosition / 1000).toInt()
            if (binding.waveformSeekBar.sample != null) {
                binding.waveformSeekBar.progress = (it.currentPosition.toFloat() / duration.toFloat()) * 100
            }
            binding.durationPlayed.text = it.currentPosition.milliseconds.formatAsTime()
        } }
    }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaController?.let { controller ->
            val index = controller.currentMediaItemIndex
            if (index != -1 && index in viewModel.listSongs.indices) {
                if (index != viewModel.position) viewModel.updatePositionAndSong(index)
                nowPlayerViewModel.saveAndBroadcastNextSong(viewModel.listSongs[index])
            }
        }
    }
    override fun onEvents(player: Player, events: Player.Events) { if (events.containsAny(Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) updateRepeatShuffleIcons(player) }
    private fun updateRepeatShuffleIcons(repeatMode: Int, isShuffle: Boolean) { binding.repeat.setImageResource(when { isShuffle -> R.drawable.ic_shuffle_on; repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one; else -> R.drawable.ic_repeat }) }
    private fun updateRepeatShuffleIcons(player: Player) { updateRepeatShuffleIcons(player.repeatMode, musicViewModel.shuffleMode.value) }
}
