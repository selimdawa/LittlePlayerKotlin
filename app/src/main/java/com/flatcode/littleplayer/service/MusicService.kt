package com.flatcode.littleplayer.service

import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.os.CountDownTimer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.data.entity.EqualizerEntity
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.data.entity.RecentEntity
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.getAlbumArtBytes
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaSessionService(), Player.Listener {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var repository: MusicRoomRepository

    var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    var position = -1

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepTimer: CountDownTimer? = null

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var eqEnabled = false
    private var currentBandLevels = shortArrayOf(0, 0, 0, 0, 0)
    private var bassStrength: Short = 0
    private var virtualizerStrength: Short = 0
    private var currentPreset = "Custom"

    private val musicFileKey = stringPreferencesKey(DATA.MUSIC_FILE)
    private val artistNameKey = stringPreferencesKey(DATA.ARTIST_NAME)
    private val songNameKey = stringPreferencesKey(DATA.SONG_NAME)
    private val albumIdKey = stringPreferencesKey(DATA.ALBUM_ID)
    private val cachedImagePathKey = stringPreferencesKey(DATA.CACHED_IMAGE_PATH)

    private val customCommandFavorite = SessionCommand(COMMAND_FAVORITE, Bundle.EMPTY)
    private val customCommandPlaybackCycle = SessionCommand(COMMAND_PLAYBACK_CYCLE, Bundle.EMPTY)
    private val customCommandStop = SessionCommand(COMMAND_STOP_SERVICE, Bundle.EMPTY)

    override fun onCreate() {
        super.onCreate()

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(this@MusicService)
            repeatMode = Player.REPEAT_MODE_ALL
        }

        exoPlayer?.let { player ->
            mediaSession =
                MediaSession.Builder(this, player).setCallback(CustomSessionCallback()).build()

            loadEqualizerSettings()
            initAudioEffects(player.audioSessionId)
        }
    }

    private fun loadEqualizerSettings() {
        serviceScope.launch {
            repository.getEqualizerSettings()?.let { settings ->
                eqEnabled = settings.enabled
                bassStrength = settings.bassStrength
                virtualizerStrength = settings.virtualizerStrength
                currentPreset = settings.presetName
                currentBandLevels = settings.bandLevels.split(",").map { it.toShort() }.toShortArray()
                
                // If effects are already initialized, apply them
                applyEqualizerSettings()
            }
        }
    }

    private fun saveEqualizerSettings() {
        serviceScope.launch {
            repository.saveEqualizerSettings(
                EqualizerEntity(
                    enabled = eqEnabled,
                    bassStrength = bassStrength,
                    virtualizerStrength = virtualizerStrength,
                    bandLevels = currentBandLevels.joinToString(","),
                    presetName = currentPreset
                )
            )
        }
    }

    private fun applyEqualizerSettings() {
        equalizer?.enabled = eqEnabled
        bassBoost?.enabled = eqEnabled
        virtualizer?.enabled = eqEnabled

        if (eqEnabled) {
            try {
                bassBoost?.setStrength(bassStrength)
                virtualizer?.setStrength(virtualizerStrength)
                for (i in currentBandLevels.indices) {
                    equalizer?.setBandLevel(i.toShort(), currentBandLevels[i])
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initAudioEffects(sessionId: Int) {
        if (sessionId != -1) {
            try {
                equalizer?.release()
                bassBoost?.release()
                virtualizer?.release()

                equalizer = Equalizer(100, sessionId)
                bassBoost = BassBoost(100, sessionId)
                virtualizer = Virtualizer(100, sessionId)

                applyEqualizerSettings()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        initAudioEffects(audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun updateLastPlayedInfo() {
        val player = exoPlayer ?: return
        val currentMediaItem = player.currentMediaItem ?: return
        val metadata = currentMediaItem.mediaMetadata

        val songId = currentMediaItem.mediaId
        val title = metadata.title?.toString() ?: DATA.UNKNOWN
        val artist = metadata.artist?.toString() ?: DATA.UNKNOWN
        val album = metadata.albumTitle?.toString() ?: DATA.UNKNOWN
        val albumId = metadata.extras?.getString("ALBUM_ID")
        val cachedPath = metadata.extras?.getString("CACHED_IMAGE_PATH")
        val path = currentMediaItem.localConfiguration?.uri?.path ?: ""
        val currentIndex = player.currentMediaItemIndex

        serviceScope.launch(Dispatchers.IO) {
            repository.insertRecent(
                RecentEntity(
                    songId = songId,
                    title = title,
                    artist = artist,
                    album = album,
                    albumId = albumId,
                    duration = null,
                    path = path
                )
            )

            dataStore.edit { preferences ->
                preferences[musicFileKey] = path
                preferences[artistNameKey] = artist
                preferences[songNameKey] = title
                preferences[albumIdKey] = albumId ?: ""
                preferences[cachedImagePathKey] = cachedPath ?: ""
                preferences[intPreferencesKey(DATA.LAST_POSITION)] = currentIndex
            }
        }
    }

    private fun updateNotificationLayout() {
        val player = exoPlayer ?: return
        val currentMediaItem = player.currentMediaItem ?: return
        val songId = currentMediaItem.mediaId

        serviceScope.launch {
            val isFav = repository.isFavorite(songId)
            val favIcon = if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border

            val cycleIcon = when {
                player.shuffleModeEnabled -> R.drawable.ic_shuffle_on
                player.repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                else -> R.drawable.ic_repeat_on
            }

            val favoriteButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(customCommandFavorite)
                .setDisplayName(getString(R.string.favorite)).setCustomIconResId(favIcon).build()

            val cycleButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(customCommandPlaybackCycle)
                .setDisplayName(getString(R.string.cycle)).setCustomIconResId(cycleIcon).build()

            val stopButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(customCommandStop)
                .setDisplayName(getString(R.string.stop)).setCustomIconResId(R.drawable.ic_close).build()

            mediaSession?.setCustomLayout(listOf(favoriteButton, cycleButton, stopButton))
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return

        exoPlayer?.let {
            position = it.currentMediaItemIndex
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                loadArtForCurrentItem(it)
            }

            val currentId = it.currentMediaItem?.mediaId
            if (currentId != null) {
                serviceScope.launch {
                    repository.incrementPlayCount(currentId)
                }
            }
        }
        updateLastPlayedInfo()
        updateNotificationLayout()
    }

    private fun loadArtForCurrentItem(player: Player) {
        val currentMediaItem = player.currentMediaItem ?: return
        if (currentMediaItem.mediaMetadata.artworkData != null) return
        val path = currentMediaItem.localConfiguration?.uri?.path ?: return
        val cachedPath = currentMediaItem.mediaMetadata.extras?.getString("CACHED_IMAGE_PATH")

        serviceScope.launch(Dispatchers.IO) {
            val artBytes = if (!cachedPath.isNullOrEmpty()) {
                val file = File(cachedPath)
                if (file.exists()) file.readBytes() else getAlbumArtBytes(path)
            } else {
                getAlbumArtBytes(path)
            }

            if (artBytes != null) {
                val updatedMetadata = currentMediaItem.mediaMetadata.buildUpon()
                    .setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER).build()

                launch(Dispatchers.Main) {
                    if (player.currentMediaItem?.mediaId == currentMediaItem.mediaId) {
                        val index = player.currentMediaItemIndex
                        player.replaceMediaItem(
                            index,
                            currentMediaItem.buildUpon().setMediaMetadata(updatedMetadata).build()
                        )
                    }
                }
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_REPEAT_MODE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            updateNotificationLayout()
        }
    }

    private inner class CustomSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession, controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(customCommandFavorite)
                .add(customCommandPlaybackCycle)
                .add(customCommandStop)
                .add(SessionCommand(COMMAND_TOGGLE_EQ, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_EQ_BAND, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_BASS, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_VIRTUALIZER, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_PRESET, Bundle.EMPTY))
                .build()

            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands).build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                COMMAND_FAVORITE -> {
                    val currentMediaItem = session.player.currentMediaItem
                    if (currentMediaItem != null) {
                        val songId = currentMediaItem.mediaId
                        val title = currentMediaItem.mediaMetadata.title?.toString() ?: ""
                        val artist = currentMediaItem.mediaMetadata.artist?.toString() ?: ""
                        val album = currentMediaItem.mediaMetadata.albumTitle?.toString()
                        val path = currentMediaItem.localConfiguration?.uri?.toString() ?: ""

                        serviceScope.launch {
                            val isFav = repository.isFavorite(songId)
                            if (isFav) {
                                repository.deleteFavorite(
                                    FavoriteEntity(
                                        songId = songId,
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        duration = null,
                                        path = path
                                    )
                                )
                            } else {
                                repository.insertFavorite(
                                    FavoriteEntity(
                                        songId = songId,
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        duration = null,
                                        path = path
                                    )
                                )
                            }
                            updateNotificationLayout()
                        }
                    }
                }

                COMMAND_PLAYBACK_CYCLE -> {
                    val player = session.player
                    when {
                        !player.shuffleModeEnabled && player.repeatMode != Player.REPEAT_MODE_ONE -> {
                            player.repeatMode = Player.REPEAT_MODE_ONE
                            player.shuffleModeEnabled = false
                        }

                        !player.shuffleModeEnabled && player.repeatMode == Player.REPEAT_MODE_ONE -> {
                            player.repeatMode = Player.REPEAT_MODE_ALL
                            player.shuffleModeEnabled = true
                        }

                        else -> {
                            player.repeatMode = Player.REPEAT_MODE_ALL
                            player.shuffleModeEnabled = false
                        }
                    }
                    updateNotificationLayout()
                }

                COMMAND_SET_SLEEP_TIMER -> {
                    val minutes = args.getInt("MINUTES", 0)
                    startSleepTimer(minutes)
                }

                COMMAND_SET_EQ_BAND -> {
                    val band = args.getShort("BAND", 0)
                    val level = args.getShort("LEVEL", 0)
                    try {
                        if (band.toInt() in currentBandLevels.indices) {
                            currentBandLevels[band.toInt()] = level
                        }
                        equalizer?.setBandLevel(band, level)
                        saveEqualizerSettings()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                COMMAND_TOGGLE_EQ -> {
                    val enabled = args.getBoolean("ENABLED", false)
                    eqEnabled = enabled
                    equalizer?.enabled = enabled
                    bassBoost?.enabled = enabled
                    virtualizer?.enabled = enabled
                    saveEqualizerSettings()
                }

                COMMAND_SET_BASS -> {
                    val strength = args.getShort("STRENGTH", 0)
                    bassStrength = strength
                    bassBoost?.setStrength(strength)
                    saveEqualizerSettings()
                }

                COMMAND_SET_VIRTUALIZER -> {
                    val strength = args.getShort("STRENGTH", 0)
                    virtualizerStrength = strength
                    virtualizer?.setStrength(strength)
                    saveEqualizerSettings()
                }

                COMMAND_SET_PRESET -> {
                    currentPreset = args.getString("PRESET") ?: "Custom"
                    saveEqualizerSettings()
                }

                COMMAND_STOP_SERVICE -> {
                    stopPlaybackAndService()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun stopPlaybackAndService() {
        sleepTimer?.cancel()
        exoPlayer?.let {
            it.stop()
            it.clearMediaItems()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startSleepTimer(minutes: Int) {
        sleepTimer?.cancel()
        if (minutes <= 0) return

        sleepTimer = object : CountDownTimer(minutes * 60 * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                exoPlayer?.pause()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        serviceScope.cancel()
    }

    companion object {
        const val COMMAND_FAVORITE = "COMMAND_FAVORITE"
        const val COMMAND_PLAYBACK_CYCLE = "COMMAND_PLAYBACK_CYCLE"
        const val COMMAND_SET_SLEEP_TIMER = "COMMAND_SET_SLEEP_TIMER"
        const val COMMAND_SET_EQ_BAND = "COMMAND_SET_EQ_BAND"
        const val COMMAND_TOGGLE_EQ = "COMMAND_TOGGLE_EQ"
        const val COMMAND_SET_BASS = "COMMAND_SET_BASS"
        const val COMMAND_SET_VIRTUALIZER = "COMMAND_SET_VIRTUALIZER"
        const val COMMAND_STOP_SERVICE = "COMMAND_STOP_SERVICE"
        const val COMMAND_SET_PRESET = "COMMAND_SET_PRESET"
    }
}