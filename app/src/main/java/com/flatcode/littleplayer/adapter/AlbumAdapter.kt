package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemAlbumBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.FastScrollableAdapter
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur

class AlbumAdapter(
    private val context: Context,
    private val onItemClick: (MusicFiles, View) -> Unit,
) : ListAdapter<MusicFiles, AlbumAdapter.AlbumViewHolder>(AlbumDiffCallback()), FastScrollableAdapter {

    class AlbumViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlbumViewHolder(binding)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val currentFile = getItem(position)
        holder.binding.name.text = currentFile.album

        holder.binding.image.loadSongImage(
            currentFile.albumId, currentFile.path, currentFile.cachedImagePath, currentFile.album, isAlbum = true
        )
        holder.binding.imageBlur.loadSongImageBlur(
            currentFile.albumId, 50, currentFile.path, currentFile.cachedImagePath, currentFile.album, isAlbum = true
        )

        holder.itemView.setOnClickListener {
            onItemClick(currentFile, holder.binding.image)
        }
    }

    override fun getPopupText(position: Int): String {
        val albumName = getItem(position).album ?: ""
        return if (albumName.isNotEmpty()) albumName.substring(0, 1).uppercase() else ""
    }

    private class AlbumDiffCallback : DiffUtil.ItemCallback<MusicFiles>() {
        override fun areItemsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem.album == newItem.album
        }

        override fun areContentsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem == newItem
        }
    }
}