package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.FastScrollableAdapter
import com.flatcode.littleplayer.utils.PlaybackAnimatable
import com.flatcode.littleplayer.utils.generateRandomColor
import com.flatcode.littleplayer.utils.getAlbumArtBytes
import com.flatcode.littleplayer.utils.gone
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.visible

class AlbumDetailsAdapter(
    private val context: Context,
    private val onItemClick: (MusicFiles, Int) -> Unit,
    private val onColorGenerated: ((String, Int) -> Unit)? = null
) : ListAdapter<MusicFiles, AlbumDetailsAdapter.AlbumDetailsViewHolder>(AlbumDetailsDiffCallback()),
    PlaybackAnimatable, FastScrollableAdapter {

    private var playingPath: String? = null
    private var isPlaying: Boolean = false

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumDetailsViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlbumDetailsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumDetailsViewHolder, position: Int) {
        val currentFile = getItem(position)

        holder.binding.songName.text = currentFile.title
        val songDetailsText = context.getString(
            R.string.song_details_format,
            currentFile.safeArtist,
            currentFile.album ?: DATA.UNKNOWN
        )
        holder.binding.songDetails.text = songDetailsText

        var songColor = currentFile.color
        val hasAlbumArt = (currentFile.albumId != null && currentFile.albumId != "-1" && currentFile.albumId != "0") ||
                !currentFile.cachedImagePath.isNullOrEmpty()

        if (songColor == null && !hasAlbumArt) {
            val newColor = generateRandomColor()
            songColor = newColor
            currentFile.id?.let { onColorGenerated?.invoke(it, newColor) }
        }

        holder.binding.image.loadSongImage(
            currentFile.albumId, currentFile.path, currentFile.cachedImagePath, songColor
        )
        holder.binding.imageBlur.loadSongImageBlur(
            currentFile.albumId, 50, currentFile.path, currentFile.cachedImagePath, songColor
        )

        if ((currentFile.path == playingPath) && isPlaying) {
            holder.binding.wave.visible()
        } else {
            holder.binding.wave.gone()
        }

        val params = holder.itemView.layoutParams as MarginLayoutParams
        params.bottomMargin = if (position == (itemCount - 1)) {
            (95 * context.resources.displayMetrics.density).toInt()
        } else {
            (10 * context.resources.displayMetrics.density).toInt()
        }
        holder.itemView.layoutParams = params

        holder.itemView.setOnClickListener {
            onItemClick(currentFile, holder.bindingAdapterPosition)
        }
    }

    override fun getPopupText(position: Int): String {
        val title = getItem(position).title ?: ""
        return if (title.isNotEmpty()) title.substring(0, 1).uppercase() else ""
    }

    class AlbumDetailsViewHolder(val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class AlbumDetailsDiffCallback : DiffUtil.ItemCallback<MusicFiles>() {
        override fun areItemsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem == newItem
        }
    }
}