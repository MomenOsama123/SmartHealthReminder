package com.example.smarthealthreminder.features.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.Profileinfo.reports.ReportsActivity
import com.example.smarthealthreminder.features.activity.MainActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QuickActionsBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_quick_actions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.action_add_reminder).setOnClickListener {
            navigateTo(MainActivity.DESTINATION_REMINDERS)
        }

        view.findViewById<View>(R.id.action_add_alarm).setOnClickListener {
            navigateTo(MainActivity.DESTINATION_ALARMS)
        }

        view.findViewById<View>(R.id.action_add_report).setOnClickListener {
            startActivity(Intent(requireContext(), ReportsActivity::class.java))
            dismiss()
        }
    }

    private fun navigateTo(destination: String) {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_DESTINATION, destination)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        dismiss()
    }

    companion object {
        const val TAG = "QuickActionsBottomSheet"
    }
}