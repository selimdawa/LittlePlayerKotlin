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
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.google.android.material.snackbar.Snackbar
import io.selimdawa.multiwave.MultiWaveHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicAdapter(private val context: Context, mFiles: ArrayList<MusicFiles>) :
    RecyclerView.Adapter<MusicAdapter.ViewHolder>() {

    private val adapterScope = CoroutineScope(Dispatchers.Main)
    private var playingPath: String? = null
    private var isPlaying: Boolean = false

    init {
        Companion.mFiles = mFiles
    }

    fun updatePlaybackState(path: String?, playing: Boolean) {
        val oldPath = this.playingPath
        val oldPlaying = this.isPlaying

        this.playingPath = path
        this.isPlaying = playing

        if ((oldPath != path) || (oldPlaying != playing)) {
            mFiles?.forEachIndexed { index, musicFiles ->
                if ((musicFiles.path == oldPath) || (musicFiles.path == path)) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filesList = mFiles ?: return
        val currentFile = filesList[position]

        holder.songName.text = currentFile.title
        val songDetailsText = context.getString(
            R.string.song_details_format,
            currentFile.safeArtist,
            currentFile.album ?: "Unknown Album"
        )
        holder.songDetails.text = songDetailsText

        holder.image.loadSongImage(currentFile.albumId)
        holder.imageBlur.loadSongImageBlur(currentFile.albumId, 100)

        if ((currentFile.path == playingPath) && isPlaying) {
            holder.wave.visibility = View.VISIBLE
        } else {
            holder.wave.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            updatePlaybackState(currentFile.path, true)
            context.launchActivity<PlayerActivity> {
                putExtra(DATA.POSITION, holder.bindingAdapterPosition)
            }
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
        if (position < 0 || (position >= filesList.size)) return

        val fileId = filesList[position].id ?: return
        val filePath = filesList[position].path ?: return

        adapterScope.launch {
            val isDeleted = withContext(Dispatchers.IO) {
                val file = File(filePath)
                val deleted = file.delete()
                if (deleted) {
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fileId.toLong()
                    )
                    context.contentResolver.delete(contentUri, null, null)
                    true
                } else {
                    false
                }
            }

            if (isDeleted) {
                if (position in filesList.indices) {
                    filesList.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, filesList.size)
                }
                Snackbar.make(v, "File Deleted : ", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(v, "Can't be Deleted : ", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun getItemCount(): Int = mFiles?.size ?: 0

    class ViewHolder(binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        val songName: TextView = binding.songName
        val songDetails: TextView = binding.songDetails
        val image: ImageView = binding.image
        val imageBlur: ImageView = binding.imageBlur
        val more: ImageView = binding.more
        var wave: MultiWaveHeader = binding.wave
    }

    fun updateList(musicFilesArrayList: ArrayList<MusicFiles>, onCommit: (() -> Unit)? = null) {
        val oldList = mFiles ?: ArrayList()
        val newList = ArrayList(musicFilesArrayList)

        adapterScope.launch {
            val diffResult = withContext(Dispatchers.Default) {
                DiffUtil.calculateDiff(
                    object : DiffUtil.Callback() {
                        override fun getOldListSize(): Int = oldList.size
                        override fun getNewListSize(): Int = newList.size

                        override fun areItemsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int,
                        ): Boolean {
                            return oldList[oldItemPosition].id == newList[newItemPosition].id
                        }

                        override fun areContentsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int,
                        ): Boolean {
                            return oldList[oldItemPosition] == newList[newItemPosition]
                        }
                    },
                )
            }
            mFiles = newList
            diffResult.dispatchUpdatesTo(this@MusicAdapter)
            onCommit?.invoke()
        }
    }

    companion object {
        var mFiles: ArrayList<MusicFiles>? = null
    }
}