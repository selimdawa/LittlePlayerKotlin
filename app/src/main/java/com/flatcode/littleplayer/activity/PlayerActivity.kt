package com.flatcode.littleplayer.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.AlbumDetailsAdapter
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityPlayerBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.unit.ActionPlaying
import com.flatcode.littleplayer.unit.DATA
import com.flatcode.littleplayer.unit.VOID
import com.flatcode.littleplayer.viewmodel.PlayerViewModel
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
class PlayerActivity : AppCompatActivity(), ActionPlaying, ServiceConnection {

    private lateinit var binding: ActivityPlayerBinding
    private val context: Context = this@PlayerActivity
    private val viewModel: PlayerViewModel by viewModels()

    private var progressJob: Job? = null
    var musicService: MusicService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreen()
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        getIntentMethod()
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (musicService != null && fromUser) {
                    musicService?.seekTo(progress * 1000)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.shuffle.setOnClickListener { viewModel.toggleShuffle() }
        binding.repeat.setOnClickListener { viewModel.toggleRepeat() }

        binding.prev.setOnClickListener { prevBtn() }
        binding.next.setOnClickListener { nextBtn() }
        binding.playPauseBtn.setOnClickListener { playPauseBtn() }
    }

    private fun observeViewModel() {
        viewModel.isShuffle.observe(this) { isShuffle ->
            val icon = if (isShuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off
            binding.shuffle.setImageResource(icon)
        }

        viewModel.isRepeat.observe(this) { isRepeat ->
            val icon = if (isRepeat) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off
            binding.repeat.setImageResource(icon)
        }

        viewModel.currentSong.observe(this) { song ->
            song?.let {
                binding.songName.text = it.title
                binding.songArtist.text = it.artist
                metaData(viewModel.uri)
            }
        }
    }

    private fun getIntentMethod() {
        val position = intent.getIntExtra(DATA.POSITION, -1)
        val sender = intent.getStringExtra(DATA.SENDER)

        viewModel.listSongs = if (sender != null && sender == DATA.ALBUM_DETAILS) {
            AlbumDetailsAdapter.albumFiles ?: ArrayList()
        } else {
            MusicAdapter.mFiles ?: ArrayList()
        }

        if (viewModel.listSongs.isNotEmpty() && position != -1) {
            binding.playPause.setImageResource(R.drawable.ic_pause)
            viewModel.updatePositionAndSong(position)
        }

        val intentService = Intent(context, MusicService::class.java).apply {
            putExtra(DATA.SERVICE_POSITION, viewModel.position)
        }
        startService(intentService)
    }

    override fun prevBtn() {
        musicService?.let { service ->
            val prevPos = viewModel.calculatePrevPosition()
            viewModel.updatePositionAndSong(prevPos)

            service.createMediaPlayer(viewModel.position)
            service.start()

            binding.seekBar.max = service.getDuration() / 1000
            binding.songName.text = viewModel.listSongs[viewModel.position].title
            binding.songArtist.text = viewModel.listSongs[viewModel.position].artist
            metaData(viewModel.uri)

            resetProgressLoop()
            service.onCompleted()

            binding.playPause.setBackgroundResource(R.drawable.ic_pause)
            binding.playPause.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun nextBtn() {
        musicService?.let { service ->
            val nextPos = viewModel.calculateNextPosition()
            viewModel.updatePositionAndSong(nextPos)

            service.createMediaPlayer(viewModel.position)
            service.start()

            binding.seekBar.max = service.getDuration() / 1000
            binding.songName.text = viewModel.listSongs[viewModel.position].title
            binding.songArtist.text = viewModel.listSongs[viewModel.position].artist
            metaData(viewModel.uri)

            resetProgressLoop()
            service.onCompleted()

            binding.playPause.setBackgroundResource(R.drawable.ic_pause)
            binding.playPause.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun playPauseBtn() {
        musicService?.let { service ->
            if (service.isPlaying()) {
                binding.playPause.setBackgroundResource(R.drawable.ic_play)
                binding.playPause.setImageResource(R.drawable.ic_play)
                service.pause()
                stopProgressUpdater()
            } else {
                binding.playPause.setBackgroundResource(R.drawable.ic_pause)
                binding.playPause.setImageResource(R.drawable.ic_pause)
                service.start()
                startProgressUpdater()
            }
            binding.seekBar.max = service.getDuration() / 1000
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
                musicService?.let { service ->
                    val mCurrentPosition = service.getCurrentPosition() / 1000
                    val duration = service.getDuration() / 1000

                    if (duration > 0 && binding.seekBar.max != duration) {
                        binding.seekBar.max = duration
                    }

                    binding.seekBar.progress = mCurrentPosition
                    binding.durationPlayed.text = formattedTime(mCurrentPosition)
                }
                delay(1000.milliseconds)
            }
        }
    }

    private fun stopProgressUpdater() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun setFullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(context, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startProgressUpdater()
    }

    override fun onPause() {
        super.onPause()
        unbindService(this)
        stopProgressUpdater()
    }

    private fun formattedTime(currentPosition: Int): String {
        val seconds = (currentPosition % 60).toString()
        val minutes = (currentPosition / 60).toString()
        return if (seconds.length == 1) "$minutes:0$seconds" else "$minutes:$seconds"
    }

    private fun metaData(uri: Uri?) {
        if (uri == null) return
        lifecycleScope.launch {
            val retriever = android.media.MediaMetadataRetriever()
            val art = withContext(Dispatchers.IO) {
                try {
                    retriever.setDataSource(context, uri)
                    retriever.embeddedPicture
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val durationTotal =
                (viewModel.listSongs[viewModel.position].duration?.toLong() ?: 0L) / 1000
            binding.durationTotal.text = formattedTime(durationTotal.toInt())

            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                VOID.coilBitmap(bitmap, binding.image)
                VOID.coilBlurBitmap(context, bitmap, binding.imageBlur, 10)
            } else {
                VOID.coil(null, binding.image)
                binding.songName.setTextColor(Color.WHITE)
                binding.songArtist.setTextColor(Color.DKGRAY)
            }

            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val myBinder = service as? MusicService.MyBinder
        musicService = myBinder?.service

        musicService?.let { serviceInstance ->
            serviceInstance.setCallBack(this)
            serviceInstance.musicFiles = viewModel.listSongs

            Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show()

            val currentPlayingUri = serviceInstance.exoPlayer?.currentMediaItem?.localConfiguration?.uri
            if (currentPlayingUri != viewModel.uri) {
                serviceInstance.createMediaPlayer(viewModel.position)
                serviceInstance.start()
            }

            binding.songName.text = viewModel.listSongs[viewModel.position].title
            binding.songArtist.text = viewModel.listSongs[viewModel.position].artist
            metaData(viewModel.uri)

            val duration = serviceInstance.getDuration() / 1000
            if (duration > 0) {
                binding.seekBar.max = duration
            }

            serviceInstance.onCompleted()
            resetProgressLoop()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressUpdater()
    }
}