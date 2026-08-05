package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemArtistBinding
import com.flatcode.littleplayer.model.Artist

import com.flatcode.littleplayer.utils.FastScrollableAdapter

class ArtistAdapter(
    private val context: Context,
    private var artistList: ArrayList<Artist>,
    private val onItemClick: (String, android.view.View) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>(), FastScrollableAdapter {

    class ArtistViewHolder(val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artistList[position]
        holder.binding.artistName.text = artist.name
        holder.binding.songsCount.text =
            if (artist.songsCount == 1) "1 song" else "${artist.songsCount} songs"

        val params = holder.itemView.layoutParams as MarginLayoutParams
        params.bottomMargin = if (position == itemCount - 1) {
            (95 * context.resources.displayMetrics.density).toInt()
        } else {
            (10 * context.resources.displayMetrics.density).toInt()
        }
        holder.itemView.layoutParams = params

        holder.itemView.setOnClickListener {
            onItemClick(artist.name, holder.itemView) // Or an icon if artists had images
        }
    }

    override fun getItemCount(): Int = artistList.size

    override fun getPopupText(position: Int): String {
        val artistName = artistList.getOrNull(position)?.name ?: ""
        return if (artistName.isNotEmpty()) artistName.substring(0, 1).uppercase() else ""
    }

    fun updateList(newList: ArrayList<Artist>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = artistList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return artistList[oldItemPosition].name == newList[newItemPosition].name
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return artistList[oldItemPosition] == newList[newItemPosition]
            }
        })
        artistList = newList
        diffResult.dispatchUpdatesTo(this)
    }
}