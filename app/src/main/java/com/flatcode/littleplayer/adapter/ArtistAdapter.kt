package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemArtistBinding
import com.flatcode.littleplayer.model.Artist
import me.zhanghai.android.fastscroll.PopupTextProvider

class ArtistAdapter(
    private val context: Context,
    private val artistList: ArrayList<Artist>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>(), PopupTextProvider {

    class ArtistViewHolder(val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artistList[position]
        holder.binding.artistName.text = artist.name
        holder.binding.songsCount.text = if (artist.songsCount == 1) "1 song" else "${artist.songsCount} songs"

        holder.itemView.setOnClickListener {
            onItemClick(artist.name)
        }
    }

    override fun getItemCount(): Int = artistList.size

    @NonNull
    override fun getPopupText(@NonNull view: View, position: Int): CharSequence {
        val artistName = artistList[position].name
        return if (artistName.isNotEmpty()) artistName.substring(0, 1).uppercase() else ""
    }
}
