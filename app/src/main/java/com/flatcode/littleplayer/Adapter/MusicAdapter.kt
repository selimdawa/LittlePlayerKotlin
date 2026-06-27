package com.flatcode.littleplayer.Adapter

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.Model.MusicFiles
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.Unit.CLASS
import com.flatcode.littleplayer.Unit.DATA
import com.flatcode.littleplayer.Unit.VOID
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.ArrayList

class MusicAdapter(private val context: Context, mFiles: ArrayList<MusicFiles>) :
    RecyclerView.Adapter<MusicAdapter.ViewHolder>() {

    init {
        Companion.mFiles = mFiles
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filesList = mFiles ?: return
        val currentFile = filesList[position]

        holder.name.text = currentFile.title
        val image = getAlbumArt(currentFile.path)

        VOID.GlideByte(context, image, holder.image)
        VOID.GlideBlurByte(context, image, holder.imageBlur, 50)

        holder.itemView.setOnClickListener {
            VOID.IntentExtraInt(context, CLASS.PLAYER, DATA.POSITION, holder.bindingAdapterPosition)
        }

        holder.more.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menuInflater.inflate(R.menu.popup, popupMenu.menu)
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.delete) {
                    Toast.makeText(context, "Delete Clicked!!", Toast.LENGTH_SHORT).show()
                    deleteFile(holder.bindingAdapterPosition, v)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun deleteFile(position: Int, v: View) {
        val filesList = mFiles ?: return
        if (position < 0 || position >= filesList.size) return

        val fileId = filesList[position].id ?: return
        val filePath = filesList[position].path ?: return

        val contentUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            fileId.toLong()
        )
        val file = File(filePath)
        val deleted = file.delete()

        if (deleted) {
            context.contentResolver.delete(contentUri, null, null)
            filesList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, filesList.size)
            Snackbar.make(v, "File Deleted : ", Snackbar.LENGTH_LONG).show()
        } else {
            Snackbar.make(v, "Can't be Deleted : ", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun getItemCount(): Int {
        return mFiles?.size ?: 0
    }

    class ViewHolder(binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        val name: TextView = binding.name
        val image: ImageView = binding.image
        val imageBlur: ImageView = binding.imageBlur
        val more: ImageView = binding.more
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

    fun updateList(musicFilesArrayList: ArrayList<MusicFiles>) {
        mFiles = ArrayList(musicFilesArrayList)
        notifyDataSetChanged()
    }

    companion object {
        var mFiles: ArrayList<MusicFiles>? = null
    }
}