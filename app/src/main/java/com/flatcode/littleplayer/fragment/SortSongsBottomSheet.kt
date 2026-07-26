package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.flatcode.littleplayer.databinding.DialogSortSongsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.getLibraryColor
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SortSongsBottomSheet(
    private val currentSort: String?, private val onSortSelected: (String) -> Unit
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
                binding.checkByDate.visibility = View.VISIBLE
                binding.checkByDate.setColorFilter(trackColor)
                binding.textByDate.setTextColor(trackColor)
            }

            DATA.SORT_BY_NAME -> {
                binding.checkByName.visibility = View.VISIBLE
                binding.checkByName.setColorFilter(trackColor)
                binding.textByName.setTextColor(trackColor)
            }

            DATA.SORT_BY_PLAY_COUNT -> {
                binding.checkByPlayCount.visibility = View.VISIBLE
                binding.checkByPlayCount.setColorFilter(trackColor)
                binding.textByPlayCount.setTextColor(trackColor)
            }

            DATA.SORT_BY_RELEASE_DATE -> {
                binding.checkByReleaseDate.visibility = View.VISIBLE
                binding.checkByReleaseDate.setColorFilter(trackColor)
                binding.textByReleaseDate.setTextColor(trackColor)
            }

            DATA.SORT_BY_SIZE -> {
                binding.checkBySize.visibility = View.VISIBLE
                binding.checkBySize.setColorFilter(trackColor)
                binding.textBySize.setTextColor(trackColor)
            }
        }

        binding.sortByDate.setOnClickListener {
            if (currentSort != DATA.SORT_BY_DATE) onSortSelected(DATA.SORT_BY_DATE)
            dismiss()
        }

        binding.sortByName.setOnClickListener {
            if (currentSort != DATA.SORT_BY_NAME) onSortSelected(DATA.SORT_BY_NAME)
            dismiss()
        }

        binding.sortByPlayCount.setOnClickListener {
            if (currentSort != DATA.SORT_BY_PLAY_COUNT) onSortSelected(DATA.SORT_BY_PLAY_COUNT)
            dismiss()
        }

        binding.sortByReleaseDate.setOnClickListener {
            if (currentSort != DATA.SORT_BY_RELEASE_DATE) onSortSelected(DATA.SORT_BY_RELEASE_DATE)
            dismiss()
        }

        binding.sortBySize.setOnClickListener {
            if (currentSort != DATA.SORT_BY_SIZE) onSortSelected(DATA.SORT_BY_SIZE)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(
                android.R.color.transparent
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}