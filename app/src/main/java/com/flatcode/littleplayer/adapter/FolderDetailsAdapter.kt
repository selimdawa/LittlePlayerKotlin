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
import com.flatcode.littleplayer.utils.PlaybackAnimatable
import com.flatcode.littleplayer.utils.gone
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.visible

class FolderDetailsAdapter(
    private val context: Context,
    private val onItemClick: (MusicFiles, Int) -> Unit
) : ListAdapter<MusicFiles, FolderDetailsAdapter.FolderDetailsViewHolder>(FolderDetailsDiffCallback()), PlaybackAnimatable {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderDetailsViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return FolderDetailsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderDetailsViewHolder, position: Int) {
        val song = getItem(position)

        holder.binding.songName.text = song.safeTitle
        holder.binding.songDetails.text = context.getString(
            R.string.song_details_format, song.safeArtist, song.album ?: DATA.UNKNOWN
        )

        holder.binding.image.loadSongImage(song.albumId, song.path, song.cachedImagePath)
        holder.binding.imageBlur.loadSongImageBlur(
            song.albumId, 100, song.path, song.cachedImagePath
        )

        if ((song.path == playingPath) && isPlaying) {
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
            onItemClick(song, holder.bindingAdapterPosition)
        }
    }

    class FolderDetailsViewHolder(val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class FolderDetailsDiffCallback : DiffUtil.ItemCallback<MusicFiles>() {
        override fun areItemsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem == newItem
        }
    }
}