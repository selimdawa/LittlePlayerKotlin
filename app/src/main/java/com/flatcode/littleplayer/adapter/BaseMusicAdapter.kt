package com.flatcode.littleplayer.adapter

import android.content.Context
import android.graphics.Color
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.PlaybackAnimatable
import com.flatcode.littleplayer.utils.ensureBrightColor
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.gone
import com.flatcode.littleplayer.utils.visible

abstract class BaseMusicAdapter<VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<MusicFiles>,
) : ListAdapter<MusicFiles, VH>(diffCallback), PlaybackAnimatable {

    protected var playingPath: String? = null
    protected var isPlaying: Boolean = false
    protected var currentThemeMode: Int = DATA.MODE_BASIC
    protected var currentThemeColor: Int = Color.WHITE
    protected var listItemThemeEnabled: Boolean = false

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

    override fun updateThemeState(mode: Int, color: Int) {
        val oldMode = this.currentThemeMode
        val oldColor = this.currentThemeColor

        this.currentThemeMode = mode
        this.currentThemeColor = color

        if ((oldMode != mode) || (oldColor != color)) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun updateListThemeState(enabled: Boolean) {
        val old = this.listItemThemeEnabled
        this.listItemThemeEnabled = enabled
        if (old != enabled) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    protected fun ItemMusicBinding.applyTheme(context: Context, songPath: String?) {
        if (songPath == playingPath) {
            wave.visible()
            wave.start()

            val trackColor = if (listItemThemeEnabled) {
                when (currentThemeMode) {
                    DATA.MODE_PALETTE -> currentThemeColor.ensureBrightColor()
                    DATA.MODE_WHITE -> Color.WHITE
                    else -> context.getLibraryColor("mc_track")
                }
            } else {
                context.getLibraryColor("mc_track")
            }

            val closeColor = if (listItemThemeEnabled) {
                when (currentThemeMode) {
                    DATA.MODE_PALETTE -> trackColor
                    DATA.MODE_WHITE -> Color.WHITE
                    else -> context.getLibraryColor("mc_tick")
                }
            } else {
                context.getLibraryColor("mc_tick")
            }

            wave.startColor = trackColor
            wave.closeColor = closeColor
            songName.setTextColor(trackColor)
        } else {
            wave.gone()
            wave.stop()
            songName.setTextColor(context.getLibraryColor("colorOnSurface"))
        }
    }
}
