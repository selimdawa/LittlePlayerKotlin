package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.R.color.transparent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.flatcode.littleplayer.databinding.DialogSortSongsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.visible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.R as MaterialR

class SortSongsBottomSheet(
    private val category: String,
    private val currentSort: String?,
    private val onSortSelected: (String, String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogSortSongsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogSortSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trackColor = requireContext().getLibraryColor("mc_track")
        when (currentSort) {
            DATA.SORT_BY_DATE -> {
                binding.checkByDate.visible()
                binding.checkByDate.setColorFilter(trackColor)
                binding.textByDate.setTextColor(trackColor)
            }

            DATA.SORT_BY_NAME -> {
                binding.checkByName.visible()
                binding.checkByName.setColorFilter(trackColor)
                binding.textByName.setTextColor(trackColor)
            }

            DATA.SORT_BY_PLAY_COUNT -> {
                binding.checkByPlayCount.visible()
                binding.checkByPlayCount.setColorFilter(trackColor)
                binding.textByPlayCount.setTextColor(trackColor)
            }

            DATA.SORT_BY_RELEASE_DATE -> {
                binding.checkByReleaseDate.visible()
                binding.checkByReleaseDate.setColorFilter(trackColor)
                binding.textByReleaseDate.setTextColor(trackColor)
            }

            DATA.SORT_BY_SIZE -> {
                binding.checkBySize.visible()
                binding.checkBySize.setColorFilter(trackColor)
                binding.textBySize.setTextColor(trackColor)
            }

            DATA.SORT_BY_SONG_COUNT -> {
                binding.checkBySize.visible()
                binding.checkBySize.setColorFilter(trackColor)
                binding.textBySize.setTextColor(trackColor)
            }
        }

        binding.sortByDate.setOnClickListener {
            if (currentSort != DATA.SORT_BY_DATE) onSortSelected(category, DATA.SORT_BY_DATE)
            dismiss()
        }

        binding.sortByName.setOnClickListener {
            if (currentSort != DATA.SORT_BY_NAME) onSortSelected(category, DATA.SORT_BY_NAME)
            dismiss()
        }

        binding.sortByPlayCount.setOnClickListener {
            if (currentSort != DATA.SORT_BY_PLAY_COUNT) onSortSelected(category, DATA.SORT_BY_PLAY_COUNT)
            dismiss()
        }

        binding.sortByReleaseDate.setOnClickListener {
            if (currentSort != DATA.SORT_BY_RELEASE_DATE) onSortSelected(category, DATA.SORT_BY_RELEASE_DATE)
            dismiss()
        }

        binding.sortBySize.setOnClickListener {
            val sortType = if (category == DATA.ALBUMS || category == DATA.PLAYLISTS) DATA.SORT_BY_SONG_COUNT else DATA.SORT_BY_SIZE
            if (currentSort != sortType) onSortSelected(category, sortType)
            dismiss()
        }

        if (category == DATA.ALBUMS) {
            binding.title.text = getString(com.flatcode.littleplayer.R.string.sort_albums)
            binding.sortByPlayCount.visibility = View.GONE
            binding.sortByReleaseDate.visibility = View.GONE
            binding.textBySize.text = getString(com.flatcode.littleplayer.R.string.by_song_count)
        }

        if (category == DATA.PLAYLISTS) {
            binding.title.text = getString(com.flatcode.littleplayer.R.string.sort_playlists)
            binding.sortByDate.visibility = View.GONE
            binding.sortByPlayCount.visibility = View.GONE
            binding.sortByReleaseDate.visibility = View.GONE
            binding.textBySize.text = getString(com.flatcode.littleplayer.R.string.by_song_count)
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