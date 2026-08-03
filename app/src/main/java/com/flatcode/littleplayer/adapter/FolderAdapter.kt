package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemFolderBinding
import com.flatcode.littleplayer.model.Folder

class FolderAdapter(
    private val context: Context,
    private var folderList: ArrayList<Folder>,
    private val onItemClick: (String, String, android.view.View) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    class FolderViewHolder(val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folderList[position]
        holder.binding.folderName.text = folder.name
        holder.binding.folderDetails.text = "${folder.songsCount} songs | ${folder.path}"

        val params = holder.itemView.layoutParams as MarginLayoutParams
        params.bottomMargin = if (position == itemCount - 1) {
            (95 * context.resources.displayMetrics.density).toInt()
        } else {
            (10 * context.resources.displayMetrics.density).toInt()
        }
        holder.itemView.layoutParams = params

        holder.itemView.setOnClickListener {
            onItemClick(folder.name, folder.path, holder.itemView)
        }
    }

    override fun getItemCount(): Int = folderList.size

    fun updateList(newList: ArrayList<Folder>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = folderList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return folderList[oldItemPosition].path == newList[newItemPosition].path
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return folderList[oldItemPosition] == newList[newItemPosition]
            }
        })
        folderList = newList
        diffResult.dispatchUpdatesTo(this)
    }
}