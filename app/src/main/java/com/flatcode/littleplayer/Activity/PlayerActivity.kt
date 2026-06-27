package com.flatcode.littleplayer.Activity

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
import androidx.appcompat.app.AppCompatActivity
import com.flatcode.littleplayer.Adapter.AlbumDetailsAdapter
import com.flatcode.littleplayer.Adapter.MusicAdapter
import com.flatcode.littleplayer.Model.MusicFiles
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.Service.MusicService
import com.flatcode.littleplayer.unit.ActionPlaying
import com.flatcode.littleplayer.unit.DATA
import com.flatcode.littleplayer.unit.VOID
import com.flatcode.littleplayer.databinding.ActivityPlayerBinding
import java.util.ArrayList
import java.util.Random

class PlayerActivity : AppCompatActivity(), ActionPlaying, ServiceConnection {

    private lateinit var binding: ActivityPlayerBinding
    private val context: Context = this@PlayerActivity

    private var position = -1
    private val handler = Handler(Looper.getMainLooper())
    private var playThread: Thread? = null
    private var prevThread: Thread? = null
    private var nextThread: Thread? = null
    var musicService: MusicService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreen()
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.hide()
        getIntentMethod()

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

        PlayerActivity@this.runOnUiThread(object : Runnable {
            override fun run() {
                musicService?.let { service ->
                    val mCurrentPosition = service.getCurrentPosition() / 1000
                    binding.seekBar.progress = mCurrentPosition
                    binding.durationPlayed.text = formattedTime(mCurrentPosition)
                }
                handler.postDelayed(this, 1000)
            }
        })

        binding.shuffle.setOnClickListener {
            if (MainActivity.shuffleBoolean) {
                MainActivity.shuffleBoolean = false
                binding.shuffle.setImageResource(R.drawable.ic_shuffle_off)
            } else {
                MainActivity.shuffleBoolean = true
                binding.shuffle.setImageResource(R.drawable.ic_shuffle_on)
            }
        }

        binding.repeat.setOnClickListener {
            if (MainActivity.repeatBoolean) {
                MainActivity.repeatBoolean = false
                binding.repeat.setImageResource(R.drawable.ic_repeat_off)
            } else {
                MainActivity.repeatBoolean = true
                binding.repeat.setImageResource(R.drawable.ic_repeat_on)
            }
        }
    }

    private fun setFullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    override fun onResume() {
        val intent = Intent(context, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        playThreadBtn()
        nextThreadBtn()
        prevThreadBtn()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        unbindService(this)
    }

    private fun prevThreadBtn() {
        prevThread = Thread {
            binding.prev.setOnClickListener { prevBtn() }
        }.apply { start() }
    }

    override fun prevBtn() {
        musicService?.let { service ->
            val playing = service.isPlaying()
            service.stop()
            service.release()

            if (MainActivity.shuffleBoolean && !MainActivity.repeatBoolean) {
                position = getRandom(listSongs.size - 1)
            } else if (!MainActivity.shuffleBoolean && !MainActivity.repeatBoolean) {
                position = if (position - 1 < 0) listSongs.size - 1 else position - 1
            }

            uri = Uri.parse(listSongs[position].path)
            service.createMediaPlayer(position)
            metaData(uri)

            binding.songName.text = listSongs[position].title
            binding.songArtist.text = listSongs[position].artist
            binding.seekBar.max = service.getDuration() / 1000

            PlayerActivity@this.runOnUiThread(object : Runnable {
                override fun run() {
                    musicService?.let {
                        val currentPosition = it.getCurrentPosition() / 1000
                        binding.seekBar.progress = currentPosition
                    }
                    handler.postDelayed(this, 1000)
                }
            })

            service.onCompleted()
            if (playing) {
                binding.playPause.setBackgroundResource(R.drawable.ic_pause)
                service.start()
            } else {
                binding.playPause.setBackgroundResource(R.drawable.ic_play)
            }
        }
    }

    private fun nextThreadBtn() {
        nextThread = Thread {
            binding.next.setOnClickListener { nextBtn() }
        }.apply { start() }
    }

    override fun nextBtn() {
        musicService?.let { service ->
            val playing = service.isPlaying()
            service.stop()
            service.release()

            if (MainActivity.shuffleBoolean && !MainActivity.repeatBoolean) {
                position = getRandom(listSongs.size - 1)
            } else if (!MainActivity.shuffleBoolean && !MainActivity.repeatBoolean) {
                position = (position + 1) % listSongs.size
            }

            uri = Uri.parse(listSongs[position].path)
            service.createMediaPlayer(position)
            metaData(uri)

            binding.songName.text = listSongs[position].title
            binding.songArtist.text = listSongs[position].artist
            binding.seekBar.max = service.getDuration() / 1000

            PlayerActivity@this.runOnUiThread(object : Runnable {
                override fun run() {
                    musicService?.let {
                        val currentPosition = it.getCurrentPosition() / 1000
                        binding.seekBar.progress = currentPosition
                    }
                    handler.postDelayed(this, 1000)
                }
            })

            service.onCompleted()
            if (playing) {
                binding.playPause.setBackgroundResource(R.drawable.ic_pause)
                service.start()
            } else {
                binding.playPause.setBackgroundResource(R.drawable.ic_play)
            }
        }
    }

    private fun getRandom(i: Int): Int {
        val random = Random()
        return if (i > 0) random.nextInt(i + 1) else 0
    }

    private fun playThreadBtn() {
        playThread = Thread {
            binding.playPauseBtn.setOnClickListener { playPauseBtn() }
        }.apply { start() }
    }

    override fun playPauseBtn() {
        musicService?.let { service ->
            if (service.isPlaying()) {
                binding.playPause.setImageResource(R.drawable.ic_play)
                service.pause()
                binding.seekBar.max = service.getDuration() / 1000
                PlayerActivity@this.runOnUiThread(object : Runnable {
                    override fun run() {
                        musicService?.let {
                            val currentPosition = it.getCurrentPosition() / 1000
                            binding.seekBar.progress = currentPosition
                        }
                        handler.postDelayed(this, 1000)
                    }
                })
            } else {
                binding.playPause.setImageResource(R.drawable.ic_pause)
                service.start()
                binding.seekBar.max = service.getDuration() / 1000
                PlayerActivity@this.runOnUiThread(object : Runnable {
                    override fun run() {
                        musicService?.let {
                            val currentPosition = it.getCurrentPosition() / 1000
                            binding.seekBar.progress = currentPosition
                        }
                        handler.postDelayed(this, 1000)
                    }
                })
            }
        }
    }

    private fun formattedTime(currentPosition: Int): String {
        val seconds = (currentPosition % 60).toString()
        val minutes = (currentPosition / 60).toString()
        val totalOut = "$minutes:$seconds"
        val totalNew = "$minutes:0$seconds"
        return if (seconds.length == 1) totalNew else totalOut
    }

    private fun getIntentMethod() {
        position = intent.getIntExtra(DATA.POSITION, -1)
        val sender = intent.getStringExtra(DATA.SENDER)

        listSongs = if (sender != null && sender == DATA.ALBUM_DETAILS) {
            AlbumDetailsAdapter.albumFiles ?: ArrayList()
        } else {
            MusicAdapter.mFiles ?: ArrayList()
        }

        if (listSongs.isNotEmpty() && position != -1) {
            binding.playPause.setImageResource(R.drawable.ic_pause)
            uri = Uri.parse(listSongs[position].path)
        }

        val intentService = Intent(context, MusicService::class.java).apply {
            putExtra(DATA.SERVICE_POSITION, position)
        }
        startService(intentService)
    }

    private fun metaData(uri: Uri?) {
        if (uri == null) return
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(uri.toString())
            val durationTotal = (listSongs[position].duration?.toInt() ?: 0) / 1000
            binding.durationTotal.text = formattedTime(durationTotal)

            val art = retriever.embeddedPicture
            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                VOID.glideBitmap(context, bitmap, binding.image)
                VOID.glideBlurBitmap(context, bitmap, binding.imageBlur, 10)
            } else {
                VOID.glide(context, null, binding.image)
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
        Toast.makeText(context, "Connected$musicService", Toast.LENGTH_SHORT).show()

        musicService?.let {
            binding.seekBar.max = it.getDuration() / 1000
            metaData(uri)
            binding.songName.text = listSongs[position].title
            binding.songArtist.text = listSongs[position].artist
            it.onCompleted()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    companion object {
        var listSongs = ArrayList<MusicFiles>()
        var uri: Uri? = null
    }
}