package com.flatcode.littleplayer.adapter

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.flatcode.littleplayer.R
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.PlaybackAnimatable
import com.flatcode.littleplayer.utils.ensureBrightColor
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.gone
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.visible

abstract class BaseMusicAdapter<VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<MusicFiles>,
) : ListAdapter<MusicFiles, VH>(diffCallback), PlaybackAnimatable {

    protected var playingPath: String? = null
    protected var isPlaying: Boolean = false
    protected var currentThemeMode: Int = DATA.MODE_BASIC
    protected var currentThemeColor: Int = Color.WHITE
    protected var currentThemeColorSecond: Int = Color.WHITE
    protected var listItemThemeEnabled: Boolean = false

    private var colorOnSurface: Int = Color.GRAY
    private var mcTrack: Int = Color.GRAY
    private var mcTick: Int = Color.GRAY

    private fun initColors(context: Context) {
        if (colorOnSurface == Color.GRAY) {
            colorOnSurface = context.getLibraryColor("colorOnSurface")
            mcTrack = context.getLibraryColor("mc_track")
            mcTick = context.getLibraryColor("mc_tick")
        }
    }

    override fun updatePlaybackState(path: String?, isPlaying: Boolean) {
        val oldPath = this.playingPath
        val oldPlaying = this.isPlaying

        this.playingPath = path
        this.isPlaying = isPlaying

        if ((oldPath != path) || (oldPlaying != isPlaying)) {
            currentList.forEachIndexed { index, musicFiles ->
                if ((musicFiles.path == oldPath) || (musicFiles.path == path)) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    override fun updateThemeState(mode: Int, color: Int, colorSecond: Int) {
        val oldMode = this.currentThemeMode
        val oldColor = this.currentThemeColor
        val oldColorSecond = this.currentThemeColorSecond

        this.currentThemeMode = mode
        this.currentThemeColor = color
        this.currentThemeColorSecond = colorSecond

        if (listItemThemeEnabled && ((oldMode != mode) || (oldColor != color) || (oldColorSecond != colorSecond))) {
            // Only update the playing item to avoid full list refresh
            currentList.forEachIndexed { index, musicFiles ->
                if (musicFiles.path == playingPath) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    override fun updateListThemeState(enabled: Boolean) {
        val old = this.listItemThemeEnabled
        this.listItemThemeEnabled = enabled
        if (old != enabled) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    protected fun ItemMusicBinding.applyTheme(context: Context, song: MusicFiles) {
        initColors(context)
        val isCurrentlyPlaying = song.path == playingPath
        
        // Ensure blur is always visible and loaded for all items
        imageBlur.visibility = android.view.View.VISIBLE
        imageBlur.loadSongImageBlur(song.albumId, 25, song.path, song.cachedImagePath, song.album)

        if (isCurrentlyPlaying) {
            if (wave.visibility != android.view.View.VISIBLE) {
                wave.visible()
                wave.start()
            }

            val trackColor = if (listItemThemeEnabled) {
                when (currentThemeMode) {
                    DATA.MODE_PALETTE -> currentThemeColor.ensureBrightColor()
                    DATA.MODE_WHITE -> ContextCompat.getColor(context, R.color.white)
                    else -> mcTrack
                }
            } else {
                mcTrack
            }

            val closeColor = if (listItemThemeEnabled) {
                when (currentThemeMode) {
                    DATA.MODE_PALETTE -> currentThemeColorSecond.ensureBrightColor()
                    DATA.MODE_WHITE -> ContextCompat.getColor(context, R.color.white)
                    else -> mcTick
                }
            } else {
                mcTick
            }

            wave.startColor = trackColor
            wave.closeColor = closeColor
            songName.setTextColor(trackColor)
        } else {
            if (wave.visibility != android.view.View.GONE) {
                wave.gone()
                wave.stop()
            }
            songName.setTextColor(colorOnSurface)
        }
    }
}
