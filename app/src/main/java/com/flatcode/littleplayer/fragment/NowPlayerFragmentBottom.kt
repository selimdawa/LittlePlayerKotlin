package com.flatcode.littleplayer.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.load
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.MainActivity
import com.flatcode.littleplayer.databinding.FragmentNowPlayerBottomBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel

class NowPlayerFragmentBottom : Fragment(), ServiceConnection {

    private var _binding: FragmentNowPlayerBottomBinding? = null
    private val binding get() = _binding!!

    private var musicService: MusicService? = null
    private val handler = Handler(Looper.getMainLooper())

    private val viewModel: NowPlayerViewModel by activityViewModels()

    private val progressUpdater = object : Runnable {
        override fun run() {
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
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
                if (service.musicFiles.isNotEmpty() && service.position in service.musicFiles.indices) {
                    val currentSong = service.musicFiles[service.position]
                    viewModel.saveAndBroadcastNextSong(currentSong)
                }
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
                val art = getAlbumArt(it.path)
                binding.albumArt.load(art ?: R.drawable.logo) {
                    crossfade(true)
                }
                binding.name.text = it.title
                binding.artist.text = it.artist
            }
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            binding.playPauseBtn.setImageResource(icon)
        }
    }

    override fun onResume() {
        super.onResume()
        if (MainActivity.SHOW_MINI_PLAYER) {
            MainActivity.PATH_TO_FRAG?.let { path ->
                val art = getAlbumArt(path)
                binding.albumArt.load(art ?: R.drawable.logo) {
                    crossfade(true)
                }
                binding.name.text = MainActivity.SONG_NAME_TO_FRAG
                binding.artist.text = MainActivity.ARTIST_TO_FRAG

                val intent = Intent(context, MusicService::class.java)
                context?.bindService(intent, this, Context.BIND_AUTO_CREATE)
            }
        }
        handler.post(progressUpdater)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(progressUpdater)
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

        musicService?.let {
            viewModel.updatePlaybackState(it.isPlaying())
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}