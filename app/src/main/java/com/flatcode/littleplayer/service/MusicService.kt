package com.flatcode.littleplayer.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.ActionPlaying
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicService : Service(), Player.Listener {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private val binder: IBinder = MyBinder()
    var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    var musicFiles = ArrayList<MusicFiles>()
    var uri: Uri? = null
    var position = -1

    private var actionPlaying: ActionPlaying? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val MUSIC_FILE_KEY = stringPreferencesKey(MUSIC_FILE)
    private val ARTIST_NAME_KEY = stringPreferencesKey(ARTIST_NAME)
    private val SONG_NAME_KEY = stringPreferencesKey(SONG_NAME)

    override fun onCreate() {
        super.onCreate()

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(this@MusicService)
        }

        exoPlayer?.let { player ->
            mediaSession = MediaSession.Builder(this, player).build()
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
                it.getSerializableExtra(DATA.SERVICE_POSITION, Int::class.java) ?: -1
            } else {
                it.getIntExtra(DATA.SERVICE_POSITION, -1)
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
        if (musicFiles.isNotEmpty() && position in musicFiles.indices) {
            createMediaPlayer(position)
            start()
        }
    }

    fun start() {
        exoPlayer?.playWhenReady = true
        exoPlayer?.prepare()
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    fun stop() {
        exoPlayer?.stop()
    }

    fun release() {
        exoPlayer?.release()
    }

    fun getDuration(): Int {
        return exoPlayer?.duration?.toInt() ?: 0
    }

    fun seekTo(position: Int) {
        exoPlayer?.seekTo(position.toLong())
    }

    fun getCurrentPosition(): Int {
        return exoPlayer?.currentPosition?.toInt() ?: 0
    }

    fun getAudioSessionId(): Int {
        return exoPlayer?.audioSessionId ?: 0
    }

    fun createMediaPlayer(positionInner: Int) {
        if (musicFiles.isEmpty() || positionInner !in musicFiles.indices) return

        position = positionInner
        val song = musicFiles[position]
        val path = song.path ?: return
        uri = path.toUri()

        serviceScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[MUSIC_FILE_KEY] = uri.toString()
                preferences[ARTIST_NAME_KEY] = song.artist ?: "Unknown"
                preferences[SONG_NAME_KEY] = song.title ?: "Unknown"
            }
        }

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(song.title ?: "Unknown Track")
            .setArtist(song.artist ?: "Unknown Artist")
            .setAlbumTitle(song.album ?: "Unknown Album")
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(mediaMetadata)
            .build()

        exoPlayer?.stop()
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
    }

    fun pause() {
        exoPlayer?.playWhenReady = false
    }

    fun onCompleted() {

    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            actionPlaying?.nextBtn()
            if (musicFiles.isNotEmpty() && position in musicFiles.indices) {
                createMediaPlayer(position)
                start()
            }
        }
    }

    fun setCallBack(actionPlaying: ActionPlaying) {
        this.actionPlaying = actionPlaying
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
        mediaSession?.release()
        exoPlayer?.removeListener(this)
        exoPlayer?.release()
        serviceScope.cancel()
    }

    companion object {
        const val MUSIC_FILE = "STORED_MUSIC"
        const val ARTIST_NAME = "ARTIST NAME"
        const val SONG_NAME = "SONG NAME"
    }
}