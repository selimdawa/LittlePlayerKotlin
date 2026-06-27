package com.flatcode.littleplayer.service

import android.app.Service
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.unit.ActionPlaying
import com.flatcode.littleplayer.unit.DATA

class MusicService : Service(), MediaPlayer.OnCompletionListener {

    private val binder: IBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    var musicFiles = ArrayList<MusicFiles>()

    var uri: Uri? = null
    var position = -1

    private var actionPlaying: ActionPlaying? = null
    private var mediaSessionCompat: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        mediaSessionCompat = MediaSessionCompat(baseContext, "My Audio")
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
            val myPosition = it.getIntExtra(DATA.SERVICE_POSITION, -1)
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
        musicFiles = PlayerActivity.listSongs ?: ArrayList()
        position = startPosition

        mediaPlayer?.let {
            it.stop()
            it.release()
            if (musicFiles.isNotEmpty()) {
                createMediaPlayer(position)
                mediaPlayer?.start()
            }
        } ?: run {
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
        position = positionInner
        val path = musicFiles[position].path
        uri = Uri.parse(path)

        val editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit()
        editor.putString(MUSIC_FILE, uri.toString())
        editor.putString(ARTIST_NAME, musicFiles[position].artist)
        editor.putString(SONG_NAME, musicFiles[position].title)
        editor.apply()

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
        if (mediaPlayer != null) {
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
            retriever.setDataSource(uri)
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
        mediaSessionCompat?.release()
    }

    companion object {
        const val MUSIC_LAST_PLAYED = "LAST_PLAYED"
        const val MUSIC_FILE = "STORED_MUSIC"
        const val ARTIST_NAME = "ARTIST NAME"
        const val SONG_NAME = "SONG NAME"
    }
}