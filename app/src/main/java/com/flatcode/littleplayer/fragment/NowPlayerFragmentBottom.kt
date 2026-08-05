package com.flatcode.littleplayer.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.databinding.FragmentNowPlayerBottomBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@AndroidEntryPoint
class NowPlayerFragmentBottom : Fragment(), Player.Listener {

    private var _binding: FragmentNowPlayerBottomBinding? = null
    private val binding get() = _binding!!

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var lastLoadedPath: String? = null

    private val viewModel: NowPlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNowPlayerBottomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.cardBottomPlayer.setOnClickListener {
            requireContext().openPlayer(-1, binding.albumArt)
        }

        binding.nextBtn.setOnClickListener {
            mediaController?.let { controller ->
                controller.seekToNext()
                if (!controller.playWhenReady) {
                    controller.play()
                }
            }
        }

        binding.playPauseBtn.setOnClickListener {
            mediaController?.let { controller ->
                if (controller.currentMediaItem == null) {
                    val song = viewModel.currentPlayingSong.value
                    lifecycleScope.launch {
                        val currentQueue = viewModel.getCurrentQueue()
                        if (song != null && currentQueue.isNotEmpty()) {
                            val mediaItems = currentQueue.map { item ->
                                MediaItem.Builder().setUri(item.path?.toUri())
                                    .setMediaId(item.id ?: "").setMediaMetadata(
                                        MediaMetadata.Builder().setTitle(item.title)
                                            .setArtist(item.artist).setExtras(Bundle().apply {
                                                putString("ALBUM_ID", item.albumId)
                                                putString("CACHED_IMAGE_PATH", item.cachedImagePath)
                                            }).build()
                                    ).build()
                            }
                            val startIndex =
                                currentQueue.indexOfFirst { it.path == song.path }.coerceAtLeast(0)
                            controller.setMediaItems(mediaItems, startIndex, 0L)
                            controller.prepare()
                            controller.play()
                        } else if (song != null) {
                            val mediaItem = MediaItem.Builder().setUri(song.path?.toUri())
                                .setMediaId(song.id ?: "").setMediaMetadata(
                                    MediaMetadata.Builder().setTitle(song.title)
                                        .setArtist(song.artist).setExtras(Bundle().apply {
                                            putString("ALBUM_ID", song.albumId)
                                            putString("CACHED_IMAGE_PATH", song.cachedImagePath)
                                        }).build()
                                ).build()
                            controller.setMediaItem(mediaItem)
                            controller.prepare()
                            controller.play()
                        }
                        viewModel.updatePlaybackState(controller.isPlaying)
                    }
                } else {
                    if (controller.isPlaying) {
                        controller.pause()
                    } else {
                        controller.play()
                    }
                    viewModel.updatePlaybackState(controller.isPlaying)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.currentPlayingSong.collectWithLifecycle(viewLifecycleOwner) { song ->
            song?.let {
                binding.albumArt.loadSongImage(it.albumId, it.path, it.cachedImagePath)
                binding.name.text = it.safeTitle
                binding.artist.text = it.safeArtist
            }
        }

        viewModel.isPlaying.collectWithLifecycle(viewLifecycleOwner) { isPlaying ->
            updatePlayPauseAnimation(isPlaying)
        }

        combine(
            viewModel.bottomPlayerThemeEnabled,
            viewModel.themeColorMode,
            viewModel.currentThemeColor
        ) { enabled, mode, color ->
            Triple(enabled, mode, color)
        }.collectWithLifecycle(viewLifecycleOwner) { (enabled, mode, color) ->
            updateThemeColors(enabled, mode, color ?: Color.WHITE)
        }
    }

    private fun updateThemeColors(enabled: Boolean, mode: Int, color: Int) {
        val track = requireContext().getLibraryColor("mc_track")
        val tick = requireContext().getLibraryColor("mc_tick")

        fun applyTo(view: View) {
            val background = view.background.mutate()
            val gradient = when (background) {
                is GradientDrawable -> background
                is LayerDrawable -> background.getDrawable(0) as? GradientDrawable
                else -> null
            }

            gradient?.let {
                if (enabled) {
                    val targetColor = when (mode) {
                        DATA.MODE_PALETTE -> color
                        DATA.MODE_WHITE -> Color.WHITE
                        else -> null
                    }
                    if (targetColor != null) {
                        it.colors = intArrayOf(targetColor, targetColor)
                    } else {
                        it.colors = intArrayOf(track, tick)
                    }
                } else {
                    it.colors = intArrayOf(track, tick)
                }
                view.background = background
            }
        }

        applyTo(binding.playPauseBtn)
        applyTo(binding.albumArtContainer)
        applyTo(binding.bottomPlayerContainer)
    }

    private fun updateUiFromPlayer(player: Player) {
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null) {
            val title = currentMediaItem.mediaMetadata.title?.toString() ?: DATA.UNKNOWN
            val artist = currentMediaItem.mediaMetadata.artist?.toString() ?: DATA.UNKNOWN
            val path = currentMediaItem.localConfiguration?.uri?.path
            val albumId = currentMediaItem.mediaMetadata.extras?.getString("ALBUM_ID")
            val cachedPath = currentMediaItem.mediaMetadata.extras?.getString("CACHED_IMAGE_PATH")

            viewModel.saveAndBroadcastNextSong(
                MusicFiles(
                    path = path,
                    title = title,
                    artist = artist,
                    albumId = albumId,
                    cachedImagePath = cachedPath
                )
            )

            if (lastLoadedPath != path) {
                lastLoadedPath = path
                binding.albumArt.loadSongImage(albumId, path, cachedPath)
                binding.name.text = title
                binding.artist.text = artist

                // Extract Palette Color
                val request = ImageRequest.Builder(requireContext())
                    .data(cachedPath ?: path)
                    .allowHardware(false)
                    .target { result ->
                        val bitmap = (result as? BitmapDrawable)?.bitmap
                        bitmap?.let { b ->
                            Palette.from(b).generate { palette ->
                                val color = palette?.getVibrantColor(Color.GRAY)
                                    ?: palette?.getLightVibrantColor(Color.GRAY)
                                    ?: palette?.getDominantColor(Color.GRAY)
                                    ?: Color.GRAY
                                viewModel.updateThemeColor(color)
                            }
                        }
                    }.build()
                requireContext().imageLoader.enqueue(request)
            }
        } else {
            val song = viewModel.currentPlayingSong.value
            song?.let {
                binding.albumArt.loadSongImage(it.albumId, it.path, it.cachedImagePath)
                binding.name.text = it.safeTitle
                binding.artist.text = it.safeArtist

                // Extract Palette Color if missing
                if (viewModel.currentThemeColor.value == null) {
                    val request = ImageRequest.Builder(requireContext())
                        .data(it.cachedImagePath ?: it.path)
                        .allowHardware(false)
                        .target { result ->
                            val bitmap = (result as? BitmapDrawable)?.bitmap
                            bitmap?.let { b ->
                                Palette.from(b).generate { palette ->
                                    val color = palette?.getVibrantColor(Color.GRAY)
                                        ?: palette?.getLightVibrantColor(Color.GRAY)
                                        ?: palette?.getDominantColor(Color.GRAY)
                                        ?: Color.GRAY
                                    viewModel.updateThemeColor(color)
                                }
                            }
                        }.build()
                    requireContext().imageLoader.enqueue(request)
                }
            }
        }
        updatePlayPauseAnimation(player.isPlaying)
    }

    private fun updatePlayPauseAnimation(isPlaying: Boolean) {
        binding.playPauseView.setPlaying(isPlaying)
    }

    override fun onStart() {
        super.onStart()
        val currentContext = context ?: return
        val sessionToken = SessionToken(
            currentContext, ComponentName(currentContext, MusicService::class.java)
        )
        controllerFuture = MediaController.Builder(currentContext, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(this)
                mediaController?.let { updateUiFromPlayer(it) }
                startProgressUpdater()
            },
            ContextCompat.getMainExecutor(currentContext),
        )
    }

    override fun onStop() {
        super.onStop()
        stopProgressUpdater()
        mediaController?.removeListener(this)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    private fun startProgressUpdater() {
        stopProgressUpdater()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        val duration = controller.duration
                        if (duration > 0) {
                            val currentPosition = controller.currentPosition
                            val progress = ((currentPosition * 100) / duration).toInt()
                            binding.miniProgressBar.progress = progress
                        }
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

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        viewModel.updatePlaybackState(isPlaying)
        updatePlayPauseAnimation(isPlaying)
        if (isPlaying) startProgressUpdater() else stopProgressUpdater()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaController?.let { updateUiFromPlayer(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}