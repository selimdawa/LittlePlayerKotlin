package com.flatcode.littleplayer.fragment

import android.content.ComponentName
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.load
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.FragmentNowPlayerBottomBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            requireContext().launchActivity<PlayerActivity>()
        }

        binding.nextBtn.setOnClickListener {
            mediaController?.seekToNext()
        }

        binding.playPauseBtn.setOnClickListener {
            mediaController?.let { controller ->
                if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
                viewModel.updatePlaybackState(controller.isPlaying)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.currentPlayingSong.observe(viewLifecycleOwner) { song ->
            song?.let {
                if (lastLoadedPath != it.path) {
                    lastLoadedPath = it.path
                    lifecycleScope.launch {
                        val art = withContext(Dispatchers.IO) { getAlbumArt(it.path) }
                        binding.albumArt.load(art ?: R.drawable.logo) { crossfade(enable = true) }
                    }
                }
                binding.name.text = it.title
                binding.artist.text = it.artist
            }
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            updatePlayPauseAnimation(isPlaying)
        }
    }

    private fun updateUiFromPlayer(player: Player) {
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null) {
            val title = currentMediaItem.mediaMetadata.title?.toString() ?: "Unknown Track"
            val artist = currentMediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"
            val path = currentMediaItem.localConfiguration?.uri?.path

            binding.name.text = title
            binding.artist.text = artist

            viewModel.saveAndBroadcastNextSong(
                com.flatcode.littleplayer.model.MusicFiles(
                    path = path, title = title, artist = artist
                )
            )

            if (lastLoadedPath != path) {
                lastLoadedPath = path
                lifecycleScope.launch {
                    val art = withContext(Dispatchers.IO) { getAlbumArt(path) }
                    binding.albumArt.load(art ?: R.drawable.logo) { crossfade(enable = true) }
                }
            }
        }
        updatePlayPauseAnimation(player.isPlaying)
    }

    private fun updatePlayPauseAnimation(isPlaying: Boolean) {
        if (isPlaying) {
            binding.playPauseAnimView.speed = 1f
            binding.playPauseAnimView.playAnimation()
        } else {
            binding.playPauseAnimView.speed = -1f
            binding.playPauseAnimView.playAnimation()
        }
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
            MoreExecutors.directExecutor(),
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

    private fun getAlbumArt(uri: String?): ByteArray? {
        if (uri.isNullOrEmpty()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(uri)
            val art = retriever.embeddedPicture
            retriever.release()
            art
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        viewModel.updatePlaybackState(isPlaying)
        updatePlayPauseAnimation(isPlaying)
        if (isPlaying) startProgressUpdater() else stopProgressUpdater()
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        mediaController?.let { updateUiFromPlayer(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}