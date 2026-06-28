package com.flatcode.littleplayer.adapter

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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.model.MusicFiles
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
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filesList = mFiles ?: return
        val currentFile = filesList[position]

        holder.name.text = currentFile.title

        VOID.coilImage(context, currentFile.id, holder.image,150)
        VOID.coilImageBlur(context, currentFile.id, holder.imageBlur, 50)

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
        val oldList = mFiles ?: ArrayList()
        val newList = ArrayList(musicFilesArrayList)

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        mFiles = newList
        diffResult.dispatchUpdatesTo(this)
    }

    companion object {
        var mFiles: ArrayList<MusicFiles>? = null
    }
}