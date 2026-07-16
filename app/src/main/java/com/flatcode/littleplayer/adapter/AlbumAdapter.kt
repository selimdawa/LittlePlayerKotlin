package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.databinding.ItemAlbumBinding
import java.util.ArrayList

class AlbumAdapter(
    private val context: Context,
    private val albumFiles: ArrayList<MusicFiles>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentFile = albumFiles[position]
        holder.binding.name.text = currentFile.album

        holder.binding.image.loadSongImage(context, currentFile.id, currentFile.path, 300)
        holder.binding.imageBlur.loadSongImageBlur(context, currentFile.id, currentFile.path, 50)

        holder.itemView.setOnClickListener {
            onItemClick(currentFile.album ?: "")
        }
    }

    override fun getItemCount(): Int = albumFiles.size
}
