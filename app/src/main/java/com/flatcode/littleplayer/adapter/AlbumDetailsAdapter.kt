package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur

class AlbumDetailsAdapter(
    private val context: Context,
    private var albumFiles: ArrayList<MusicFiles>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<AlbumDetailsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filesList = albumFiles ?: return
        if (position !in filesList.indices) return

        val currentFile = filesList[position]
        holder.binding.songName.text = currentFile.title
        holder.binding.songDetails.text = context.getString(
            R.string.song_details_format,
            currentFile.safeArtist,
            currentFile.album ?: "Unknown Album"
        )

        holder.binding.image.loadSongImage(currentFile.albumId, currentFile.path, currentFile.cachedImagePath)
        holder.binding.imageBlur.loadSongImageBlur(currentFile.albumId, 50, currentFile.path, currentFile.cachedImagePath)

        holder.itemView.setOnClickListener {
            onItemClick(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = albumFiles.size

    class ViewHolder(val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root)
}
