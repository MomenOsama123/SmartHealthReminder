package com.example.smarthealthreminder.features.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.core.base.BaseActivity
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.ScheduleEntryEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class CreateScheduleEntryActivity : BaseActivity() {

    companion object {
        const val EXTRA_SELECTED_DATE = "extra_selected_date"
    }

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var etTime: TextInputEditText
    private lateinit var etCategory: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private var selectedDate: String = ""
    private var selectedTime: String = ""

    private lateinit var repository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_schedule_entry)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = AppDatabase.getDatabase(this)
        repository = HealthRepository(db)

        initViews()
        setupListeners()

        val prefillDate = intent.getStringExtra(EXTRA_SELECTED_DATE)
        if (prefillDate != null) {
            selectedDate = prefillDate
            etDate.setText(selectedDate)
        } else {
            val calendar = Calendar.getInstance()
            selectedDate = String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            etDate.setText(selectedDate)
        }

        selectedTime = String.format(
            "%02d:%02d",
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE)
        )
        etTime.setText(selectedTime)
        etCategory.setText(getString(R.string.custom))
    }

    private fun initViews() {
        etTitle = findViewById(R.id.et_title)
        etDescription = findViewById(R.id.et_description)
        etDate = findViewById(R.id.et_date)
        etTime = findViewById(R.id.et_time)
        etCategory = findViewById(R.id.et_category)
        btnSave = findViewById(R.id.btn_save)
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        etDate.setOnClickListener { showDatePicker() }
        etTime.setOnClickListener { showTimePicker() }

        btnSave.setOnClickListener { saveEntry() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                etDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                etTime.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveEntry() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            etTitle.error = "Please enter a title"
            return
        }

        val description = etDescription.text.toString().trim()
        val category = etCategory.text.toString().trim().ifEmpty { getString(R.string.custom) }

        val entry = ScheduleEntryEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            date = selectedDate,
            time = selectedTime,
            category = category
        )

        lifecycleScope.launch {
            repository.insertScheduleEntry(entry)
            Toast.makeText(this@CreateScheduleEntryActivity, "Schedule entry saved!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
}
