package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.unit.CLASS
import com.flatcode.littleplayer.unit.DATA
import com.flatcode.littleplayer.unit.VOID
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import java.util.ArrayList

class AlbumDetailsAdapter(private val context: Context, albumFiles: ArrayList<MusicFiles>) :
    RecyclerView.Adapter<AlbumDetailsAdapter.ViewHolder>() {

    init {
        Companion.albumFiles = albumFiles
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filesList = albumFiles ?: return
        val currentFile = filesList[position]

        holder.name.text = currentFile.title

        VOID.coiImage(context, currentFile.id, holder.image)
        VOID.coiImageBlur(context, currentFile.id, holder.imageBlur, 50)

        holder.itemView.setOnClickListener {
            VOID.intentExtra2Int(
                context, CLASS.PLAYER,
                DATA.SENDER, DATA.ALBUM_DETAILS, DATA.POSITION, holder.bindingAdapterPosition
            )
        }
    }

    override fun getItemCount(): Int {
        return albumFiles?.size ?: 0
    }

    class ViewHolder(binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        val name: TextView = binding.name
        val image: ImageView = binding.image
        val imageBlur: ImageView = binding.imageBlur
    }

    companion object {
        var albumFiles: ArrayList<MusicFiles>? = null
    }
}