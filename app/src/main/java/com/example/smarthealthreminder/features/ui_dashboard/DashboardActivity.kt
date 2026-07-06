package com.example.smarthealthreminder.ui_dashboard

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.alarm.ReminderReceiver
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.navigation.BottomNavHelper
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewModel: HealthViewModel
    private var firebaseId: String = ""

    private lateinit var tvGreeting: TextView
    private lateinit var tvAdherencePercent: TextView
    private lateinit var tvTotalMeds: TextView
    private lateinit var tvTakenToday: TextView
    private lateinit var tvMissedToday: TextView

    private lateinit var tvNextMedName: TextView
    private lateinit var tvNextMedTime: TextView
    private lateinit var btnMarkTaken: TextView
    private lateinit var btnSnooze: TextView

    private lateinit var layoutUpcomingMeds: View
    private lateinit var containerUpcomingItems: LinearLayout
    private lateinit var tvUpcomingTitle: TextView
    private lateinit var tvNoUpcoming: TextView
    private lateinit var fabAddMed: View

    private var reminderEntities: List<ReminderEntity> = emptyList()
    private var currentNextItem: ScheduleItem? = null

    private companion object {
        private const val PREFS_NAME = "DashboardPrefs"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_LAST_RESET_WEEK = "last_reset_week"
        private const val KEY_LAST_RESET_MONTH = "last_reset_month"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val sharedPref = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        firebaseId = auth.currentUser?.uid ?: sharedPref.getString("FIREBASE_ID", "") ?: ""

        if (firebaseId.isEmpty()) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        if (sharedPref.getString("FIREBASE_ID", "")?.isEmpty() == true) {
            sharedPref.edit { putString("FIREBASE_ID", firebaseId) }
        }

        val db = AppDatabase.getDatabase(this)
        val repository = HealthRepository(db)
        viewModel = ViewModelProvider(this, HealthViewModelFactory(repository))[HealthViewModel::class.java]

        initViews()
        setupClickListeners()
        setupBottomNavigation()

        lifecycleScope.launch {
            viewModel.allReminders.collect { reminders ->
                reminderEntities = reminders

                resetRecurringReminders()

                loadUserGreeting()
                loadStats()
                loadNextDose()
                loadUpcomingList()
            }
        }
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvAdherencePercent = findViewById(R.id.tvAdherencePercent)
        tvTotalMeds = findViewById(R.id.tvTotalMeds)
        tvTakenToday = findViewById(R.id.tvTakenToday)
        tvMissedToday = findViewById(R.id.tvMissedToday)

        tvNextMedName = findViewById(R.id.tvNextMedName)
        tvNextMedTime = findViewById(R.id.tvNextMedTime)
        btnMarkTaken = findViewById(R.id.btnMarkTaken)
        btnSnooze = findViewById(R.id.btnSnooze)

        layoutUpcomingMeds = findViewById(R.id.layoutUpcomingMeds)
        containerUpcomingItems = findViewById(R.id.containerUpcomingItems)
        tvUpcomingTitle = findViewById(R.id.tvUpcomingTitle)
        tvNoUpcoming = findViewById(R.id.tvNoUpcoming)
        fabAddMed = findViewById(R.id.fabAddMed)
    }

    private fun getCurrentMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun timeToMinutes(time: String?): Int {
        val hour = time?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = time?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    private fun isItemDue(item: ScheduleItem): Boolean {
        return getCurrentMinutes() >= item.minutes
    }

    /**
     * Check if reminder is in warning stage (2-10 min after due time)
     * Matches ReminderReceiver logic: Warning at 2min, Missed at 10min
     */
    private fun isWarningStage(item: ScheduleItem): Boolean {
        val now = getCurrentMinutes()
        return item.status.equals("Pending", true) &&
                now >= item.minutes + 2 &&
                now < item.minutes + 10
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getCurrentWeek(): Int {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        return cal.get(Calendar.WEEK_OF_YEAR)
    }

    private fun getCurrentMonth(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.MONTH) + 1
    }

    private fun resetRecurringReminders() {
        val today = getTodayString()
        val currentWeek = getCurrentWeek()
        val currentMonth = getCurrentMonth()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "")
        val lastResetWeek = prefs.getInt(KEY_LAST_RESET_WEEK, -1)
        val lastResetMonth = prefs.getInt(KEY_LAST_RESET_MONTH, -1)

        var needsResetDaily = false
        var needsResetWeekly = false
        var needsResetMonthly = false

        if (today != lastResetDate) needsResetDaily = true
        if (currentWeek != lastResetWeek) needsResetWeekly = true
        if (currentMonth != lastResetMonth) needsResetMonthly = true

        if (!needsResetDaily && !needsResetWeekly && !needsResetMonthly) return

        var anyReset = false

        reminderEntities.forEach { reminder ->
            if (!reminder.isRecurring) return@forEach

            val shouldReset = when (reminder.recurrenceType) {
                RecurrenceHelper.DAILY -> needsResetDaily
                RecurrenceHelper.WEEKLY -> needsResetWeekly
                RecurrenceHelper.MONTHLY -> needsResetMonthly
                else -> false
            }

            if (shouldReset && shouldShowToday(reminder)) {
                if (reminder.status == "Missed") {
                    viewModel.resetReminderStatus(reminder.id, "Pending")
                    anyReset = true
                }
            }
        }

        if (anyReset || needsResetDaily || needsResetWeekly || needsResetMonthly) {
            prefs.edit {
                if (needsResetDaily) putString(KEY_LAST_RESET_DATE, today)
                if (needsResetWeekly) putInt(KEY_LAST_RESET_WEEK, currentWeek)
                if (needsResetMonthly) putInt(KEY_LAST_RESET_MONTH, currentMonth)
            }
        }
    }

    private fun setupClickListeners() {
        fabAddMed.setOnClickListener {
            startActivity(Intent(this, AddReminderActivity::class.java))
        }

        btnMarkTaken.setOnClickListener {
            currentNextItem?.let { item ->
                if (isItemDue(item)) {
                    val reminder = reminderEntities.find { it.id == item.id }
                    reminder?.let {
                        viewModel.markReminderDone(it.id)
                        Toast.makeText(this, getString(R.string.marked_done), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.not_yet_time, item.time), Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, getString(R.string.no_medication), Toast.LENGTH_SHORT).show()
            }
        }

        btnSnooze.setOnClickListener {
            currentNextItem?.let { item ->
                if (isItemDue(item)) {
                    val reminder = reminderEntities.find { it.id == item.id }
                    reminder?.let {
                        val intent = Intent(this, ReminderReceiver::class.java).apply {
                            action = ReminderReceiver.ACTION_SNOOZE
                            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                            putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
                            // FIX: reminder.title is a non-null String, .orEmpty() was redundant
                            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
                            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, reminder.description.orEmpty())
                        }
                        sendBroadcast(intent)

                        Toast.makeText(this, getString(R.string.snoozed), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.not_yet_time, item.time), Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, getString(R.string.no_medication_to_snooze), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        BottomNavHelper.setup(this, bottomNav, R.id.nav_create)
    }

    private fun loadUserGreeting() {
        val sharedPref = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        if (sharedPref.getString("USER_NAME", null) == null) {
            val userName = auth.currentUser?.displayName
                ?: auth.currentUser?.email?.substringBefore("@")?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                ?: "User"
            sharedPref.edit { putString("USER_NAME", userName) }
        }

        // FIX: getString(key, "User") already can't return null here, the trailing "?: "User"" was redundant
        val userName = sharedPref.getString("USER_NAME", "User")

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            in 17..20 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }

        tvGreeting.text = getString(greetingRes, userName)
    }

    private fun shouldShowToday(reminder: ReminderEntity): Boolean {
        val today = Calendar.getInstance()

        if (!reminder.isRecurring) {
            val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)
            return reminder.date == todayString
        }

        return when (reminder.recurrenceType) {
            RecurrenceHelper.DAILY -> true

            RecurrenceHelper.WEEKLY -> {
                val reminderDateString = reminder.date ?: return false
                val reminderDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .parse(reminderDateString) ?: return false
                val reminderCal = Calendar.getInstance()
                reminderCal.time = reminderDate
                reminderCal.get(Calendar.DAY_OF_WEEK) == today.get(Calendar.DAY_OF_WEEK)
            }

            RecurrenceHelper.MONTHLY -> {
                val reminderDateString = reminder.date ?: return false
                val reminderDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .parse(reminderDateString) ?: return false
                val reminderCal = Calendar.getInstance()
                reminderCal.time = reminderDate
                reminderCal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            }

            else -> {
                val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)
                reminder.date == todayString
            }
        }
    }

    private fun loadStats() {
        val todayReminders = reminderEntities.filter { shouldShowToday(it) }

        val total = todayReminders.size
        val done = todayReminders.count { it.status == "Completed" }
        val missed = todayReminders.count { it.status == "Missed" }
        val pending = todayReminders.count { it.status == "Pending" }

        tvTotalMeds.text = total.toString()
        tvTakenToday.text = done.toString()
        tvMissedToday.text = missed.toString()

        val percentage = if (total > 0) (done * 100 / total) else 0
        tvAdherencePercent.text = getString(R.string.percentage_format, percentage)

        val prefs = getSharedPreferences("health_prefs", MODE_PRIVATE)
        prefs.edit {
            putInt("adherence_percent", percentage)
        }

        Log.d("DASH_STATS", "total=$total, done=$done, missed=$missed, pending=$pending")
    }

    private fun loadNextDose() {
        val currentMinutes = getCurrentMinutes()
        val allItems = mutableListOf<ScheduleItem>()

        reminderEntities
            .filter { it.status == "Pending" || it.status == "Snoozed" }
            .forEach { reminder ->
                if (!shouldShowToday(reminder)) return@forEach

                val reminderMinutes = timeToMinutes(reminder.time)
                if (reminderMinutes == 0 && reminder.time.isNullOrEmpty()) return@forEach

                allItems.add(ScheduleItem(
                    id = reminder.id,
                    name = reminder.title,
                    dosage = reminder.description.orEmpty(),
                    time = reminder.time.orEmpty(),
                    minutes = reminderMinutes,
                    type = "reminder",
                    category = reminder.category,
                    status = reminder.status
                ))
            }

        val nextItem = when {
            allItems.isEmpty() -> null
            else -> {
                val validSnoozed = allItems.filter {
                    it.status.equals("Snoozed", true) && currentMinutes >= it.minutes
                }
                val pendingItems = allItems.filter {
                    it.status.equals("Pending", true) && currentMinutes < it.minutes
                }

                when {
                    validSnoozed.isNotEmpty() -> validSnoozed.minByOrNull { it.minutes }
                    pendingItems.isNotEmpty() -> pendingItems.minByOrNull { it.minutes }
                    else -> null
                }
            }
        }

        currentNextItem = nextItem

        if (nextItem != null) {
            val displayName = if (nextItem.status.equals("Snoozed", true)) {
                getString(R.string.snoozed_prefix, nextItem.name)
            } else {
                nextItem.name
            }
            val displayDosage = if (nextItem.status.equals("Snoozed", true)) {
                getString(R.string.snoozed_dosage_suffix, nextItem.dosage)
            } else {
                nextItem.dosage
            }

            tvNextMedName.text = getString(R.string.med_name_dosage_format, displayName, displayDosage)
            tvNextMedTime.text = nextItem.time

            when {
                nextItem.status.equals("Snoozed", true) -> {
                    btnMarkTaken.visibility = View.VISIBLE
                    btnMarkTaken.isEnabled = true
                    btnMarkTaken.alpha = 1f
                    btnSnooze.visibility = View.GONE
                }
                else -> {
                    val isDue = isItemDue(nextItem)
                    btnMarkTaken.isEnabled = isDue
                    btnMarkTaken.alpha = if (isDue) 1.0f else 0.5f
                    btnSnooze.visibility = View.VISIBLE
                    btnSnooze.isEnabled = isDue
                    btnSnooze.alpha = if (isDue) 1.0f else 0.5f
                }
            }
        } else {
            tvNextMedName.setText(R.string.no_more_meds)
            tvNextMedTime.text = ""
            currentNextItem = null
            btnMarkTaken.isEnabled = false
            btnMarkTaken.alpha = 0.5f
            btnSnooze.isEnabled = false
            btnSnooze.alpha = 0.5f
        }
    }

    private fun loadUpcomingList() {
        val allItems = mutableListOf<ScheduleItem>()

        reminderEntities.forEach { reminder ->

            Log.d(
                "UPCOMING",
                "title=${reminder.title}, status=${reminder.status}, time=${reminder.time}"
            )

            if (!shouldShowToday(reminder)) return@forEach

            val reminderMinutes = timeToMinutes(reminder.time)
            if (reminderMinutes == 0 && reminder.time.isNullOrEmpty()) return@forEach

            val status = when (reminder.status) {
                "Completed" -> "Taken"
                "Snoozed" -> "Snoozed"
                "Missed" -> "Missed"
                else -> "Pending"
            }

            allItems.add(ScheduleItem(
                id = reminder.id,
                // FIX: reminder.title is a non-null String, .orEmpty() was redundant
                name = reminder.title,
                dosage = reminder.description.orEmpty(),
                time = reminder.time.orEmpty(),
                minutes = reminderMinutes,
                type = "reminder",
                category = reminder.category,
                status = status
            ))
        }

        val upcoming = allItems.sortedBy { it.minutes }
        removeDynamicViews()

        if (upcoming.isEmpty()) {
            tvNoUpcoming.visibility = View.VISIBLE
            tvNoUpcoming.setText(R.string.no_more_meds_today)
        } else {
            tvNoUpcoming.visibility = View.GONE
            upcoming.forEachIndexed { index, item ->
                val itemView = buildUpcomingItemView(item)
                containerUpcomingItems.addView(itemView)

                val isClickable = item.status == "Pending" || item.status == "Snoozed"
                itemView.isClickable = isClickable
                itemView.isFocusable = isClickable

                if (isClickable) {
                    itemView.setOnClickListener {
                        val reminder = reminderEntities.find { it.id == item.id }
                        reminder?.let { showReminderActions(it, item) }
                    }
                    itemView.alpha = 1.0f
                } else {
                    itemView.setOnClickListener(null)
                    itemView.alpha = 0.5f
                }

                if (index < upcoming.size - 1) {
                    val separator = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        ).apply { setMargins(0, 16, 0, 16) }
                        setBackgroundColor(ContextCompat.getColor(this@DashboardActivity, R.color.hint_gray))
                    }
                    containerUpcomingItems.addView(separator)
                }
            }
        }
    }

    /**
     * Show actions based on reminder stage
     * First 2 min: Mark as Done + Snooze 10 min + Cancel
     * 2-10 min (Warning): Mark as Done + Cancel
     * Snoozed: Mark as Done + Cancel
     */
    private fun showReminderActions(reminder: ReminderEntity, item: ScheduleItem) {
        val options = when {
            item.status.equals("Snoozed", true) ->
                arrayOf(getString(R.string.mark_as_done), getString(R.string.cancel))

            isWarningStage(item) ->
                arrayOf(getString(R.string.mark_as_done), getString(R.string.cancel))

            else ->
                arrayOf(getString(R.string.mark_as_done), getString(R.string.snooze_10_min), getString(R.string.cancel))
        }

        AlertDialog.Builder(this)
            .setTitle(reminder.title)
            .setItems(options) { _, which ->
                if (item.status.equals("Snoozed", true) || isWarningStage(item)) {
                    when (which) {
                        0 -> {
                            if (!isItemDue(item)) {
                                Toast.makeText(this, getString(R.string.not_yet_time_short), Toast.LENGTH_SHORT).show()
                                return@setItems
                            }
                            viewModel.markReminderDone(reminder.id)
                            Toast.makeText(this, getString(R.string.marked_done), Toast.LENGTH_SHORT).show()
                        }
                        1 -> { /* Cancel */ }
                    }
                } else {
                    when (which) {
                        0 -> {
                            if (!isItemDue(item)) {
                                Toast.makeText(this, getString(R.string.not_yet_time_short), Toast.LENGTH_SHORT).show()
                                return@setItems
                            }
                            viewModel.markReminderDone(reminder.id)
                            Toast.makeText(this, getString(R.string.marked_done), Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            if (!isItemDue(item)) {
                                Toast.makeText(this, getString(R.string.not_yet_time_short), Toast.LENGTH_SHORT).show()
                                return@setItems
                            }
                            val intent = Intent(this, ReminderReceiver::class.java).apply {
                                action = ReminderReceiver.ACTION_SNOOZE
                                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                                putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
                                // FIX: reminder.title is a non-null String, .orEmpty() was redundant
                                putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
                                putExtra(ReminderReceiver.EXTRA_DESCRIPTION, reminder.description.orEmpty())
                            }
                            sendBroadcast(intent)

                            Toast.makeText(this, getString(R.string.snoozed), Toast.LENGTH_SHORT).show()
                        }
                        2 -> { /* Cancel */ }
                    }
                }
            }
            .show()
    }

    private fun removeDynamicViews() {
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until containerUpcomingItems.childCount) {
            val view = containerUpcomingItems.getChildAt(i)
            if (view.id != R.id.tvUpcomingTitle && view.id != R.id.tvNoUpcoming) {
                viewsToRemove.add(view)
            }
        }
        viewsToRemove.forEach { containerUpcomingItems.removeView(it) }
    }

    private fun buildUpcomingItemView(item: ScheduleItem): LinearLayout {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(44.dpToPx(), 44.dpToPx()).apply {
                marginEnd = 12.dpToPx()
            }
            setBackgroundResource(R.drawable.bg_card_beige)
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            setImageResource(
                when {
                    (item.category ?: "").lowercase() in listOf("capsule", "pill") -> R.drawable.capsule
                    (item.category ?: "").lowercase() == "injection" -> R.drawable.health_cross
                    (item.category ?: "").lowercase() == "syrup" -> R.drawable.health_cross
                    else -> R.drawable.ic_pill
                }
            )
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val displayName = when {
            item.status.equals("Taken", true) -> getString(R.string.taken_prefix, item.name)
            item.status.equals("Snoozed", true) -> getString(R.string.snoozed_prefix, item.name)
            item.status.equals("Missed", true) -> getString(R.string.missed_prefix, item.name)
            else -> item.name
        }

        val nameView = TextView(this).apply {
            text = displayName
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.text_dark))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }

        val displayDosage = when {
            item.status.equals("Taken", true) -> getString(R.string.taken_dosage_suffix, item.dosage)
            item.status.equals("Snoozed", true) -> getString(R.string.snoozed_dosage_suffix, item.dosage)
            item.status.equals("Missed", true) -> getString(R.string.missed_dosage_suffix, item.dosage)
            else -> item.dosage
        }

        val dosageView = TextView(this).apply {
            text = displayDosage
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.hint_gray))
            textSize = 12f
        }

        textLayout.addView(nameView)
        textLayout.addView(dosageView)

        val timeView = TextView(this).apply {
            text = item.time
            setTextColor(
                when {
                    item.status.equals("Snoozed", true) -> ContextCompat.getColor(this@DashboardActivity, R.color.primary)
                    item.status.equals("Missed", true) -> ContextCompat.getColor(this@DashboardActivity, android.R.color.holo_red_dark)
                    else -> ContextCompat.getColor(this@DashboardActivity, R.color.hint_gray)
                }
            )
            textSize = 12f
        }

        rowLayout.addView(iconView)
        rowLayout.addView(textLayout)
        rowLayout.addView(timeView)

        return rowLayout
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    data class ScheduleItem(
        val id: String,
        val name: String,
        val dosage: String,
        val time: String,
        val minutes: Int,
        val type: String,
        val category: String?,
        val status: String = "PENDING"
    )
}