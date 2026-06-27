package com.flatcode.littleplayer.Adapter

import android.content.ContentUris
import android.content.Context
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
import com.flatcode.littleplayer.unit.CLASS
import com.flatcode.littleplayer.unit.DATA
import com.flatcode.littleplayer.unit.VOID
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
        // تم الإصلاح: تعريف الـ binding محلياً لمنع تهنيج قائمة الأغاني
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filesList = mFiles ?: return
        val currentFile = filesList[position]

        holder.name.text = currentFile.title

        VOID.coiImage(context, currentFile.id, holder.image)
        VOID.coiImageBlur(context, currentFile.id, holder.imageBlur, 50)

        holder.itemView.setOnClickListener {
            VOID.intentExtraInt(context, CLASS.PLAYER, DATA.POSITION, holder.bindingAdapterPosition)
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

    override fun getItemCount(): Int = mFiles?.size ?: 0

    class ViewHolder(binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        val name: TextView = binding.name
        val image: ImageView = binding.image
        val imageBlur: ImageView = binding.imageBlur
        val more: ImageView = binding.more
    }

    fun updateList(musicFilesArrayList: ArrayList<MusicFiles>) {
        mFiles = ArrayList(musicFilesArrayList)
        notifyDataSetChanged()
    }

    companion object {
        var mFiles: ArrayList<MusicFiles>? = null
    }
}