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
        val image = getAlbumArt(currentFile.path)

        VOID.GlideByte(context, image, holder.image)
        VOID.GlideBlurByte(context, image, holder.imageBlur, 50)

        holder.itemView.setOnClickListener {
            VOID.IntentExtra2Int(
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

    companion object {
        var albumFiles: ArrayList<MusicFiles>? = null
    }
}