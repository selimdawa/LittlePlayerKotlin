package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.R.color.transparent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.DialogSleepTimerBinding
import com.flatcode.littleplayer.databinding.DialogSleepTimerCustomBinding
import com.flatcode.littleplayer.utils.showKeyboard
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.R as MaterialR

class SleepTimerBottomSheet(
    private val onTimeSelected: (Int, String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogSleepTimerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogSleepTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionOff.setOnClickListener {
            onTimeSelected(0, getString(R.string.off))
            dismiss()
        }

        binding.option15Min.setOnClickListener {
            onTimeSelected(15, getString(R.string._15_minutes))
            dismiss()
        }

        binding.option30Min.setOnClickListener {
            onTimeSelected(30, getString(R.string._30_minutes))
            dismiss()
        }

        binding.option60Min.setOnClickListener {
            onTimeSelected(60, getString(R.string._60_minutes))
            dismiss()
        }

        binding.optionCustom.setOnClickListener {
            showCustomTimeDialog()
        }
    }

    private fun showCustomTimeDialog() {
        val context = requireContext()
        val dialogBinding = DialogSleepTimerCustomBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(transparent)
            dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            dialogBinding.editText.requestFocus()
            dialogBinding.editText.showKeyboard()
        }

        dialogBinding.btnOk.setOnClickListener {
            val minutesStr = dialogBinding.editText.text.toString().trim()
            if (minutesStr.isNotEmpty()) {
                val minutes = minutesStr.toIntOrNull() ?: 0
                if (minutes > 0) {
                    onTimeSelected(minutes, getString(R.string.timer_set_format, minutes))
                    dialog.dismiss()
                    dismiss()
                } else {
                    dialogBinding.inputLayout.error = getString(R.string.enter_valid_minutes)
                }
            } else {
                dialogBinding.inputLayout.error = getString(R.string.enter_minutes)
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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