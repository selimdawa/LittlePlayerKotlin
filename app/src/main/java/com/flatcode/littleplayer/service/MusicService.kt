package com.flatcode.littleplayer.service

import android.app.Service
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.unit.ActionPlaying
import com.flatcode.littleplayer.unit.DATA
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class MusicService : Service(), MediaPlayer.OnCompletionListener {

    private val binder: IBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null

    var musicFiles = ArrayList<MusicFiles>()

    var uri: Uri? = null
    var position = -1

    private var actionPlaying: ActionPlaying? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val stubPlayer = object : SimpleBasePlayer(mainLooper) {
                override fun getState(): State {
                    return State.Builder()
                        .setAvailableCommands(Player.Commands.EMPTY)
                        .setPlaylist(emptyList())
                        .build()
                }
            }
            mediaSession = MediaSession.Builder(this, stubPlayer).build()
        } catch (_: Exception) {
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class MyBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val myPosition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializableExtra(DATA.SERVICE_POSITION, Integer::class.java) as? Int ?: -1
            } else {
                @Suppress("DEPRECATION")
                it.getSerializableExtra(DATA.SERVICE_POSITION) as? Int ?: -1
            }
            val actionName = it.getStringExtra(DATA.ACTION_NAME)

            if (myPosition != -1) {
                playMedia(myPosition)
            }

            actionName?.let { action ->
                when (action) {
                    "playPause" -> actionPlaying?.playPauseBtn()
                    "next" -> actionPlaying?.nextBtn()
                    "previous" -> actionPlaying?.prevBtn()
                }
            }
        }
        return START_STICKY
    }

    private fun playMedia(startPosition: Int) {
        position = startPosition

        mediaPlayer?.let {
            it.stop()
            it.release()
        }

        if (musicFiles.isNotEmpty() && position in musicFiles.indices) {
            createMediaPlayer(position)
            mediaPlayer?.start()
        }
    }

    fun start() {
        mediaPlayer?.start()
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun stop() {
        mediaPlayer?.stop()
    }

    fun release() {
        mediaPlayer?.release()
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun createMediaPlayer(positionInner: Int) {
        if (musicFiles.isEmpty() || positionInner !in musicFiles.indices) return

        position = positionInner
        val path = musicFiles[position].path ?: return
        uri = path.toUri()

        getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit {
            putString(MUSIC_FILE, uri.toString())
            putString(ARTIST_NAME, musicFiles[position].artist)
            putString(SONG_NAME, musicFiles[position].title)
        }

        mediaPlayer = MediaPlayer.create(baseContext, uri)
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun onCompleted() {
        mediaPlayer?.setOnCompletionListener(this)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        actionPlaying?.nextBtn()
        if (mediaPlayer != null && musicFiles.isNotEmpty() && position in musicFiles.indices) {
            createMediaPlayer(position)
            mediaPlayer?.start()
            onCompleted()
        }
    }

    fun setCallBack(actionPlaying: ActionPlaying) {
        this.actionPlaying = actionPlaying
    }

    private fun getAlbumArt(uri: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(baseContext, uri.toUri())
            val art = retriever.embeddedPicture
            retriever.release()
            art
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun nextBtnClicked() {
        actionPlaying?.nextBtn()
    }

    fun previousBtnClicked() {
        actionPlaying?.prevBtn()
    }

    fun playPauseBtnClicked() {
        actionPlaying?.playPauseBtn()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaSession?.release()
    }

    companion object {
        const val MUSIC_LAST_PLAYED = "LAST_PLAYED"
        const val MUSIC_FILE = "STORED_MUSIC"
        const val ARTIST_NAME = "ARTIST NAME"
        const val SONG_NAME = "SONG NAME"
    }
}