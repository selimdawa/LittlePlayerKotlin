package com.flatcode.littleplayer.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.load
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.FragmentNowPlayerBottomBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
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
class NowPlayerFragmentBottom : Fragment(), ServiceConnection, Player.Listener {

    private var _binding: FragmentNowPlayerBottomBinding? = null
    private val binding get() = _binding!!

    private var musicService: MusicService? = null
    private var progressJob: Job? = null
    private var lastLoadedPath: String? = null

    private val viewModel: NowPlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        binding.nextBtn.setOnClickListener {
            musicService?.let { service ->
                service.nextBtnClicked()
                updateUiFromService(service)
            }
        }

        binding.playPauseBtn.setOnClickListener {
            musicService?.let { service ->
                service.playPauseBtnClicked()
                viewModel.updatePlaybackState(service.isPlaying())
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
                        binding.albumArt.load(art ?: R.drawable.logo) { crossfade(true) }
                    }
                }
                binding.name.text = it.title
                binding.artist.text = it.artist
            }
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            if (isPlaying) {
                binding.playPauseAnimView.speed = 1f
                binding.playPauseAnimView.playAnimation()
            } else {
                binding.playPauseAnimView.speed = -1f
                binding.playPauseAnimView.playAnimation()
            }
        }
    }

    private fun updateUiFromService(service: MusicService) {
        val player = service.exoPlayer ?: return
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null) {
            val title = currentMediaItem.mediaMetadata.title?.toString() ?: "Unknown Track"
            val artist = currentMediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"
            val path = currentMediaItem.localConfiguration?.uri?.path

            binding.name.text = title
            binding.artist.text = artist

            if (lastLoadedPath != path) {
                lastLoadedPath = path
                lifecycleScope.launch {
                    val art = withContext(Dispatchers.IO) { getAlbumArt(path) }
                    binding.albumArt.load(art ?: R.drawable.logo) { crossfade(true) }
                }
            }
        }
        if (player.isPlaying) {
            binding.playPauseAnimView.progress = 1f
        } else {
            binding.playPauseAnimView.progress = 0f
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(context, MusicService::class.java)
        context?.bindService(intent, this, Context.BIND_AUTO_CREATE)
        startProgressUpdater()
    }

    override fun onPause() {
        super.onPause()
        stopProgressUpdater()
        musicService?.exoPlayer?.removeListener(this)
        context?.unbindService(this)
    }

    private fun startProgressUpdater() {
        stopProgressUpdater()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                musicService?.let { service ->
                    if (service.isPlaying()) {
                        val duration = service.getDuration()
                        if (duration > 0) {
                            val currentPosition = service.getCurrentPosition()
                            val progress = (currentPosition.toLong() * 100 / duration).toInt()
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

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as? MusicService.MyBinder
        musicService = binder?.service

        musicService?.let { serviceInstance ->
            serviceInstance.exoPlayer?.addListener(this)
            viewModel.updatePlaybackState(serviceInstance.isPlaying())
            updateUiFromService(serviceInstance)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) || events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
            musicService?.let { service ->
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    updateUiFromService(service)
                } else {
                    if (player.isPlaying) {
                        binding.playPauseAnimView.speed = 1f
                        binding.playPauseAnimView.playAnimation()
                    } else {
                        binding.playPauseAnimView.speed = -1f
                        binding.playPauseAnimView.playAnimation()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}