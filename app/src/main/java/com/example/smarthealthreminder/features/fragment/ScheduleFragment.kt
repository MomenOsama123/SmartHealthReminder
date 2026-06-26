package com.example.smarthealthreminder.features.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.repository.HealthRepository
import com.example.smarthealthreminder.features.adapter.CalendarDay
import com.example.smarthealthreminder.features.adapter.CalendarDayAdapter
import com.example.smarthealthreminder.features.schedule.details.DayDetailsActivity
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment() {

    private var recyclerCalendar: RecyclerView? = null
    private var calendarDayAdapter: CalendarDayAdapter? = null
    private var selectedDate = ""
    private var currentCalendar = Calendar.getInstance()

    // Dates that have content — drives dots on the calendar
    private var eventDates = setOf<String>()        // reminders
    private var noteDates = setOf<String>()          // notes
    private var reportDates = setOf<String>()        // reports
    private var scheduleEntryDates = setOf<String>() // schedule entries

    private val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())

    private val viewModel: HealthViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_schedule, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedDate = dateSdf.format(Date())

        // Calendar grid
        recyclerCalendar = view.findViewById(R.id.recycler_calendar)
        recyclerCalendar?.layoutManager = GridLayoutManager(requireContext(), 7)

        calendarDayAdapter = CalendarDayAdapter { date ->
            // Every tap opens DayDetailsActivity — it decides what to show
            selectedDate = date
            val cal = dateSdf.parse(date)
            val displayDate = cal?.let { displaySdf.format(it) } ?: date
            startActivity(
                Intent(requireContext(), DayDetailsActivity::class.java).apply {
                    putExtra(DayDetailsActivity.EXTRA_DATE, date)
                    putExtra(DayDetailsActivity.EXTRA_DATE_DISPLAY, displayDate)
                }
            )
        }
        recyclerCalendar?.adapter = calendarDayAdapter

        // Month navigation
        view.findViewById<ImageButton>(R.id.btn_prev_month)?.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            buildCalendar()
        }
        view.findViewById<ImageButton>(R.id.btn_next_month)?.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            buildCalendar()
        }

        // Observe all data sources to know which days get dots
        observeData()

        buildCalendar()
        updateSelectedDateLabel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh calendar when returning from DayDetailsActivity
        // (user may have added a note or reminder)
        buildCalendar()
        updateSelectedDateLabel()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { collectReminders() }
                launch { collectAlarms() }
                launch { collectScheduleEntries() }
                launch { collectReports() }
                launch { collectNotes() }
            }
        }
    }

    private suspend fun collectReminders() {
        viewModel.allReminders.collect { reminders ->
            eventDates = reminders
                .mapNotNull { normalizeDate(it.date).takeIf { d -> d.isNotBlank() } }
                .toSet()
            buildCalendar()
        }
    }

    private suspend fun collectAlarms() {
        // Active alarms are "daily" so they don't dot specific dates — skip
        viewModel.allAlarms.collect { buildCalendar() }
    }

    private suspend fun collectScheduleEntries() {
        viewModel.allScheduleEntries.collect { entries ->
            scheduleEntryDates = entries
                .mapNotNull { it.date?.takeIf { d -> d.isNotBlank() } }
                .toSet()
            buildCalendar()
        }
    }

    private suspend fun collectReports() {
        viewModel.allReports.collect { reports ->
            reportDates = reports
                .mapNotNull { it.date?.takeIf { d -> d.isNotBlank() } }
                .toSet()
            buildCalendar()
        }
    }

    private suspend fun collectNotes() {
        viewModel.allNoteDates.collect { dates ->
            noteDates = dates.filter { it.isNotBlank() }.toSet()
            buildCalendar()
        }
    }

    private fun buildCalendar() {
        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = dateSdf.format(Date())
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        view?.findViewById<TextView>(R.id.tv_month_year)?.text = monthSdf.format(cal.time)

        val days = mutableListOf<CalendarDay>()

        // Empty padding cells before day 1
        repeat(firstDayOfWeek) {
            days.add(CalendarDay("", 0, false, false, false))
        }

        for (day in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
            days.add(
                CalendarDay(
                    date = dateStr,
                    dayNumber = day,
                    isCurrentMonth = true,
                    isToday = dateStr == today,
                    hasEvents = eventDates.contains(dateStr),
                    hasNotes = noteDates.contains(dateStr),
                    hasReports = reportDates.contains(dateStr),
                    hasScheduleEntries = scheduleEntryDates.contains(dateStr)
                )
            )
        }

        calendarDayAdapter?.setDays(days, selectedDate)
    }

    private fun updateSelectedDateLabel() {
        val cal = dateSdf.parse(selectedDate)
        val label = if (selectedDate == dateSdf.format(Date())) "Today"
        else cal?.let { displaySdf.format(it) } ?: selectedDate

        view?.findViewById<TextView>(R.id.tv_selected_date)?.text = label

        // Count items for selected date
        val count = eventDates.count { it == selectedDate } +
                scheduleEntryDates.count { it == selectedDate } +
                reportDates.count { it == selectedDate } +
                noteDates.count { it == selectedDate }
        view?.findViewById<TextView>(R.id.tv_events_count)?.text =
            if (count > 0) "$count item${if (count > 1) "s" else ""}" else ""
    }

    private fun normalizeDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val sdf1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf1.isLenient = false
            val date1 = sdf1.parse(dateStr)
            if (date1 != null) return dateSdf.format(date1)

            val sdf2 = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            sdf2.isLenient = false
            val date2 = sdf2.parse(dateStr)
            if (date2 != null) return dateSdf.format(date2)

            dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCalendar = null
        calendarDayAdapter = null
    }
}
