package com.example.smarthealthreminder.features.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.smarthealthreminder.databinding.BottomSheetQuickActionsBinding
import com.example.smarthealthreminder.features.activity.MainActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class QuickActionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQuickActionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetQuickActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.actionAddReminder.setOnClickListener {
            startActivity(
                Intent(requireContext(), MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_ADD_REMINDER)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            dismiss()
        }

        binding.actionAddAlarm.setOnClickListener {
            startActivity(
                Intent(requireContext(), MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_ALARMS)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            dismiss()
        }

        // 🌟 التعديل هنا: خليناه يكلم MainActivity عشان يفتح الـ ReportsFragment
        binding.actionAddReport.setOnClickListener {
            startActivity(
                Intent(requireContext(), MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_REPORTS)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            dismiss()
        }

        binding.actionViewDashboard.setOnClickListener {
            startActivity(
                Intent(requireContext(), MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_DASHBOARD)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            dismiss()
        }

        binding.actionAddMedicationPlan.setOnClickListener {
            startActivity(
                Intent(requireContext(), MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_MEDICATION_PLANS)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "QuickActionsBottomSheet"

        fun newInstance(): QuickActionsBottomSheet {
            return QuickActionsBottomSheet()
        }
    }
}