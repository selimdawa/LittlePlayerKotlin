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
    diffCallback: DiffUtil.ItemCallback<MusicFiles>
) : ListAdapter<MusicFiles, VH>(diffCallback), PlaybackAnimatable {

    protected var playingPath: String? = null
    protected var isPlaying: Boolean = false
    protected var currentThemeMode: Int = DATA.MODE_BASIC
    protected var currentThemeColor: Int = Color.WHITE

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

        if (oldMode != mode || oldColor != color) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    protected fun ItemMusicBinding.applyTheme(context: Context, songPath: String?) {
        if ((songPath == playingPath) && isPlaying) {
            wave.visible()
            val trackColor = when (currentThemeMode) {
                DATA.MODE_PALETTE -> currentThemeColor.ensureBrightColor()
                DATA.MODE_WHITE -> Color.WHITE
                else -> context.getLibraryColor("mc_track")
            }
            wave.startColor = trackColor
            songName.setTextColor(trackColor)
        } else {
            wave.gone()
            songName.setTextColor(context.getLibraryColor("colorError"))
        }
    }
}
