package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemMusicBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur

class ArtistDetailsAdapter(
    private val context: Context,
    private val songList: ArrayList<MusicFiles>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ArtistDetailsAdapter.ArtistDetailsViewHolder>() {

    class ArtistDetailsViewHolder(val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistDetailsViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(context), parent, false)
        return ArtistDetailsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistDetailsViewHolder, position: Int) {
        val song = songList[position]

        holder.binding.songName.text = song.safeTitle
        holder.binding.songDetails.text = context.getString(
            R.string.song_details_format, song.safeArtist, song.album ?: DATA.UNKNOWN
        )

        holder.binding.image.loadSongImage(song.albumId, song.path, song.cachedImagePath)
        holder.binding.imageBlur.loadSongImageBlur(
            song.albumId, 100, song.path, song.cachedImagePath
        )

        holder.itemView.setOnClickListener {
            onItemClick(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = songList.size
}