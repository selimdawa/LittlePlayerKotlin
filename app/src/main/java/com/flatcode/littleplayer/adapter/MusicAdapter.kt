package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import io.selimdawa.multiwave.MultiWaveHeader

class MusicAdapter(
    private val context: Context,
    private val onItemClick: (MusicFiles, Int) -> Unit,
    private val onDeleteClick: (MusicFiles) -> Unit
) : ListAdapter<MusicFiles, MusicAdapter.ViewHolder>(MusicDiffCallback()) {

    private var playingPath: String? = null
    private var isPlaying: Boolean = false

    fun updatePlaybackState(path: String?, playing: Boolean) {
        val oldPath = this.playingPath
        val oldPlaying = this.isPlaying

        this.playingPath = path
        this.isPlaying = playing

        if ((oldPath != path) || (oldPlaying != playing)) {
            currentList.forEachIndexed { index, musicFiles ->
                if ((musicFiles.path == oldPath) || (musicFiles.path == path)) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentFile = getItem(position)

        holder.songName.text = currentFile.title
        val songDetailsText = context.getString(
            R.string.song_details_format,
            currentFile.safeArtist,
            currentFile.album ?: "Unknown Album"
        )
        holder.songDetails.text = songDetailsText

        holder.image.loadSongImage(currentFile.albumId, currentFile.path, currentFile.cachedImagePath)
        holder.imageBlur.loadSongImageBlur(currentFile.albumId, 100, currentFile.path, currentFile.cachedImagePath)

        if ((currentFile.path == playingPath) && isPlaying) {
            holder.wave.visibility = View.VISIBLE
        } else {
            holder.wave.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(currentFile, holder.bindingAdapterPosition)
        }

        holder.more.setOnClickListener { v ->
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

    class ViewHolder(binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        val songName: TextView = binding.songName
        val songDetails: TextView = binding.songDetails
        val image: ImageView = binding.image
        val imageBlur: ImageView = binding.imageBlur
        val more: ImageView = binding.more
        var wave: MultiWaveHeader = binding.wave
    }

    private class MusicDiffCallback : DiffUtil.ItemCallback<MusicFiles>() {
        override fun areItemsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem == newItem
        }
    }
}