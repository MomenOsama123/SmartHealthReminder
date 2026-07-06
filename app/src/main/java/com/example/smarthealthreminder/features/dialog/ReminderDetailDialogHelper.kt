package com.example.smarthealthreminder.features.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.smarthealthreminder.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

object ReminderDetailDialogHelper {

    fun show(
        context: Context,
        title: String,
        description: String?,
        date: String?,
        time: String?,
        category: String?,
        status: String?,
        onMarkDone: () -> Unit,
        onMarkMissed: () -> Unit,
        onDelete: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reminder_detail, null)

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = title
        dialogView.findViewById<TextView>(R.id.tv_dialog_description).text =
            if (description.isNullOrBlank()) "No description provided." else description
        dialogView.findViewById<TextView>(R.id.tv_dialog_date).text = date ?: "--"
        dialogView.findViewById<TextView>(R.id.tv_dialog_time).text = time ?: "--:--"
        dialogView.findViewById<TextView>(R.id.tv_dialog_category).text = category ?: "General"

        val statusText = status ?: "PENDING"
        val statusChip = dialogView.findViewById<Chip>(R.id.chip_dialog_status)
        statusChip.text = statusText
        when (statusText.uppercase()) {
            "MISSED" -> {
                statusChip.setTextColor(context.getColor(R.color.missed))
                statusChip.chipBackgroundColor = context.getColorStateList(R.color.missed_light)
            }
            "DONE", "COMPLETED" -> {
                statusChip.setTextColor(context.getColor(R.color.success))
                statusChip.chipBackgroundColor = context.getColorStateList(R.color.success_light)
            }
            "SNOOZED" -> {
                statusChip.setTextColor(context.getColor(R.color.pending))
                statusChip.chipBackgroundColor = context.getColorStateList(R.color.surface_variant)
            }
            else -> {
                statusChip.setTextColor(context.getColor(R.color.pending))
                statusChip.chipBackgroundColor = context.getColorStateList(R.color.surface_variant)
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val btnMarkDone = dialogView.findViewById<MaterialButton>(R.id.btn_mark_done)
        val btnMarkMissed = dialogView.findViewById<MaterialButton>(R.id.btn_mark_missed)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btn_delete)

        when (statusText.uppercase()) {
            "COMPLETED", "DONE" -> {
                btnMarkDone.visibility = View.GONE
                btnMarkMissed.visibility = View.VISIBLE
            }
            "MISSED" -> {
                btnMarkDone.visibility = View.VISIBLE
                btnMarkMissed.visibility = View.GONE
            }
            else -> {
                btnMarkDone.visibility = View.VISIBLE
                btnMarkMissed.visibility = View.VISIBLE
            }
        }

        btnMarkDone.setOnClickListener {
            onMarkDone()
            dialog.dismiss()
        }

        btnMarkMissed.setOnClickListener {
            onMarkMissed()
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete \"$title\"?")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }
}