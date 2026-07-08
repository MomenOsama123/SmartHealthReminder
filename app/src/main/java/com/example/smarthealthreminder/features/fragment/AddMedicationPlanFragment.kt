package com.example.smarthealthreminder.features.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.alarm.ReminderScheduler
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.util.*

class AddMedicationPlanFragment : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etDosage: EditText
    private lateinit var etInstructions: EditText
    private lateinit var btnAddTime: MaterialButton
    private lateinit var chipGroupTimes: ChipGroup
    private lateinit var tvNoTimes: View
    private lateinit var etStartDate: EditText
    private lateinit var etDurationValue: EditText
    private lateinit var toggleUnit: MaterialButtonToggleGroup
    private lateinit var btnSave: MaterialButton

    private lateinit var repository: HealthRepository
    private val selectedTimes = mutableListOf<String>()
    private var selectedStartDate: String = RecurrenceHelper.getTodayString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_medication_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = HealthRepository(AppDatabase.getDatabase(requireContext()))

        etName = view.findViewById(R.id.et_medicine_name)
        etDosage = view.findViewById(R.id.et_dosage)
        etInstructions = view.findViewById(R.id.et_instructions)
        btnAddTime = view.findViewById(R.id.btn_add_time)
        chipGroupTimes = view.findViewById(R.id.chip_group_times)
        tvNoTimes = view.findViewById(R.id.tv_no_times)
        etStartDate = view.findViewById(R.id.et_start_date)
        etDurationValue = view.findViewById(R.id.et_duration_value)
        toggleUnit = view.findViewById(R.id.toggle_duration_unit)
        btnSave = view.findViewById(R.id.btn_save_plan)

        etStartDate.setText(selectedStartDate)

        view.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        etStartDate.setOnClickListener { showStartDatePicker() }
        btnAddTime.setOnClickListener { showAddTimeDialog() }
        btnSave.setOnClickListener { savePlan() }

        updateTimesUi()
    }

    private fun showStartDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedStartDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                etStartDate.setText(selectedStartDate)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showAddTimeDialog() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                if (!selectedTimes.contains(time)) {
                    selectedTimes.add(time)
                    selectedTimes.sort()
                    updateTimesUi()
                }
            },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        ).show()
    }

    private fun updateTimesUi() {
        chipGroupTimes.removeAllViews()
        tvNoTimes.visibility = if (selectedTimes.isEmpty()) View.VISIBLE else View.GONE

        selectedTimes.forEach { time ->
            val chip = Chip(requireContext()).apply {
                text = time
                isCloseIconVisible = true
                setChipBackgroundColorResource(R.color.surface_variant)
                setTextColor(requireContext().getColor(R.color.text_primary))
                setOnCloseIconClickListener {
                    selectedTimes.remove(time)
                    updateTimesUi()
                }
            }
            chipGroupTimes.addView(chip)
        }
    }

    private fun savePlan() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            etName.error = "Please enter medicine name"
            return
        }
        if (selectedTimes.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.please_add_at_least_one_time), Toast.LENGTH_SHORT).show()
            return
        }
        val durationValue = etDurationValue.text.toString().toIntOrNull()
        if (durationValue == null || durationValue <= 0) {
            etDurationValue.error = getString(R.string.enter_a_valid_duration)
            return
        }

        val durationDays = when (toggleUnit.checkedButtonId) {
            R.id.btn_unit_weeks -> durationValue * 7
            R.id.btn_unit_months -> durationValue * 30
            else -> durationValue
        }

        val startMillis = RecurrenceHelper.parseDateTimeMillis(selectedStartDate, "00:00") ?: System.currentTimeMillis()
        val endMillis = startMillis + (durationDays.toLong() * 24 * 60 * 60 * 1000L)
        val endDate = RecurrenceHelper.formatDate(endMillis)

        val dosage = etDosage.text.toString().trim().ifEmpty { null }
        val instructions = etInstructions.text.toString().trim().ifEmpty { null }
        val planId = UUID.randomUUID().toString()

        val plan = MedicationPlanEntity(
            id = planId,
            medicineName = name,
            dosage = dosage,
            timesPerDay = selectedTimes.size,
            timesOfDay = selectedTimes.joinToString(","),
            startDate = selectedStartDate,
            durationDays = durationDays,
            endDate = endDate,
            instructions = instructions
        )

        btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            repository.insertMedicationPlan(plan)

            selectedTimes.forEach { time ->
                val reminder = ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = name,
                    description = instructions,
                    category = "Medicine",
                    date = selectedStartDate,
                    time = time,
                    priority = "Medium",
                    status = "Pending",
                    isRecurring = true,
                    recurrenceType = RecurrenceHelper.DAILY,
                    planId = planId,
                    endDate = endDate
                )
                repository.insertReminder(reminder)

                val triggerMillis = RecurrenceHelper.computeNextTriggerMillis(
                    reminder.date, reminder.time, reminder.recurrenceType
                )
                if (triggerMillis != null) {
                    ReminderScheduler.scheduleReminder(requireContext(), reminder, triggerMillis)
                }
            }

            Toast.makeText(
                requireContext(),
                "Plan saved: $name will remind you ${selectedTimes.size} time(s) a day until $endDate",
                Toast.LENGTH_LONG
            ).show()
            parentFragmentManager.popBackStack()
        }
    }
}