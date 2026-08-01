package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.PlaybackAnimatable
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.gone
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.visible
import me.zhanghai.android.fastscroll.PopupTextProvider

class MusicAdapter(
    private val context: Context,
    private val onItemClick: (MusicFiles, Int) -> Unit,
    private val onDeleteClick: (MusicFiles) -> Unit
) : ListAdapter<MusicFiles, MusicAdapter.MusicViewHolder>(MusicDiffCallback()), PlaybackAnimatable,
    PopupTextProvider {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val currentFile = getItem(position)

        holder.binding.songName.text = currentFile.title
        val songDetailsText = context.getString(
            R.string.song_details_format, currentFile.safeArtist, currentFile.album ?: DATA.UNKNOWN
        )
        holder.binding.songDetails.text = songDetailsText

        holder.binding.image.loadSongImage(
            currentFile.albumId, currentFile.path, currentFile.cachedImagePath
        )
        holder.binding.imageBlur.loadSongImageBlur(
            currentFile.albumId, 100, currentFile.path, currentFile.cachedImagePath
        )

        if ((currentFile.path == playingPath) && isPlaying) {
            holder.binding.wave.visible()
            holder.binding.songName.setTextColor(context.getLibraryColor("mc_track"))
        } else {
            holder.binding.wave.gone()
            holder.binding.songName.setTextColor(context.getLibraryColor("colorError"))
        }

        holder.itemView.setOnClickListener {
            onItemClick(currentFile, holder.bindingAdapterPosition)
        }

        holder.binding.more.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menuInflater.inflate(R.menu.popup, popupMenu.menu)
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.delete) {
                    onDeleteClick(currentFile)
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun getItemCount(): Int = currentList.size

    override fun getPopupText(view: android.view.View, position: Int): CharSequence {
        val title = getItem(position).title ?: ""
        return if (title.isNotEmpty()) title.substring(0, 1).uppercase() else ""
    }

    class MusicViewHolder(val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root)

    private class MusicDiffCallback : DiffUtil.ItemCallback<MusicFiles>() {
        override fun areItemsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem == newItem
        }
    }
}