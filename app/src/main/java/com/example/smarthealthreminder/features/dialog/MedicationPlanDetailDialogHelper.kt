package com.example.smarthealthreminder.features.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip


object MedicationPlanDetailDialogHelper {

    fun show(
        context: Context,
        plan: MedicationPlanEntity,
        onStopPlan: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_medication_plan_detail, null)

        dialogView.findViewById<TextView>(R.id.tv_plan_dialog_title).text = plan.medicineName
        dialogView.findViewById<TextView>(R.id.tv_plan_dialog_dosage).text =
            plan.dosage?.takeIf { it.isNotBlank() } ?: "Not specified"
        dialogView.findViewById<TextView>(R.id.tv_plan_dialog_instructions).text =
            plan.instructions?.takeIf { it.isNotBlank() } ?: "No special instructions"
        dialogView.findViewById<TextView>(R.id.tv_plan_dialog_times).text =
            "${plan.timesPerDay}x daily — ${plan.timesOfDay.replace(",", ", ")}"
        dialogView.findViewById<TextView>(R.id.tv_plan_dialog_start_date).text = plan.startDate
        dialogView.findViewById<TextView>(R.id.tv_plan_dialog_end_date).text = plan.endDate

        val today = RecurrenceHelper.getTodayString()
        val isFinished = !plan.isActive || plan.endDate < today

        val statusChip = dialogView.findViewById<Chip>(R.id.chip_plan_dialog_status)
        if (isFinished) {
            statusChip.text = "Finished"
            statusChip.setTextColor(context.getColor(R.color.text_secondary))
            statusChip.chipBackgroundColor = context.getColorStateList(R.color.surface_variant)
        } else {
            statusChip.text = "Active"
            statusChip.setTextColor(context.getColor(R.color.success))
            statusChip.chipBackgroundColor = context.getColorStateList(R.color.success_light)
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val btnStopPlan = dialogView.findViewById<MaterialButton>(R.id.btn_stop_plan)
        if (isFinished) {
            btnStopPlan.visibility = View.GONE
        } else {
            btnStopPlan.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.stop_plan))
                    .setMessage("Stop reminding you for \"${plan.medicineName}\"? Remaining reminders for this plan will be cancelled.")
                    .setPositiveButton("Stop") { _, _ ->
                        onStopPlan()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        dialog.show()
    }
}