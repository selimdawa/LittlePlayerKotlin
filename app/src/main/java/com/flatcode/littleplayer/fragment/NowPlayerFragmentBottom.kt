package com.flatcode.littleplayer.fragment

import android.content.ComponentName
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.FragmentNowPlayerBottomBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getCurrentThemeColors
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
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
        binding.playerContent.cardBottomPlayer.setOnClickListener {
            requireContext().openPlayer(-1, binding.playerContent.albumArt)
        }

        binding.playerContent.nextBtn.setOnClickListener {
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

        binding.playerContent.playPauseBtn.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
            binding.playerContent.playPause.startAnimation(animation)
            mediaController?.let { controller ->
                if (controller.currentMediaItem == null) {
                    val song = viewModel.currentPlayingSong.value
                    lifecycleScope.launch {
                        val currentQueue = viewModel.getCurrentQueue()
                        if ((song != null) && currentQueue.isNotEmpty()) {
                            val mediaItems = currentQueue.map { item ->
                                MediaItem.Builder().setUri(item.path?.toUri())
                                    .setMediaId(item.id ?: "").setMediaMetadata(
                                        MediaMetadata.Builder().setTitle(item.title)
                                            .setArtist(item.artist).setExtras(
                                                Bundle().apply {
                                                    putString("ALBUM_ID", item.albumId)
                                                    putString(
                                                        "CACHED_IMAGE_PATH", item.cachedImagePath,
                                                    )
                                                },
                                            ).build(),
                                    ).build()
                            }
                            val startIndex = if (!song.id.isNullOrEmpty()) {
                                currentQueue.indexOfFirst { it.id == song.id }
                            } else {
                                currentQueue.indexOfFirst { it.path == song.path }
                            }.coerceAtLeast(0)
                            controller.setMediaItems(mediaItems, startIndex, 0L)
                            controller.prepare()
                            controller.play()
                        } else if (song != null) {
                            val mediaItem = MediaItem.Builder().setUri(song.path?.toUri())
                                .setMediaId(song.id ?: "").setMediaMetadata(
                                    MediaMetadata.Builder().setTitle(song.title)
                                        .setArtist(song.artist).setExtras(
                                            Bundle().apply {
                                                putString("ALBUM_ID", song.albumId)
                                                putString("CACHED_IMAGE_PATH", song.cachedImagePath)
                                            },
                                        ).build(),
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
                binding.playerContent.albumArt.loadSongImage(
                    it.albumId, it.path, it.cachedImagePath, it.album
                )
                binding.playerContent.name.text = it.safeTitle
                binding.playerContent.artist.text = it.safeArtist

                if (lastLoadedPath != it.path) {
                    lastLoadedPath = it.path
                }
            }
        }

        viewModel.isPlaying.collectWithLifecycle(viewLifecycleOwner) { isPlaying ->
            updatePlayPauseAnimation(isPlaying)
        }

        combine(
            viewModel.bottomPlayerThemeEnabled,
            viewModel.themeColorMode,
            viewModel.currentThemeColor,
            viewModel.marqueeEnabled,
        ) { enabled, mode, color, marquee ->
            Quadruple(enabled, mode, color, marquee)
        }.collectWithLifecycle(viewLifecycleOwner) { (enabled, mode, color, marquee) ->
            updateThemeColors(enabled, mode, color)
            binding.playerContent.name.isSelected = marquee
        }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A, val second: B, val third: C, val fourth: D
    )

    private fun updateThemeColors(enabled: Boolean, mode: Int, colorPair: Pair<Int, Int>?) {
        val colors = requireContext().getCurrentThemeColors(
            if (enabled) mode else DATA.MODE_BASIC, colorPair
        )

        fun applyTo(view: View) {
            val background = view.background?.mutate() ?: return
            val gradient = when (background) {
                is GradientDrawable -> background
                is LayerDrawable -> background.getDrawable(0) as? GradientDrawable
                else -> null
            }

            gradient?.let {
                it.colors = intArrayOf(colors.first, colors.second)
                view.background = background
            }
        }

        applyTo(binding.playerContent.playPauseBtn)
        applyTo(binding.playerContent.albumArtContainer)
        applyTo(binding.playerContent.bottomPlayerContainer)
    }

    private fun updateUiFromPlayer(player: Player) {
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null) {
            val id = currentMediaItem.mediaId
            
            // Try to find the song in the existing playlist to get its colors
            val existingSong = viewModel.getCurrentSongFromRepository(id)
            
            if (existingSong != null) {
                viewModel.saveAndBroadcastNextSong(existingSong)
            } else {
                val title = currentMediaItem.mediaMetadata.title?.toString() ?: getString(R.string.unknown)
                val artist = currentMediaItem.mediaMetadata.artist?.toString() ?: getString(R.string.unknown)
                val album = currentMediaItem.mediaMetadata.albumTitle?.toString()
                val path = currentMediaItem.localConfiguration?.uri?.path
                val albumId = currentMediaItem.mediaMetadata.extras?.getString("ALBUM_ID")
                val cachedPath = currentMediaItem.mediaMetadata.extras?.getString("CACHED_IMAGE_PATH")

                viewModel.saveAndBroadcastNextSong(
                    MusicFiles(
                        path = path,
                        title = title,
                        artist = artist,
                        album = album,
                        id = id,
                        albumId = albumId,
                        cachedImagePath = cachedPath,
                    ),
                )
            }
        }
        updatePlayPauseAnimation(player.isPlaying)
    }

    private fun updatePlayPauseAnimation(isPlaying: Boolean) {
        val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.playerContent.playPause.setImageResource(icon)
    }

    override fun onStart() {
        super.onStart()
        val currentContext = context ?: return
        val sessionToken = SessionToken(
            currentContext,
            ComponentName(currentContext, MusicService::class.java),
        )
        controllerFuture = MediaController.Builder(currentContext, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    if ((_binding != null) && isAdded) {
                        mediaController = controllerFuture?.get()
                        mediaController?.addListener(this)
                        mediaController?.let { updateUiFromPlayer(it) }
                        startProgressUpdater()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
                            binding.playerContent.miniProgressBar.progress = progress
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