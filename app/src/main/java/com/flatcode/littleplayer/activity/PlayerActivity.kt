package com.flatcode.littleplayer.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : AppCompatActivity(), ActionPlaying, ServiceConnection {

    private lateinit var binding: ActivityPlayerBinding
    private val context: Context = this@PlayerActivity
    private val viewModel: PlayerViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())
    var musicService: MusicService? = null

    private val progressUpdater = object : Runnable {
        override fun run() {
            musicService?.let { service ->
                val mCurrentPosition = service.getCurrentPosition() / 1000
                binding.seekBar.progress = mCurrentPosition
                binding.durationPlayed.text = formattedTime(mCurrentPosition)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreen()
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        getIntentMethod()
        setupListeners()
        observeViewModel()

        handler.post(progressUpdater)
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
            service.stop()
            service.release()

            val prevPos = viewModel.calculatePrevPosition()
            viewModel.updatePositionAndSong(prevPos)

            service.createMediaPlayer(viewModel.position)
            service.start()
            binding.seekBar.max = service.getDuration() / 1000

            resetProgressLoop()
            service.onCompleted()

            binding.playPause.setBackgroundResource(R.drawable.ic_pause)
            binding.playPause.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun nextBtn() {
        musicService?.let { service ->
            service.stop()
            service.release()

            val nextPos = viewModel.calculateNextPosition()
            viewModel.updatePositionAndSong(nextPos)

            service.createMediaPlayer(viewModel.position)
            service.start()
            binding.seekBar.max = service.getDuration() / 1000

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
            } else {
                binding.playPause.setBackgroundResource(R.drawable.ic_pause)
                binding.playPause.setImageResource(R.drawable.ic_pause)
                service.start()
            }
            binding.seekBar.max = service.getDuration() / 1000
            resetProgressLoop()
        }
    }

    private fun resetProgressLoop() {
        handler.removeCallbacks(progressUpdater)
        handler.post(progressUpdater)
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
    }

    override fun onPause() {
        super.onPause()
        unbindService(this)
    }

    private fun formattedTime(currentPosition: Int): String {
        val seconds = (currentPosition % 60).toString()
        val minutes = (currentPosition / 60).toString()
        return if (seconds.length == 1) "$minutes:0$seconds" else "$minutes:$seconds"
    }

    private fun metaData(uri: Uri?) {
        if (uri == null) return
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationTotal =
                (viewModel.listSongs[viewModel.position].duration?.toLong() ?: 0L) / 1000
            binding.durationTotal.text = formattedTime(durationTotal.toInt())

            val art = retriever.embeddedPicture
            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                VOID.coilBitmap(bitmap, binding.image)
                VOID.coilBlurBitmap(context, bitmap, binding.imageBlur, 10)
            } else {
                VOID.coil(null, binding.image)
                binding.songName.setTextColor(Color.WHITE)
                binding.songArtist.setTextColor(Color.DKGRAY)
            }
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val myBinder = service as? MusicService.MyBinder
        musicService = myBinder?.service
        musicService?.setCallBack(this)
        musicService?.musicFiles = viewModel.listSongs

        Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show()

        musicService?.let {
            if (!it.isPlaying()) {
                it.createMediaPlayer(viewModel.position)
                it.start()
            }
            binding.seekBar.max = it.getDuration() / 1000
            metaData(viewModel.uri)
            binding.songName.text = viewModel.listSongs[viewModel.position].title
            binding.songArtist.text = viewModel.listSongs[viewModel.position].artist
            it.onCompleted()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressUpdater)
    }
}