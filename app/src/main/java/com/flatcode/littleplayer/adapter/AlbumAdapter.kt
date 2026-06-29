package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.unit.CLASS
import com.flatcode.littleplayer.unit.DATA
import com.flatcode.littleplayer.unit.VOID
import com.flatcode.littleplayer.databinding.ItemAlbumBinding
import java.util.ArrayList

class AlbumAdapter(private val context: Context, private val albumFiles: ArrayList<MusicFiles>) :
    RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentFile = albumFiles[position]
        holder.name.text = currentFile.album

        VOID.coilImage(context, currentFile.id, holder.image, 300)
        VOID.coilImageBlur(context, currentFile.id, holder.imageBlur, 50)

        holder.itemView.setOnClickListener {
            VOID.intentExtra(context, CLASS.ALBUM_DETAILS, DATA.ALBUM_NAME, currentFile.album)
        }
    }

    override fun getItemCount(): Int = albumFiles.size

    class ViewHolder(binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        val name: TextView = binding.name
        val image: ImageView = binding.image
        val imageBlur: ImageView = binding.imageBlur
    }
}