package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemAlbumBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur

class AlbumAdapter(
    private val context: Context,
    private var albumFiles: ArrayList<MusicFiles>,
    private val onItemClick: (String, android.view.View) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlbumViewHolder(binding)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val currentFile = albumFiles[position]
        holder.binding.name.text = currentFile.album

        holder.binding.image.loadSongImage(
            currentFile.albumId, currentFile.path, currentFile.cachedImagePath
        )
        holder.binding.imageBlur.loadSongImageBlur(
            currentFile.albumId, 50, currentFile.path, currentFile.cachedImagePath
        )

        val params = holder.itemView.layoutParams as MarginLayoutParams
        params.bottomMargin = if (position == itemCount - 1) {
            (95 * context.resources.displayMetrics.density).toInt()
        } else {
            (10 * context.resources.displayMetrics.density).toInt()
        }
        holder.itemView.layoutParams = params

        holder.itemView.setOnClickListener {
            onItemClick(currentFile.album ?: "", holder.binding.image)
        }
    }

    override fun getItemCount(): Int = albumFiles.size

    fun updateList(newList: ArrayList<MusicFiles>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = albumFiles.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return albumFiles[oldItemPosition].album == newList[newItemPosition].album
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return albumFiles[oldItemPosition] == newList[newItemPosition]
            }
        })
        albumFiles = newList
        diffResult.dispatchUpdatesTo(this)
    }
}