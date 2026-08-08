package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.R.color.transparent
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.DialogSleepTimerBinding
import com.flatcode.littleplayer.databinding.DialogSleepTimerCustomBinding
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
        val dialog =
            MaterialAlertDialogBuilder(context).setView(dialogBinding.root).setCancelable(true)
                .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(transparent)
            dialogBinding.editText.requestFocus()
            val imm =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(
                dialogBinding.editText, InputMethodManager.SHOW_IMPLICIT
            )
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
                    dialogBinding.inputLayout.error = "Enter valid minutes"
                }
            } else {
                dialogBinding.inputLayout.error = "Enter minutes"
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onStart() {
        super.onStart()
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