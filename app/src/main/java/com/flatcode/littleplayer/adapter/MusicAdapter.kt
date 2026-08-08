package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.fragment.SongOptionsBottomSheet
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.FastScrollableAdapter
import com.flatcode.littleplayer.utils.getAppCompatActivity
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur

class MusicAdapter(
    private val context: Context,
    private val onItemClick: (MusicFiles, Int, View) -> Unit,
    private val onDeleteClick: (MusicFiles) -> Unit,
    private val onRemoveFromPlaylistClick: ((MusicFiles) -> Unit)? = null,
) : BaseMusicAdapter<MusicAdapter.MusicViewHolder>(MusicDiffCallback()),
    FastScrollableAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val currentFile = getItem(position)

        holder.binding.songName.text = currentFile.title
        val songDetailsText = context.getString(
            R.string.song_details_format, currentFile.safeArtist, currentFile.album ?: DATA.UNKNOWN
        )
        holder.binding.songDetails.text = songDetailsText

        holder.binding.image.loadSongImage(
            currentFile.albumId, currentFile.path, currentFile.cachedImagePath
        )
        holder.binding.imageBlur.loadSongImageBlur(
            currentFile.albumId, 100, currentFile.path, currentFile.cachedImagePath
        )

        holder.binding.applyTheme(context, currentFile.path)

        holder.itemView.setOnClickListener {
            onItemClick(currentFile, holder.bindingAdapterPosition, holder.binding.image)
        }

        holder.binding.more.setOnClickListener {
            context.getAppCompatActivity()?.let { activity ->
                val bottomSheet =
                    SongOptionsBottomSheet(currentFile, onDeleteClick, onRemoveFromPlaylistClick)
                bottomSheet.show(activity.supportFragmentManager, "SongOptionsBottomSheet")
            }
        }
    }

    override fun getItemCount(): Int = currentList.size

    override fun getPopupText(position: Int): String {
        val title = currentList.getOrNull(position)?.title ?: ""
        return if (title.isNotEmpty()) title.substring(0, 1).uppercase() else ""
    }

    class MusicViewHolder(val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root)

    private class MusicDiffCallback : DiffUtil.ItemCallback<MusicFiles>() {
        override fun areItemsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem == newItem
        }
    }
}
