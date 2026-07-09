package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemFolderDetailsBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.VOID
import java.util.ArrayList

class FolderDetailsAdapter(
    private val context: Context,
    private val songList: ArrayList<MusicFiles>
) : RecyclerView.Adapter<FolderDetailsAdapter.FolderDetailsViewHolder>() {

    class FolderDetailsViewHolder(val binding: ItemFolderDetailsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderDetailsViewHolder {
        val binding = ItemFolderDetailsBinding.inflate(LayoutInflater.from(context), parent, false)
        return FolderDetailsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderDetailsViewHolder, position: Int) {
        val song = songList[position]

        holder.binding.songName.text = song.safeTitle
        holder.binding.folderName.text = song.album ?: "Unknown Album"

        val bitmap = VOID.loadRawAlbumArt(song.path)
        if (bitmap != null) {
            holder.binding.musicImg.load(bitmap)
        } else {
            holder.binding.musicImg.load(R.drawable.logo)
        }

        holder.itemView.setOnClickListener {
            // Click listener implementation
        }
    }

    override fun getItemCount(): Int = songList.size
}