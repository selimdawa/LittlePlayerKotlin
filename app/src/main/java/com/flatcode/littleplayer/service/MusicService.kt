package com.flatcode.littleplayer.service

import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.cast.CastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.data.entity.EqualizerEntity
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.data.entity.PlaybackStateEntity
import com.flatcode.littleplayer.data.entity.RecentEntity
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.getAlbumArtBytes
import com.flatcode.littleplayer.utils.getDefaultArtBytes
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import io.selimdawa.multicolors.MultiColorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaLibraryService(), Player.Listener {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var repository: MusicRoomRepository

    @Inject
    lateinit var musicRepository: MusicRepository

    private var basePlayer: ExoPlayer? = null
    private var castPlayer: CastPlayer? = null
    var exoPlayer: Player? = null
    private var mediaSession: MediaLibrarySession? = null

    var position = -1

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepTimer: CountDownTimer? = null

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var eqEnabled = false
    private var currentBandLevels = shortArrayOf(0, 0, 0, 0, 0)
    private var customBandLevels = shortArrayOf(0, 0, 0, 0, 0)
    private var bassStrength: Short = 0
    private var virtualizerStrength: Short = 0
    private var currentPreset = "Custom"

    private var clickCount = 0
    private var updateLastPlayedJob: Job? = null
    private val clickHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val clickRunnable = Runnable { handleHeadsetClicks() }

    private val musicFileKey = stringPreferencesKey(DATA.MUSIC_FILE)
    private val artistNameKey = stringPreferencesKey(DATA.ARTIST_NAME)
    private val songNameKey = stringPreferencesKey(DATA.SONG_NAME)
    private val songIdKey = stringPreferencesKey(DATA.SONG_ID)
    private val durationKey = stringPreferencesKey(DATA.DURATION)
    private val albumIdKey = stringPreferencesKey(DATA.ALBUM_ID)
    private val cachedImagePathKey = stringPreferencesKey(DATA.CACHED_IMAGE_PATH)
    private val showSongToastKey = booleanPreferencesKey(DATA.SHOW_SONG_TOAST)
    private val playbackSpeedKey = floatPreferencesKey(DATA.PLAYBACK_SPEED)
    private val playbackPitchKey = floatPreferencesKey(DATA.PLAYBACK_PITCH)
    private val shuffleModeKey = booleanPreferencesKey(DATA.SHUFFLE_MODE)

    private val customCommandFavorite = SessionCommand(COMMAND_FAVORITE, Bundle.EMPTY)
    private val customCommandPlaybackCycle = SessionCommand(COMMAND_PLAYBACK_CYCLE, Bundle.EMPTY)
    private val customCommandStop = SessionCommand(COMMAND_STOP_SERVICE, Bundle.EMPTY)

    override fun onCreate() {
        super.onCreate()
        MultiColorManager.applyTheme(this)

        serviceScope.launch {
            MultiColorManager.currentThemeId.collect { _ ->
                MultiColorManager.applyTheme(this@MusicService)
                exoPlayer?.let { player ->
                    loadArtForCurrentItem(player, forceDefaultRefresh = true)
                }
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
            .build()

        basePlayer = ExoPlayer.Builder(this).setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_LOCAL).build().apply {
                addListener(this@MusicService)
                repeatMode = Player.REPEAT_MODE_ALL
            }

        try {
            castPlayer = CastPlayer.Builder(this).setLocalPlayer(basePlayer!!).build()
            castPlayer?.addListener(this@MusicService)
        } catch (_: Exception) {
        }

        val primaryPlayer = castPlayer ?: basePlayer!!

        exoPlayer = object : ForwardingPlayer(primaryPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.availableCommands.buildUpon()
                    .add(COMMAND_SEEK_TO_NEXT)
                    .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(COMMAND_SEEK_TO_PREVIOUS)
                    .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, 
                    COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> mediaItemCount > 0
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getNextMediaItemIndex(): Int {
                val count = mediaItemCount
                if (count <= 0) return -1
                val next = super.nextMediaItemIndex
                return if (next == -1) (currentMediaItemIndex + 1) % count else next
            }

            override fun getPreviousMediaItemIndex(): Int {
                val count = mediaItemCount
                if (count <= 0) return -1
                val prev = super.previousMediaItemIndex
                return if (prev == -1) (currentMediaItemIndex - 1 + count) % count else prev
            }

            override fun seekToNextMediaItem() {
                val nextIndex = nextMediaItemIndex
                if (nextIndex != -1) {
                    seekToDefaultPosition(nextIndex)
                }
            }

            override fun seekToPreviousMediaItem() {
                val prevIndex = previousMediaItemIndex
                if (prevIndex != -1) {
                    seekToDefaultPosition(prevIndex)
                }
            }

            override fun seekToNext() = seekToNextMediaItem()
            override fun seekToPrevious() = seekToPreviousMediaItem()
        }

        exoPlayer?.let { player ->
            mediaSession =
                MediaLibrarySession.Builder(this, player, CustomLibrarySessionCallback()).build()

            loadEqualizerSettings()
            initAudioEffects(basePlayer?.audioSessionId ?: -1)
            loadPlaybackStateAndQueue()
            loadPlaybackParameters()
            observeShuffleMode()
        }
    }

    private fun observeShuffleMode() {
        serviceScope.launch {
            musicRepository.shuffleMode.collect { enabled ->
                exoPlayer?.let { player ->
                    if (player.shuffleModeEnabled != enabled) {
                        player.shuffleModeEnabled = enabled
                        if (enabled) {
                            basePlayer?.setShuffleOrder(androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder(player.mediaItemCount))
                        }
                    }
                }
            }
        }
    }

    private fun loadPlaybackParameters() {
        serviceScope.launch {
            dataStore.data.first().let { prefs ->
                val speed = prefs[playbackSpeedKey] ?: 1.0f
                val pitch = prefs[playbackPitchKey] ?: 1.0f
                exoPlayer?.playbackParameters = PlaybackParameters(speed, pitch)
            }
        }
    }

    private fun loadPlaybackStateAndQueue() {
        serviceScope.launch {
            val queue = repository.getQueue()
            val state = repository.getPlaybackStateSync()

            if (queue.isNotEmpty() && (exoPlayer?.mediaItemCount ?: 0) == 0) {
                val mediaItems = queue.map { item ->
                    val uri = item.path?.toUri() ?: "".toUri()
                    val metadata =
                        MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist)
                            .setExtras(Bundle().apply {
                                putString("ALBUM_ID", item.albumId)
                                putString("CACHED_IMAGE_PATH", item.cachedImagePath)
                            }).build()
                    MediaItem.Builder().setUri(uri).setMediaId(item.songId)
                        .setMediaMetadata(metadata).build()
                }

                val startItemIndex = if (state != null && !state.currentSongId.isNullOrEmpty()) {
                    val index = queue.indexOfFirst { it.songId == state.currentSongId }
                    if (index != -1) index else (if (state.lastPosition in queue.indices) state.lastPosition else 0)
                } else if (state != null && state.lastPosition in queue.indices) {
                    state.lastPosition
                } else 0

                val startPosition = state?.lastProgress ?: 0L

                exoPlayer?.let { player ->
                    player.setMediaItems(mediaItems, startItemIndex, startPosition)
                    player.shuffleModeEnabled = state?.shuffleModeEnabled ?: false
                    player.repeatMode = state?.repeatMode ?: Player.REPEAT_MODE_ALL
                    player.prepare()
                    loadArtForCurrentItem(player)
                }
            } else if (state != null) {
                exoPlayer?.let { player ->
                    player.shuffleModeEnabled = state.shuffleModeEnabled
                    player.repeatMode = state.repeatMode
                }
            }
        }
    }

    private fun loadEqualizerSettings() {
        serviceScope.launch {
            repository.getEqualizerSettings().collect { settings ->
                settings?.let {
                    eqEnabled = it.enabled
                    bassStrength = it.bassStrength
                    virtualizerStrength = it.virtualizerStrength
                    currentPreset = it.presetName
                    currentBandLevels =
                        it.bandLevels.split(",").map { b -> b.toShort() }.toShortArray()
                    customBandLevels =
                        it.customBandLevels.split(",").map { b -> b.toShort() }.toShortArray()

                    applyEqualizerSettings()
                }
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
                    customBandLevels = customBandLevels.joinToString(","),
                    presetName = currentPreset
                )
            )
        }
    }

    private fun applyEqualizerSettings() {
        equalizer?.enabled = eqEnabled
        bassBoost?.enabled = eqEnabled

        val behavior = if (eqEnabled && virtualizerStrength > 0) {
            C.SPATIALIZATION_BEHAVIOR_AUTO
        } else {
            C.SPATIALIZATION_BEHAVIOR_NEVER
        }

        exoPlayer?.let { player ->
            val currentAttrs = player.audioAttributes
            if (currentAttrs.spatializationBehavior != behavior) {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(currentAttrs.contentType)
                        .setFlags(currentAttrs.flags)
                        .setUsage(currentAttrs.usage)
                        .setSpatializationBehavior(behavior)
                        .build(),
                    false
                )
            }
        }

        if (eqEnabled) {
            try {
                bassBoost?.setStrength(bassStrength)
                for (i in currentBandLevels.indices) {
                    equalizer?.setBandLevel(i.toShort(), currentBandLevels[i])
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun initAudioEffects(sessionId: Int) {
        if (sessionId != -1) {
            try {
                equalizer?.release()
                bassBoost?.release()

                equalizer = Equalizer(100, sessionId)
                bassBoost = BassBoost(100, sessionId)

                applyEqualizerSettings()
            } catch (_: Exception) {
            }
        }
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        initAudioEffects(audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                exoPlayer?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
            }

            ACTION_NEXT -> exoPlayer?.seekToNext()
            ACTION_PREV -> exoPlayer?.seekToPrevious()
            ACTION_SHUFFLE -> {
                exoPlayer?.let {
                    val nextShuffle = !it.shuffleModeEnabled
                    serviceScope.launch {
                        musicRepository.saveShuffleMode(nextShuffle)
                    }
                }
            }

            ACTION_FAVORITE -> {
                exoPlayer?.let { player ->
                    val currentMediaItem = player.currentMediaItem
                    if (currentMediaItem != null) {
                        val songId = currentMediaItem.mediaId
                        serviceScope.launch {
                            val isFav = repository.isFavorite(songId)
                            if (isFav) {
                                repository.deleteFavoriteById(songId)
                            } else {
                                val metadata = currentMediaItem.mediaMetadata
                                repository.insertFavorite(
                                    FavoriteEntity(
                                        songId = songId,
                                        title = metadata.title?.toString() ?: "",
                                        artist = metadata.artist?.toString() ?: "",
                                        album = metadata.albumTitle?.toString(),
                                        duration = player.duration.let { if (it > 0) it.toString() else null },
                                        path = currentMediaItem.localConfiguration?.uri?.toString()
                                            ?: ""
                                    )
                                )
                            }
                            sendWidgetUpdate()
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun updateLastPlayedInfo(immediate: Boolean = false) {
        val player = exoPlayer ?: return
        val currentMediaItem = player.currentMediaItem ?: return
        val metadata = currentMediaItem.mediaMetadata

        val songId = currentMediaItem.mediaId
        val title = metadata.title?.toString() ?: DATA.UNKNOWN
        val artist = metadata.artist?.toString() ?: DATA.UNKNOWN
        val album = metadata.albumTitle?.toString() ?: DATA.UNKNOWN
        val albumId = metadata.extras?.getString("ALBUM_ID")
        val duration = player.duration.let { if (it > 0) it.toString() else null }
        val cachedPath = metadata.extras?.getString("CACHED_IMAGE_PATH")
        val path = currentMediaItem.localConfiguration?.uri?.path ?: ""
        val currentIndex = player.currentMediaItemIndex
        val shuffleMode = player.shuffleModeEnabled
        val repeatMode = player.repeatMode
        val currentProgress = player.currentPosition

        updateLastPlayedJob?.cancel()
        updateLastPlayedJob = serviceScope.launch(Dispatchers.IO) {
            if (!immediate) {
                delay(1.seconds)
            }
            repository.insertRecent(
                RecentEntity(
                    songId = songId,
                    title = title,
                    artist = artist,
                    album = album,
                    albumId = albumId,
                    duration = duration,
                    path = path
                )
            )

            repository.savePlaybackState(
                PlaybackStateEntity(
                    currentSongId = songId,
                    lastPosition = currentIndex,
                    shuffleModeEnabled = shuffleMode,
                    repeatMode = repeatMode,
                    lastProgress = currentProgress
                )
            )

            dataStore.edit { preferences ->
                preferences[musicFileKey] = path
                preferences[artistNameKey] = artist
                preferences[songNameKey] = title
                preferences[songIdKey] = songId
                preferences[durationKey] = duration ?: ""
                preferences[albumIdKey] = albumId ?: ""
                preferences[cachedImagePathKey] = cachedPath ?: ""
                preferences[intPreferencesKey(DATA.LAST_POSITION)] = currentIndex
                preferences[shuffleModeKey] = shuffleMode
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
                player.shuffleModeEnabled -> R.drawable.ic_shuffle_on_24
                player.repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_24
                else -> R.drawable.ic_repeat_24
            }

            val favoriteButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(customCommandFavorite)
                .setDisplayName(getString(R.string.favorite)).setCustomIconResId(favIcon).build()

            val cycleButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(customCommandPlaybackCycle)
                .setDisplayName(getString(R.string.cycle)).setCustomIconResId(cycleIcon).build()

            val stopButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(customCommandStop).setDisplayName(getString(R.string.stop))
                .setCustomIconResId(R.drawable.ic_close_24).build()

            mediaSession?.setCustomLayout(listOf(favoriteButton, cycleButton, stopButton))
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            updateLastPlayedInfo()
        }
        sendWidgetUpdate()
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        serviceScope.launch {
            dataStore.edit { prefs ->
                prefs[playbackSpeedKey] = playbackParameters.speed
                prefs[playbackPitchKey] = playbackParameters.pitch
            }
        }
    }

    private fun sendWidgetUpdate() {
        val intent = Intent(ACTION_UPDATE_WIDGET)
        intent.setPackage(packageName)
        exoPlayer?.let { player ->
            val metadata = player.mediaMetadata
            val songId = player.currentMediaItem?.mediaId ?: ""
            intent.putExtra("title", metadata.title?.toString() ?: getString(R.string.song_title))
            intent.putExtra(
                "artist", metadata.artist?.toString() ?: getString(R.string.artist_name)
            )
            intent.putExtra("isPlaying", player.isPlaying)
            intent.putExtra("isShuffle", player.shuffleModeEnabled)

            serviceScope.launch {
                val isFav = if (songId.isNotEmpty()) repository.isFavorite(songId) else false
                intent.putExtra("isFavorite", isFav)

                val cachedPath = metadata.extras?.getString("CACHED_IMAGE_PATH")
                if (!cachedPath.isNullOrEmpty()) {
                    intent.putExtra("imagePath", cachedPath)
                    sendBroadcast(intent)
                } else {
                    val pathFromStore = dataStore.data.map { it[cachedImagePathKey] }.first()
                    intent.putExtra("imagePath", pathFromStore)
                    sendBroadcast(intent)
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        updateLastPlayedInfo(immediate = true)
        super.onTaskRemoved(rootIntent)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)

        exoPlayer?.let {
            position = it.currentMediaItemIndex
            loadArtForCurrentItem(it)

            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && !it.playWhenReady) {
                it.play()
            }

            val currentId = it.currentMediaItem?.mediaId
            if (currentId != null) {
                serviceScope.launch {
                    repository.incrementPlayCount(currentId)
                }
            }

            mediaItem?.mediaMetadata?.title?.let { title ->
                serviceScope.launch {
                    val showToast = dataStore.data.map { preferences ->
                        preferences[showSongToastKey] ?: false
                    }.first()
                    if (showToast) {
                        Toast.makeText(this@MusicService, title, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        updateLastPlayedInfo()
        updateNotificationLayout()
        sendWidgetUpdate()
    }

    private fun loadArtForCurrentItem(player: Player, forceDefaultRefresh: Boolean = false) {
        val currentMediaItem = player.currentMediaItem ?: return
        val extras = currentMediaItem.mediaMetadata.extras ?: Bundle.EMPTY
        val isCurrentlyDefault = extras.getBoolean("IS_DEFAULT_ART", false)

        if (currentMediaItem.mediaMetadata.artworkData != null && !forceDefaultRefresh) return
        if (forceDefaultRefresh && !isCurrentlyDefault) return

        val path = currentMediaItem.localConfiguration?.uri?.path ?: return
        val cachedPath = extras.getString("CACHED_IMAGE_PATH")

        serviceScope.launch(Dispatchers.IO) {
            val realArtBytes = if (!cachedPath.isNullOrEmpty()) {
                val file = File(cachedPath)
                if (file.exists()) file.readBytes() else getAlbumArtBytes(path)
            } else {
                getAlbumArtBytes(path)
            }

            val isDefault = realArtBytes == null
            val artBytes = realArtBytes ?: getDefaultArtBytes(this@MusicService)

            if (artBytes != null && artBytes.isNotEmpty()) {
                val updatedMetadata = currentMediaItem.mediaMetadata.buildUpon()
                    .setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .setExtras(Bundle(extras).apply {
                        putBoolean("IS_DEFAULT_ART", isDefault)
                    }).build()

                launch(Dispatchers.Main) {
                    if (player.currentMediaItem?.mediaId == currentMediaItem.mediaId) {
                        val index = player.currentMediaItemIndex
                        player.replaceMediaItem(
                            index,
                            currentMediaItem.buildUpon().setMediaMetadata(updatedMetadata).build()
                        )
                        sendWidgetUpdate()
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
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_MEDIA_METADATA_CHANGED
            )
        ) {
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) && player.playbackState == Player.STATE_READY) {
                loadArtForCurrentItem(player)
            }
            updateNotificationLayout()
            if (events.containsAny(
                    Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED
                )
            ) {
                updateLastPlayedInfo()
            }
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED
                )
            ) {
                sendWidgetUpdate()
            }
        }
    }

    private inner class CustomLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder().setMediaId(ROOT_ID).setMediaMetadata(
                MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setTitle(getString(R.string.app_name)).build()
            ).build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
            val executor = MoreExecutors.listeningDecorator(java.util.concurrent.Executors.newSingleThreadExecutor())
            return executor.submit<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
                try {
                    when (parentId) {
                        ROOT_ID -> {
                            val children = listOf(
                                createBrowsableItem(
                                    CATEGORY_SONGS,
                                    getString(R.string.songs),
                                    MediaMetadata.MEDIA_TYPE_MUSIC
                                ), createBrowsableItem(
                                    CATEGORY_ALBUMS,
                                    getString(R.string.albums),
                                    MediaMetadata.MEDIA_TYPE_ALBUM
                                ), createBrowsableItem(
                                    CATEGORY_ARTISTS,
                                    getString(R.string.artists),
                                    MediaMetadata.MEDIA_TYPE_ARTIST
                                ), createBrowsableItem(
                                    CATEGORY_PLAYLISTS,
                                    getString(R.string.playlists),
                                    MediaMetadata.MEDIA_TYPE_PLAYLIST
                                ), createBrowsableItem(
                                    CATEGORY_FAVORITES,
                                    getString(R.string.favourites),
                                    MediaMetadata.MEDIA_TYPE_PLAYLIST
                                ), createBrowsableItem(
                                    CATEGORY_RECENT,
                                    getString(R.string.recent),
                                    MediaMetadata.MEDIA_TYPE_PLAYLIST
                                )
                            )
                            LibraryResult.ofItemList(children, params)
                        }

                        CATEGORY_SONGS -> {
                            val songs = runBlocking { musicRepository.getAllAudio(DATA.SORT_BY_NAME) }
                            val mediaItems = songs.map { it.toMediaItem() }
                            LibraryResult.ofItemList(mediaItems, params)
                        }

                        CATEGORY_ALBUMS -> {
                            val songs = runBlocking { musicRepository.getAllAudio(DATA.SORT_BY_NAME) }
                            val albums = songs.groupBy { it.album ?: DATA.UNKNOWN }
                            val albumItems = albums.keys.map { albumName ->
                                createBrowsableItem(
                                    "album|$albumName", albumName, MediaMetadata.MEDIA_TYPE_ALBUM
                                )
                            }
                            LibraryResult.ofItemList(albumItems, params)
                        }

                        CATEGORY_ARTISTS -> {
                            val songs = runBlocking { musicRepository.getAllAudio(DATA.SORT_BY_NAME) }
                            val artists = songs.groupBy { it.artist ?: DATA.UNKNOWN }
                            val artistItems = artists.keys.map { artistName ->
                                createBrowsableItem(
                                    "artist|$artistName", artistName, MediaMetadata.MEDIA_TYPE_ARTIST
                                )
                            }
                            LibraryResult.ofItemList(artistItems, params)
                        }

                        CATEGORY_FAVORITES -> {
                            val favorites = runBlocking { repository.getAllFavorites().first() }
                            val mediaItems = favorites.map { it.toMediaItem() }
                            LibraryResult.ofItemList(mediaItems, params)
                        }

                        CATEGORY_RECENT -> {
                            val recent = runBlocking { repository.getAllRecent().first() }
                            val mediaItems = recent.map { it.toMediaItem() }
                            LibraryResult.ofItemList(mediaItems, params)
                        }

                        else -> {
                            if (parentId.startsWith("album|")) {
                                val albumName = parentId.removePrefix("album|")
                                val songs = runBlocking { musicRepository.getAllAudio(DATA.SORT_BY_NAME) }
                                val mediaItems =
                                    songs.filter { it.album == albumName }.map { it.toMediaItem() }
                                LibraryResult.ofItemList(mediaItems, params)
                            } else if (parentId.startsWith("artist|")) {
                                val artistName = parentId.removePrefix("artist|")
                                val songs = runBlocking { musicRepository.getAllAudio(DATA.SORT_BY_NAME) }
                                val mediaItems =
                                    songs.filter { it.artist == artistName }.map { it.toMediaItem() }
                                LibraryResult.ofItemList(mediaItems, params)
                            } else {
                                LibraryResult.ofItemList(listOf(), params)
                            }
                        }
                    }
                } catch (e: Exception) {
                    LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
                } finally {
                    executor.shutdown()
                }
            }
        }

        private fun RecentEntity.toMediaItem(): MediaItem {
            return MediaItem.Builder().setMediaId(this.songId).setUri(this.path.toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(this.title).setArtist(this.artist)
                        .setAlbumTitle(this.album).setIsBrowsable(false).setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()
                ).build()
        }

        private fun createBrowsableItem(id: String, title: String, mediaType: Int): MediaItem {
            return MediaItem.Builder().setMediaId(id).setMediaMetadata(
                MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false)
                    .setMediaType(mediaType).setTitle(title).build()
            ).build()
        }

        private fun MusicFiles.toMediaItem(): MediaItem {
            val uri = this.path?.toUri() ?: "".toUri()
            return MediaItem.Builder().setMediaId(this.id ?: "").setUri(uri).setMediaMetadata(
                MediaMetadata.Builder().setTitle(this.title).setArtist(this.artist)
                    .setAlbumTitle(this.album).setIsBrowsable(false).setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).setExtras(Bundle().apply {
                        putString("ALBUM_ID", this@toMediaItem.albumId)
                        putString("CACHED_IMAGE_PATH", this@toMediaItem.cachedImagePath)
                    }).build()
            ).build()
        }

        private fun FavoriteEntity.toMediaItem(): MediaItem {
            return MediaItem.Builder().setMediaId(this.songId).setUri(this.path.toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(this.title).setArtist(this.artist)
                        .setAlbumTitle(this.album).setIsBrowsable(false).setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()
                ).build()
        }

        override fun onConnect(
            session: MediaSession, controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(customCommandFavorite).add(customCommandPlaybackCycle).add(customCommandStop)
                .add(SessionCommand(COMMAND_TOGGLE_EQ, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_EQ_BAND, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_BASS, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_VIRTUALIZER, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SET_PRESET, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_SAVE_EQ_SETTINGS, Bundle.EMPTY)).build()

            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands).build()
        }

        override fun onMediaButtonEvent(
            session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent
        ): Boolean {
            val ke = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, android.view.KeyEvent::class.java)
            if (ke != null && ke.action == android.view.KeyEvent.ACTION_DOWN) {
                when (ke.keyCode) {
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, android.view.KeyEvent.KEYCODE_HEADSETHOOK -> {
                        clickCount++
                        clickHandler.removeCallbacks(clickRunnable)
                        clickHandler.postDelayed(clickRunnable, 300)
                        return true
                    }
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
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
                        val duration =
                            session.player.duration.let { if (it > 0) it.toString() else null }
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
                                        duration = duration,
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
                                        duration = duration,
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
                            serviceScope.launch {
                                musicRepository.saveShuffleMode(false)
                            }
                        }

                        !player.shuffleModeEnabled && player.repeatMode == Player.REPEAT_MODE_ONE -> {
                            player.repeatMode = Player.REPEAT_MODE_ALL
                            serviceScope.launch {
                                musicRepository.saveShuffleMode(true)
                            }
                        }

                        else -> {
                            player.repeatMode = Player.REPEAT_MODE_ALL
                            serviceScope.launch {
                                musicRepository.saveShuffleMode(false)
                            }
                        }
                    }
                    updateNotificationLayout()
                }

                COMMAND_SET_SLEEP_TIMER -> {
                    val minutes = args.getInt("MINUTES", 0)
                    startSleepTimer(minutes)
                }

                COMMAND_SAVE_EQ_SETTINGS -> {
                    saveEqualizerSettings()
                }

                COMMAND_SET_EQ_BAND -> {
                    val band = args.getShort("BAND", 0)
                    val level = args.getShort("LEVEL", 0)
                        try {
                            if (band.toInt() in currentBandLevels.indices) {
                                currentBandLevels[band.toInt()] = level
                                if (currentPreset == "Custom") {
                                    customBandLevels[band.toInt()] = level
                                }
                            }
                            equalizer?.setBandLevel(band, level)
                        } catch (_: Exception) {
                        }
                }

                COMMAND_TOGGLE_EQ -> {
                    val enabled = args.getBoolean("ENABLED", false)
                    eqEnabled = enabled
                    applyEqualizerSettings()
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
                    applyEqualizerSettings()
                    saveEqualizerSettings()
                }

                COMMAND_SET_PRESET -> {
                    currentPreset = args.getString("PRESET") ?: "Custom"
                    if (currentPreset == "Custom") {
                        System.arraycopy(
                            customBandLevels, 0, currentBandLevels, 0, customBandLevels.size
                        )
                        applyEqualizerSettings()
                    }
                    saveEqualizerSettings()
                }

                COMMAND_STOP_SERVICE -> {
                    stopPlaybackAndService()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun handleHeadsetClicks() {
        val count = clickCount
        clickCount = 0
        serviceScope.launch {
            val action = when (count) {
                1 -> DATA.ACTION_PLAY_PAUSE_TOGGLE
                2 -> dataStore.data.map {
                    it[stringPreferencesKey(DATA.HEADSET_DOUBLE_CLICK_ACTION)]
                }.first() ?: DATA.ACTION_NEXT_TRACK

                3 -> dataStore.data.map {
                    it[stringPreferencesKey(DATA.HEADSET_TRIPLE_CLICK_ACTION)]
                }.first() ?: DATA.ACTION_PREV_TRACK

                else -> DATA.ACTION_NONE
            }
            executeHeadsetAction(action)
        }
    }

    private fun executeHeadsetAction(action: String) {
        val player = exoPlayer ?: return
        when (action) {
            DATA.ACTION_PLAY_PAUSE_TOGGLE -> {
                if (player.isPlaying) player.pause() else player.play()
            }

            DATA.ACTION_NEXT_TRACK -> player.seekToNext()
            DATA.ACTION_PREV_TRACK -> player.seekToPrevious()
            DATA.ACTION_FAST_FORWARD -> player.seekForward()
            DATA.ACTION_REWIND -> player.seekBack()
            DATA.ACTION_FAVORITE_TOGGLE -> {
                val currentMediaItem = player.currentMediaItem
                if (currentMediaItem != null) {
                    val songId = currentMediaItem.mediaId
                    serviceScope.launch {
                        val isFav = repository.isFavorite(songId)
                        if (isFav) {
                            repository.deleteFavoriteById(songId)
                        } else {
                            val metadata = currentMediaItem.mediaMetadata
                            repository.insertFavorite(
                                FavoriteEntity(
                                    songId = songId,
                                    title = metadata.title?.toString() ?: "",
                                    artist = metadata.artist?.toString() ?: "",
                                    album = metadata.albumTitle?.toString(),
                                    duration = player.duration.let { if (it > 0) it.toString() else null },
                                    path = currentMediaItem.localConfiguration?.uri?.toString()
                                        ?: ""
                                )
                            )
                        }
                        updateNotificationLayout()
                        sendWidgetUpdate()
                    }
                }
            }
        }
    }

    private fun stopPlaybackAndService() {
        sleepTimer?.cancel()
        updateLastPlayedInfo(immediate = true)
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
        castPlayer?.release()
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
        const val COMMAND_SAVE_EQ_SETTINGS = "COMMAND_SAVE_EQ_SETTINGS"
        const val COMMAND_TOGGLE_EQ = "COMMAND_TOGGLE_EQ"
        const val COMMAND_SET_BASS = "COMMAND_SET_BASS"
        const val COMMAND_SET_VIRTUALIZER = "COMMAND_SET_VIRTUALIZER"
        const val COMMAND_STOP_SERVICE = "COMMAND_STOP_SERVICE"
        const val COMMAND_SET_PRESET = "COMMAND_SET_PRESET"

        const val ACTION_UPDATE_WIDGET = "com.flatcode.littleplayer.ACTION_UPDATE_WIDGET"
        const val ACTION_PLAY_PAUSE = "com.flatcode.littleplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.flatcode.littleplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.flatcode.littleplayer.ACTION_PREV"
        const val ACTION_SHUFFLE = "com.flatcode.littleplayer.ACTION_SHUFFLE"
        const val ACTION_FAVORITE = "com.flatcode.littleplayer.ACTION_FAVORITE"

        private const val ROOT_ID = "root"
        private const val CATEGORY_SONGS = "songs"
        private const val CATEGORY_ALBUMS = "albums"
        private const val CATEGORY_ARTISTS = "artists"
        private const val CATEGORY_PLAYLISTS = "playlists"
        private const val CATEGORY_FAVORITES = "favorites"
        private const val CATEGORY_RECENT = "recent"
    }
}