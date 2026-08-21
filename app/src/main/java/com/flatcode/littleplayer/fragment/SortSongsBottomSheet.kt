package com.flatcode.littleplayer.fragment

import android.R.color.transparent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.DialogSortSongsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.visible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.selimdawa.multicolors.R as MultiColorR
import com.google.android.material.R as MaterialR

class SortSongsBottomSheet(
    private val category: String,
    private val currentSort: String?,
    private val onSortSelected: (String, String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogSortSongsBinding? = null
    private val binding get() = _binding!!

    private data class SortOption(
        val type: String, val container: View, val check: ImageView, val text: TextView
    )

    private val sortOptions by lazy {
        listOf(
            SortOption(
                DATA.SORT_BY_DATE, binding.sortByDate, binding.checkByDate, binding.textByDate
            ), SortOption(
                DATA.SORT_BY_NAME, binding.sortByName, binding.checkByName, binding.textByName
            ), SortOption(
                DATA.SORT_BY_PLAY_COUNT,
                binding.sortByPlayCount,
                binding.checkByPlayCount,
                binding.textByPlayCount
            ), SortOption(
                DATA.SORT_BY_RELEASE_DATE,
                binding.sortByReleaseDate,
                binding.checkByReleaseDate,
                binding.textByReleaseDate
            ), SortOption(
                DATA.SORT_BY_SIZE, binding.sortBySize, binding.checkBySize, binding.textBySize
            ), SortOption(
                DATA.SORT_BY_SONG_COUNT, binding.sortBySize, binding.checkBySize, binding.textBySize
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogSortSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trackColor = requireContext().getLibraryColor(MultiColorR.attr.mc_track)

        sortOptions.forEach { option ->
            if (option.type == currentSort) {
                option.check.visible()
                option.check.setColorFilter(trackColor)
                option.text.setTextColor(trackColor)
            }

            option.container.setOnClickListener {
                var sortType = option.type
                if (option.type == DATA.SORT_BY_SIZE || option.type == DATA.SORT_BY_SONG_COUNT) {
                    sortType =
                        if (category == DATA.ALBUMS || category == DATA.PLAYLISTS || category == DATA.ARTISTS || category == DATA.FOLDERS) {
                            DATA.SORT_BY_SONG_COUNT
                        } else {
                            DATA.SORT_BY_SIZE
                        }
                }

                if (currentSort != sortType) onSortSelected(category, sortType)
                dismiss()
            }
        }

        when (category) {
            DATA.ALBUMS -> {
                binding.title.text = getString(R.string.sort_albums)
                binding.sortByPlayCount.visibility = View.GONE
                binding.sortByReleaseDate.visibility = View.GONE
                binding.textBySize.text = getString(R.string.by_song_count)
            }

            DATA.PLAYLISTS -> {
                binding.title.text = getString(R.string.sort_playlists)
                binding.sortByDate.visibility = View.GONE
                binding.sortByPlayCount.visibility = View.GONE
                binding.sortByReleaseDate.visibility = View.GONE
                binding.textBySize.text = getString(R.string.by_song_count)
            }

            DATA.ARTISTS -> {
                binding.title.text = getString(R.string.sort_artists)
                binding.sortByDate.visibility = View.GONE
                binding.sortByPlayCount.visibility = View.GONE
                binding.sortByReleaseDate.visibility = View.GONE
                binding.textBySize.text = getString(R.string.by_song_count)
            }

            DATA.FOLDERS -> {
                binding.title.text = getString(R.string.sort_folders)
                binding.sortByDate.visibility = View.GONE
                binding.sortByPlayCount.visibility = View.GONE
                binding.sortByReleaseDate.visibility = View.GONE
                binding.textBySize.text = getString(R.string.by_song_count)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.window?.apply {
            findViewById<View>(MaterialR.id.design_bottom_sheet)?.setBackgroundResource(
                transparent
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}