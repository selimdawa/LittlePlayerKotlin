package com.flatcode.littleplayer.service

import android.content.Intent
import android.os.Bundle
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
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.getAlbumArtBytes
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

    var musicFiles = ArrayList<MusicFiles>()
    var position = -1

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val musicFileKey = stringPreferencesKey(MUSIC_FILE)
    private val artistNameKey = stringPreferencesKey(ARTIST_NAME)
    private val songNameKey = stringPreferencesKey(SONG_NAME)

    private val customCommandFavorite = SessionCommand(COMMAND_FAVORITE, Bundle.EMPTY)
    private val customCommandPlaybackCycle = SessionCommand(COMMAND_PLAYBACK_CYCLE, Bundle.EMPTY)

    override fun onCreate() {
        super.onCreate()

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(this@MusicService)
        }

        exoPlayer?.let { player ->
            mediaSession =
                MediaSession.Builder(this, player).setCallback(CustomSessionCallback()).build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    fun createMediaPlayer(positionInner: Int) {
        if (musicFiles.isEmpty() || (positionInner !in musicFiles.indices)) return

        position = positionInner

        val mediaItems = musicFiles.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title ?: "Unknown Track")
                .setArtist(song.artist ?: "Unknown Artist")
                .setAlbumTitle(song.album ?: "Unknown Album")
                .build()

            MediaItem.Builder()
                .setUri(song.path?.toUri() ?: "".toUri())
                .setMediaMetadata(metadata)
                .setMediaId(song.id ?: "")
                .build()
        }

        exoPlayer?.apply {
            setMediaItems(mediaItems, position, 0L)
            prepare()
            play()
        }
    }

    private fun updateLastPlayedInfo() {
        val player = exoPlayer ?: return
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex in musicFiles.indices) {
            val song = musicFiles[currentIndex]
            serviceScope.launch(Dispatchers.IO) {
                dataStore.edit { preferences ->
                    preferences[musicFileKey] = song.path ?: ""
                    preferences[artistNameKey] = song.artist ?: "Unknown"
                    preferences[songNameKey] = song.title ?: "Unknown"
                }
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
                .setDisplayName(getString(R.string.favorite))
                .setCustomIconResId(favIcon)
                .build()

            val cycleButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(customCommandPlaybackCycle)
                .setDisplayName(getString(R.string.cycle))
                .setCustomIconResId(cycleIcon)
                .build()

            mediaSession?.setCustomLayout(listOf(favoriteButton, cycleButton))
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        // Avoid reloading if the transition was caused by a metadata update
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return

        exoPlayer?.let {
            position = it.currentMediaItemIndex
            // Only load art if we actually transitioned to a NEW song
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || 
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                loadArtForCurrentItem(it)
            }
        }
        updateLastPlayedInfo()
        updateNotificationLayout()
    }

    private fun loadArtForCurrentItem(player: Player) {
        val currentMediaItem = player.currentMediaItem ?: return
        // Guard: If artwork is already present, definitely skip to prevent infinite loop
        if (currentMediaItem.mediaMetadata.artworkData != null) return
        
        val path = currentMediaItem.localConfiguration?.uri?.path ?: return

        serviceScope.launch(Dispatchers.IO) {
            val artBytes = getAlbumArtBytes(path)
            if (artBytes != null) {
                val updatedMetadata = currentMediaItem.mediaMetadata.buildUpon()
                    .setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()

                launch(Dispatchers.Main) {
                    // Check if we are still on the same song before applying
                    if (player.currentMediaItem?.mediaId == currentMediaItem.mediaId) {
                        val index = player.currentMediaItemIndex
                        player.replaceMediaItem(index, 
                            currentMediaItem.buildUpon().setMediaMetadata(updatedMetadata).build())
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
                .add(customCommandFavorite).add(customCommandPlaybackCycle).build()

            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()

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
                                    com.flatcode.littleplayer.data.entity.FavoriteEntity(
                                        songId, title, artist, album, null, path
                                    )
                                )
                            } else {
                                repository.insertFavorite(
                                    com.flatcode.littleplayer.data.entity.FavoriteEntity(
                                        songId, title, artist, album, null, path
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
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        serviceScope.cancel()
    }

    companion object {
        const val MUSIC_FILE = "STORED_MUSIC"
        const val ARTIST_NAME = "ARTIST NAME"
        const val SONG_NAME = "SONG NAME"
        const val COMMAND_FAVORITE = "COMMAND_FAVORITE"
        const val COMMAND_PLAYBACK_CYCLE = "COMMAND_PLAYBACK_CYCLE"
    }
}