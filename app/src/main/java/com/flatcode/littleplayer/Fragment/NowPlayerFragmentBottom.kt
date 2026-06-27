package com.flatcode.littleplayer.Fragment

import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
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
import com.bumptech.glide.Glide
import com.flatcode.littleplayer.Activity.MainActivity
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.Service.MusicService
import com.flatcode.littleplayer.databinding.FragmentNowPlayerBottomBinding

class NowPlayerFragmentBottom : Fragment(), ServiceConnection {

    private var _binding: FragmentNowPlayerBottomBinding? = null
    private val binding get() = _binding!!

    private var musicService: MusicService? = null
    private val handler = Handler(Looper.getMainLooper())

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

        binding.nextBtn.setOnClickListener {
            musicService?.let { service ->
                service.nextBtnClicked()

                activity?.let { act ->
                    val editor = act.getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit()
                    val currentSong = service.musicFiles[service.position]

                    editor.putString(MUSIC_FILE, currentSong.path)
                    editor.putString(ARTIST_NAME, currentSong.artist)
                    editor.putString(SONG_NAME, currentSong.title)
                    editor.apply()

                    val preferences = act.getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE)
                    val path = preferences.getString(MUSIC_FILE, null)
                    val artistName = preferences.getString(ARTIST_NAME, null)
                    val songName = preferences.getString(SONG_NAME, null)

                    if (path != null) {
                        MainActivity.SHOW_MINI_PLAYER = true
                        MainActivity.PATH_TO_FRAG = path
                        MainActivity.ARTIST_TO_FRAG = artistName
                        MainActivity.SONG_NAME_TO_FRAG = songName
                    } else {
                        MainActivity.SHOW_MINI_PLAYER = false
                        MainActivity.PATH_TO_FRAG = null
                        MainActivity.ARTIST_TO_FRAG = null
                        MainActivity.SONG_NAME_TO_FRAG = null
                    }
                }

                if (MainActivity.SHOW_MINI_PLAYER) {
                    MainActivity.PATH_TO_FRAG?.let { path ->
                        val art = getAlbumArt(path)
                        if (art != null) {
                            Glide.with(requireContext()).load(art).into(binding.albumArt)
                        } else {
                            Glide.with(requireContext()).load(R.drawable.logo).into(binding.albumArt)
                        }
                        binding.name.text = MainActivity.SONG_NAME_TO_FRAG
                        binding.artist.text = MainActivity.ARTIST_TO_FRAG
                    }
                }
            }
        }

        binding.playPauseBtn.setOnClickListener {
            musicService?.let { service ->
                service.playPauseBtnClicked()
                if (service.isPlaying()) {
                    binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
                } else {
                    binding.playPauseBtn.setImageResource(R.drawable.ic_play)
                }
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (MainActivity.SHOW_MINI_PLAYER) {
            MainActivity.PATH_TO_FRAG?.let { path ->
                val art = getAlbumArt(path)
                if (art != null) {
                    Glide.with(requireContext()).load(art).into(binding.albumArt)
                } else {
                    Glide.with(requireContext()).load(R.drawable.logo).into(binding.albumArt)
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

    private fun getAlbumArt(uri: String): ByteArray? {
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
            if (it.isPlaying()) {
                binding.playPauseBtn.setImageResource(R.drawable.ic_pause)
            } else {
                binding.playPauseBtn.setImageResource(R.drawable.ic_play)
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val MUSIC_LAST_PLAYED = "LAST_PLAYED"
        const val MUSIC_FILE = "STORED_MUSIC"
        const val ARTIST_NAME = "ARTIST NAME"
        const val SONG_NAME = "SONG NAME"
    }
}