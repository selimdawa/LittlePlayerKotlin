package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.FastScrollableAdapter
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur

class AlbumDetailsAdapter(
    private val context: Context,
    private val onItemClick: (MusicFiles, Int) -> Unit
) : BaseMusicAdapter<AlbumDetailsAdapter.AlbumDetailsViewHolder>(AlbumDetailsDiffCallback()),
    FastScrollableAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumDetailsViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlbumDetailsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumDetailsViewHolder, position: Int) {
        val currentFile = getItem(position)

        holder.binding.songName.text = currentFile.title
        val songDetailsText = context.getString(
            R.string.song_details_format,
            currentFile.safeArtist,
            currentFile.album ?: DATA.UNKNOWN
        )
        holder.binding.songDetails.text = songDetailsText

        holder.binding.image.loadSongImage(
            currentFile.albumId, currentFile.path, currentFile.cachedImagePath
        )
        holder.binding.imageBlur.loadSongImageBlur(
            currentFile.albumId, 50, currentFile.path, currentFile.cachedImagePath
        )

        holder.binding.applyTheme(context, currentFile.path)

        holder.itemView.setOnClickListener {
            onItemClick(currentFile, holder.bindingAdapterPosition)
        }
    }

    override fun getPopupText(position: Int): String {
        val title = getItem(position).title ?: ""
        return if (title.isNotEmpty()) title.substring(0, 1).uppercase() else ""
    }

    class AlbumDetailsViewHolder(val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class AlbumDetailsDiffCallback : DiffUtil.ItemCallback<MusicFiles>() {
        override fun areItemsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MusicFiles, newItem: MusicFiles): Boolean {
            return oldItem == newItem
        }
    }
}
