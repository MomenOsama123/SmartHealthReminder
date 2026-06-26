package com.example.smarthealthreminder.features.schedule.details

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddNoteActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATE = "extra_note_date"
        const val EXTRA_DATE_DISPLAY = "extra_note_date_display"
        const val EXTRA_EXISTING_NOTE = "extra_existing_note"
    }

    private lateinit var viewModel: HealthViewModel
    private lateinit var etNote: TextInputEditText
    private lateinit var tvDate: TextView
    private var date: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        date = intent.getStringExtra(EXTRA_DATE) ?: ""
        val dateDisplay = intent.getStringExtra(EXTRA_DATE_DISPLAY) ?: date
        val existingNote = intent.getStringExtra(EXTRA_EXISTING_NOTE) ?: ""

        val db = AppDatabase.getDatabase(this)
        val repository = HealthRepository(db)
        viewModel = HealthViewModelFactory(repository).create(HealthViewModel::class.java)

        tvDate = findViewById(R.id.tv_note_date)
        etNote = findViewById(R.id.et_note_content)

        tvDate.text = dateDisplay
        if (existingNote.isNotBlank()) {
            etNote.setText(existingNote)
            etNote.setSelection(existingNote.length)
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btn_save_note).setOnClickListener {
            saveNote()
        }
    }

    private fun saveNote() {
        val noteText = etNote.text.toString().trim()
        if (noteText.isEmpty()) {
            etNote.error = "Please write something before saving"
            return
        }
        if (date.isBlank()) {
            Toast.makeText(this, "Invalid date", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            viewModel.saveNote(date, noteText)
            Toast.makeText(this@AddNoteActivity, "Note saved", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
}
