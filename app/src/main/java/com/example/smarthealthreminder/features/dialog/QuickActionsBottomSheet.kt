package com.example.smarthealthreminder.features.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.smarthealthreminder.databinding.BottomSheetQuickActionsBinding
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.Profileinfo.reports.ReportsActivity
import com.example.smarthealthreminder.ui.DashboardActivity
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
            startActivity(Intent(requireContext(), AddReminderActivity::class.java))
            dismiss()
        }

        binding.actionAddAlarm.setOnClickListener {
            Toast.makeText(requireContext(), "Add Alarm - coming soon", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.actionAddReport.setOnClickListener {
            startActivity(Intent(requireContext(), ReportsActivity::class.java))
            dismiss()
        }

        binding.actionViewDashboard.setOnClickListener {
            startActivity(Intent(requireContext(), DashboardActivity::class.java))
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