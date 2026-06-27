package com.flatcode.littleplayer.Adapter

import android.content.Context
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.Model.MusicFiles
import com.flatcode.littleplayer.Unit.CLASS
import com.flatcode.littleplayer.Unit.DATA
import com.flatcode.littleplayer.Unit.VOID
import com.flatcode.littleplayer.databinding.ItemAlbumBinding
import java.util.ArrayList

class AlbumAdapter(private val context: Context, private val albumFiles: ArrayList<MusicFiles>) :
    RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentFile = albumFiles[position]
        holder.name.text = currentFile.album

        val image = getAlbumArt(currentFile.path) ?: ByteArray(0)

        VOID.GlideByte(context, image, holder.image)
        VOID.GlideBlurByte(context, image, holder.imageBlur, 50)

        holder.itemView.setOnClickListener {
            VOID.IntentExtra(context, CLASS.ALBUM_DETAILS, DATA.ALBUM_NAME, currentFile.album)
        }
    }

    override fun getItemCount(): Int {
        return albumFiles.size
    }

    class ViewHolder(binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        val name: TextView = binding.name
        val image: ImageView = binding.image
        val imageBlur: ImageView = binding.imageBlur
    }

    private fun getAlbumArt(uri: String?): ByteArray? {
        if (uri == null) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(uri)
            val art = retriever.embeddedPicture
            retriever.release()
            art
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}