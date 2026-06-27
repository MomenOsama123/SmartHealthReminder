package com.example.smarthealthreminder.features.ui_d

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.data.DatabaseHelper
import com.example.smarthealthreminder.features.navigation.BottomNavHelper
import com.example.smarthealthreminder.features.auth.signIn.SignInActivity
import com.example.smarthealthreminder.features.model.Alarm
import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.alarm.AlarmHelper
import com.example.smarthealthreminder.features.alarm.ReminderReceiver
import com.example.smarthealthreminder.features.model_d.User
import com.example.smarthealthreminder.features.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var currentUserId: Int = -1
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

    private var alarmsList: List<Alarm> = emptyList()
    private var remindersList: List<Reminder> = emptyList()

    private var currentNextItem: ScheduleItem? = null

    // ✅ Handler for auto-refresh every 10 seconds (for testing)
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            Log.d("DASH", "Auto-refreshing data...")
            loadDashboardData()
            handler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val sharedPref = getSharedPreferences("HealthSyncPrefs", MODE_PRIVATE)
        val auth = FirebaseAuth.getInstance()

        // ✅ Fallback: if SharedPreferences is empty but Firebase user is logged in, use Firebase UID
        firebaseId = auth.currentUser?.uid ?: sharedPref.getString("FIREBASE_ID", "") ?: ""

        Log.d("DASH_DEBUG", "firebaseId = '$firebaseId'")

        if (firebaseId.isEmpty()) {
            Log.d("DASH_DEBUG", "firebaseId is empty, going to SignIn")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Ensure FIREBASE_ID is persisted for next time
        if (sharedPref.getString("FIREBASE_ID", "")?.isEmpty() == true) {
            sharedPref.edit().putString("FIREBASE_ID", firebaseId).apply()
        }

        dbHelper = DatabaseHelper(this)

        // ✅ Auto-insert user into SQLite if not present
        var user = dbHelper.getUserByFirebaseId(firebaseId)
        if (user == null) {
            val userName = auth.currentUser?.displayName ?: "User"
            val userEmail = auth.currentUser?.email ?: ""
            dbHelper.insertUser(
                User(
                    id = 0,
                    firebaseId = firebaseId,
                    name = userName,
                    email = userEmail
                )
            )
            user = dbHelper.getUserByFirebaseId(firebaseId)
            Log.d("DASH_DEBUG", "Auto-inserted user into SQLite: $firebaseId")
        }

        Log.d("DASH_DEBUG", "user = $user, userId = ${user?.id}")

        currentUserId = user?.id ?: -1

        if (currentUserId == -1) {
            Log.d("DASH_DEBUG", "user not found in DB after insert, going to SignIn")
            sharedPref.edit().remove("FIREBASE_ID").apply()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        initViews()
        setupClickListeners()
        setupBottomNavigation()
        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        Log.d("DASH", "onResume - refreshing data and starting auto-refresh")
        handler.removeCallbacks(refreshRunnable)
        loadDashboardData()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        Log.d("DASH", "onPause - stopping auto-refresh")
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
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

    // ✅✅✅ دوال مساعدة موحدة للوقت
    private fun getCurrentMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val result = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        Log.d("TIME_DEBUG", "getCurrentTime() returned: $result")
        return result
    }

    // ✅✅✅ دالة موحدة لتحويل Alarm لـ minutes
    private fun alarmToMinutes(alarm: Alarm): Int {
        var hour = alarm.time?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = alarm.time?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0

        if (alarm.amPm == "PM" && hour != 12) hour += 12
        if (alarm.amPm == "AM" && hour == 12) hour = 0

        return hour * 60 + minute
    }

    // ✅✅✅ دالة موحدة لتحويل Reminder لـ minutes (24h format)
    private fun reminderToMinutes(reminder: Reminder): Int {
        val hour = reminder.time?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = reminder.time?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    // ✅ حساب isDue لحظياً من الوقت الحقيقي
    private fun isItemDue(item: ScheduleItem): Boolean {
        val currentMinutes = getCurrentMinutes()
        Log.d("TIME_DEBUG", "isItemDue: currentMinutes=$currentMinutes, itemMinutes=${item.minutes}")
        return currentMinutes >= item.minutes
    }

    // ✅✅✅ حساب Missed لحظياً
    private fun isAlarmMissed(alarm: Alarm): Boolean {
        val nowMinutes = getCurrentMinutes()
        val alarmMinutes = alarmToMinutes(alarm)
        return alarm.status != "TAKEN" && nowMinutes > alarmMinutes
    }

    private fun isReminderMissed(reminder: Reminder): Boolean {
        val nowMinutes = getCurrentMinutes()
        val reminderMinutes = reminderToMinutes(reminder)
        return reminder.status != "Done" && nowMinutes > reminderMinutes
    }

    private fun setupClickListeners() {
        fabAddMed.setOnClickListener {
            startActivity(Intent(this, AddReminderActivity::class.java))
        }

        btnMarkTaken.setOnClickListener {
            Log.d("DASH_DEBUG", "btnMarkTaken clicked")
            currentNextItem?.let { item ->
                if (isItemDue(item)) {
                    handleMarkTaken(item)
                } else {
                    Toast.makeText(this, "Not yet time! Wait until ${item.time}", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "No medication to mark", Toast.LENGTH_SHORT).show()
            }
        }

        btnSnooze.setOnClickListener {
            Log.d("DASH_DEBUG", "btnSnooze clicked")
            currentNextItem?.let { item ->
                if (isItemDue(item)) {
                    handleSnooze(item)
                } else {
                    Toast.makeText(this, "Not yet time! Wait until ${item.time}", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "No medication to snooze", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleMarkTaken(item: ScheduleItem) {
        Log.d("DASH_DEBUG", "handleMarkTaken: item.id=${item.id}, type=${item.type}")

        when (item.type) {
            "alarm" -> {
                val alarm = alarmsList.find { it.id == item.id }
                alarm?.let {
                    it.id?.let { id ->
                        dbHelper.markAlarmAsTaken(id)
                        val alarmHelper = AlarmHelper(this)
                        alarmHelper.cancelAlarm(id)
                        Toast.makeText(this, "Marked as taken", Toast.LENGTH_SHORT).show()
                        loadDashboardData()
                    } ?: Log.e("DASH_DEBUG", "alarm.id is null!")
                } ?: run {
                    Log.e("DASH_DEBUG", "Alarm not found with id=${item.id}")
                    Toast.makeText(this, "Error: Alarm not found", Toast.LENGTH_SHORT).show()
                }
            }
            "reminder" -> {
                val reminder = remindersList.find { it.id == item.id }
                reminder?.let {
                    it.id?.let { id ->
                        dbHelper.updateReminderStatus(id, "Done")
                        Toast.makeText(this, "Marked as done", Toast.LENGTH_SHORT).show()
                        loadDashboardData()
                    } ?: Log.e("DASH_DEBUG", "reminder.id is null!")
                } ?: run {
                    Log.e("DASH_DEBUG", "Reminder not found with id=${item.id}")
                    Toast.makeText(this, "Error: Reminder not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleSnooze(item: ScheduleItem) {
        if (item.type == "alarm") {
            val alarm = alarmsList.find { it.id == item.id }
            alarm?.let {
                it.id?.let { id ->
                    performSnooze(id)
                }
            } ?: run {
                Toast.makeText(this, "Error: Cannot snooze", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (item.type == "reminder") {
            val snoozeIntent = Intent(this, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_SNOOZE
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, item.id)
                putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
            }
            sendBroadcast(snoozeIntent)

            Toast.makeText(
                this,
                getString(
                    R.string.snoozed_for_minutes,
                    SettingsActivity.getReminderSnoozeMinutes(this)
                ),
                Toast.LENGTH_SHORT
            ).show()

            loadDashboardData()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        BottomNavHelper.setup(this, bottomNav, R.id.nav_create)
    }

    private fun loadDashboardData() {
        val currentTime = getCurrentTime()

        loadUserGreeting()
        loadAlarmsFromDatabase()
        loadRemindersFromDatabase()

        Log.d("DASH", "alarms size = ${alarmsList.size}, reminders size = ${remindersList.size}")

        loadStats()
        loadNextDose(currentTime)
        loadUpcomingList(currentTime)
    }

    private fun loadAlarmsFromDatabase() {
        alarmsList = dbHelper.getAlarmsByUserId(currentUserId)
    }

    private fun loadRemindersFromDatabase() {
        remindersList = dbHelper.getRemindersByUserId(currentUserId)
    }

    private fun loadUserGreeting() {
        val user = dbHelper.getUserById(currentUserId)
        val userName = user?.name ?: "User"

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 5..11 -> "Good Morning, $userName ☀️"
            in 12..16 -> "Good Afternoon, $userName 🌤️"
            in 17..20 -> "Good Evening, $userName 🌅"
            else -> "Good Night, $userName 🌙"
        }

        tvGreeting.text = greeting
    }

    // ✅✅✅ loadStats مع حساب Missed لحظي
    private fun loadStats() {
        val nowMinutes = getCurrentMinutes()

        val totalAlarms = alarmsList.size
        val totalReminders = remindersList.size
        val total = totalAlarms + totalReminders

        val takenAlarms = alarmsList.count { it.status == "TAKEN" }
        val doneReminders = remindersList.count { it.status == "Done" }
        val taken = takenAlarms + doneReminders

        // ✅✅✅ MISSED لحظي (الوقت عدى ولسه مش متاخد)
        val missedAlarms = alarmsList.count { isAlarmMissed(it) }
        val missedReminders = remindersList.count { isReminderMissed(it) }
        val missedTotal = missedAlarms + missedReminders

        val snoozedAlarms = alarmsList.count { it.status == "SNOOZED" }
        val snoozedReminders = remindersList.count { it.status == "Snoozed" }

        tvTotalMeds.text = total.toString()
        tvTakenToday.text = taken.toString()
        tvMissedToday.text = missedTotal.toString()

        val percentage = if (total > 0) (taken * 100 / total) else 0
        tvAdherencePercent.text = "$percentage%"

        Log.d("DASH_STATS", "total=$total, taken=$taken, missed=$missedTotal, snoozed=${snoozedAlarms + snoozedReminders}")
    }

    private fun loadNextDose(currentTime: String) {
        val currentMinutes = getCurrentMinutes()

        val allItems = mutableListOf<ScheduleItem>()

        alarmsList
            .filter { it.isActive && (it.status == "PENDING" || it.status == "SNOOZED") }
            .forEach { alarm ->
                val alarmMinutes = alarmToMinutes(alarm)
                if (alarmMinutes == 0 && alarm.time.isNullOrEmpty()) return@forEach

                allItems.add(ScheduleItem(
                    id = alarm.id ?: "",
                    name = alarm.label ?: "Unknown",
                    dosage = alarm.dosage ?: "",
                    time = "${alarm.time} ${alarm.amPm}",
                    minutes = alarmMinutes,
                    type = "alarm",
                    category = alarm.category,
                    status = alarm.status
                ))
            }

        remindersList
            .filter { it.status == "Pending" || it.status == "Snoozed" }
            .forEach { reminder ->
                val reminderMinutes = reminderToMinutes(reminder)
                if (reminderMinutes == 0 && reminder.time.isNullOrEmpty()) return@forEach

                allItems.add(ScheduleItem(
                    id = reminder.id ?: "",
                    name = reminder.title ?: "Unknown",
                    dosage = reminder.description ?: "",
                    time = reminder.time ?: "",
                    minutes = reminderMinutes,
                    type = "reminder",
                    category = reminder.category,
                    status = reminder.status ?: "Pending"
                ))
            }

        // ✅ Find the closest item to current time
        val nextItem = when {
            allItems.isEmpty() -> null
            else -> {
                val dueItems = allItems.filter { currentMinutes >= it.minutes }
                val upcomingItems = allItems.filter { currentMinutes < it.minutes }

                when {
                    dueItems.isNotEmpty() -> {
                        Log.d("TIME_DEBUG", "Found ${dueItems.size} due items")
                        dueItems.maxByOrNull { it.minutes }
                    }
                    upcomingItems.isNotEmpty() -> {
                        Log.d("TIME_DEBUG", "No due items, picking closest upcoming")
                        upcomingItems.minByOrNull { it.minutes }
                    }
                    else -> null
                }
            }
        }

        currentNextItem = nextItem

        if (nextItem != null) {
            tvNextMedName.text = "${nextItem.name} - ${nextItem.dosage}"
            tvNextMedTime.text = nextItem.time

            if (nextItem.status.equals("SNOOZED", true) || nextItem.status.equals("Snoozed", true)) {
                tvNextMedName.text = "⏰ ${nextItem.name} - ${nextItem.dosage}"
            }

            val isDue = isItemDue(nextItem)
            Log.d("TIME_DEBUG", "Setting buttons: isDue=$isDue")

            btnMarkTaken.isEnabled = isDue
            btnMarkTaken.alpha = if (isDue) 1.0f else 0.5f

            btnSnooze.isEnabled = isDue
            btnSnooze.alpha = if (isDue) 1.0f else 0.5f

            if (nextItem.type == "alarm" && isDue) {
                val alarm = alarmsList.find { it.id == nextItem.id }
                alarm?.let {
                    val alarmHelper = AlarmHelper(this)
                    alarmHelper.scheduleAlarm(it)
                    Log.d("DASH", "Scheduled alarm: ${it.id} at ${it.time} ${it.amPm}")
                }
            }
        } else {
            tvNextMedName.text = "No more medications"
            tvNextMedTime.text = ""
            currentNextItem = null

            btnMarkTaken.isEnabled = false
            btnMarkTaken.alpha = 0.5f
            btnSnooze.isEnabled = false
            btnSnooze.alpha = 0.5f
        }
    }

    private fun performSnooze(alarmId: String) {
        Log.d("DASH_DEBUG", "performSnooze: Sending snooze broadcast for alarmId=$alarmId")

        val snoozeIntent = Intent(this, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, alarmId)
            putExtra(ReminderReceiver.EXTRA_TYPE, "alarm")
        }
        sendBroadcast(snoozeIntent)

        Toast.makeText(
            this,
            getString(
                R.string.snoozed_for_minutes,
                SettingsActivity.getAlarmSnoozeMinutes(this)
            ),
            Toast.LENGTH_SHORT
        ).show()
        loadDashboardData()
    }

    private fun loadUpcomingList(currentTime: String) {
        val currentMinutes = getCurrentMinutes()

        val allItems = mutableListOf<ScheduleItem>()

        alarmsList
            .filter { it.isActive && (it.status == "PENDING" || it.status == "SNOOZED") }
            .forEach { alarm ->
                val alarmMinutes = alarmToMinutes(alarm)
                if (alarmMinutes == 0 && alarm.time.isNullOrEmpty()) return@forEach

                allItems.add(ScheduleItem(
                    id = alarm.id ?: "",
                    name = alarm.label ?: "Unknown",
                    dosage = alarm.dosage ?: "",
                    time = "${alarm.time} ${alarm.amPm}",
                    minutes = alarmMinutes,
                    type = "alarm",
                    category = alarm.category,
                    status = alarm.status
                ))
            }

        remindersList
            .filter { it.status == "Pending" || it.status == "Snoozed" }
            .forEach { reminder ->
                val reminderMinutes = reminderToMinutes(reminder)
                if (reminderMinutes == 0 && reminder.time.isNullOrEmpty()) return@forEach

                allItems.add(ScheduleItem(
                    id = reminder.id ?: "",
                    name = reminder.title ?: "Unknown",
                    dosage = reminder.description ?: "",
                    time = reminder.time ?: "",
                    minutes = reminderMinutes,
                    type = "reminder",
                    category = reminder.category,
                    status = reminder.status ?: "Pending"
                ))
            }

        val upcoming = allItems.sortedBy { it.minutes }

        removeDynamicViews()

        if (upcoming.isEmpty()) {
            tvNoUpcoming.visibility = View.VISIBLE
            tvNoUpcoming.text = "No more medications for today 🎉"
        } else {
            tvNoUpcoming.visibility = View.GONE

            upcoming.forEachIndexed { index, item ->
                val itemView = buildUpcomingItemView(item)
                containerUpcomingItems.addView(itemView)

                itemView.setOnClickListener {
                    when (item.type) {
                        "alarm" -> {
                            val alarm = alarmsList.find { it.id == item.id }
                            alarm?.let { showAlarmActions(it, item) }
                        }
                        "reminder" -> {
                            val reminder = remindersList.find { it.id == item.id }
                            reminder?.let { showReminderActions(it, item) }
                        }
                    }
                }

                if (index < upcoming.size - 1) {
                    val separator = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        ).apply {
                            setMargins(0, 16, 0, 16)
                        }
                        setBackgroundColor(ContextCompat.getColor(this@DashboardActivity, R.color.hint_gray))
                    }
                    containerUpcomingItems.addView(separator)
                }
            }
        }
    }

    private fun showAlarmActions(alarm: Alarm, item: ScheduleItem) {
        val options = arrayOf("Mark as Taken", "Snooze 15 min", "Cancel")

        AlertDialog.Builder(this)
            .setTitle(alarm.label ?: "Medication")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (!isItemDue(item)) {
                            Toast.makeText(this, "Not yet time!", Toast.LENGTH_SHORT).show()
                            return@setItems
                        }
                        alarm.id?.let { id ->
                            dbHelper.markAlarmAsTaken(id)
                            val alarmHelper = AlarmHelper(this)
                            alarmHelper.cancelAlarm(id)
                            Toast.makeText(this, "Marked as taken", Toast.LENGTH_SHORT).show()
                            loadDashboardData()
                        }
                    }
                    1 -> {
                        if (!isItemDue(item)) {
                            Toast.makeText(this, "Not yet time!", Toast.LENGTH_SHORT).show()
                            return@setItems
                        }
                        alarm.id?.let { performSnooze(it) }
                    }
                    2 -> { }
                }
            }
            .show()
    }

    private fun showReminderActions(reminder: Reminder, item: ScheduleItem) {
        val options = arrayOf("Mark as Done", "Snooze 15 min", "Cancel")

        AlertDialog.Builder(this)
            .setTitle(reminder.title ?: "Reminder")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (!isItemDue(item)) {
                            Toast.makeText(this, "Not yet time!", Toast.LENGTH_SHORT).show()
                            return@setItems
                        }
                        reminder.id?.let { id ->
                            dbHelper.updateReminderStatus(id, "Done")
                            Toast.makeText(this, "Marked as done", Toast.LENGTH_SHORT).show()
                            loadDashboardData()
                        }
                    }
                    1 -> {
                        if (!isItemDue(item)) {
                            Toast.makeText(this, "Not yet time!", Toast.LENGTH_SHORT).show()
                            return@setItems
                        }
                        reminder.id?.let { id ->
                            val snoozeIntent = Intent(this, ReminderReceiver::class.java).apply {
                                action = ReminderReceiver.ACTION_SNOOZE
                                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
                                putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
                            }
                            sendBroadcast(snoozeIntent)

                            Toast.makeText(
                                this,
                                getString(
                                    R.string.snoozed_for_minutes,
                                    SettingsActivity.getReminderSnoozeMinutes(this)
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    2 -> { }
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
            gravity = Gravity.CENTER_VERTICAL
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
                    item.type == "reminder" -> R.drawable.ic_pill
                    (item.category ?: "").lowercase() in listOf("capsule", "pill") -> R.drawable.capsule
                    (item.category ?: "").lowercase() == "injection" -> R.drawable.health_cross
                    (item.category ?: "").lowercase() == "syrup" -> R.drawable.health_cross
                    else -> R.drawable.capsule
                }
            )
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val displayName = if (item.status.equals("SNOOZED", true) || item.status.equals("Snoozed", true)) {
            "⏰ ${item.name}"
        } else {
            item.name
        }

        val nameView = TextView(this).apply {
            text = displayName
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.text_dark))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }

        val displayDosage = if (item.status.equals("SNOOZED", true) || item.status.equals("Snoozed", true)) {
            "${item.dosage} • Snoozed".trimStart('•', ' ')
        } else {
            item.dosage
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
                if (item.status.equals("SNOOZED", true) || item.status.equals("Snoozed", true)) {
                    ContextCompat.getColor(this@DashboardActivity, R.color.primary)
                } else {
                    ContextCompat.getColor(this@DashboardActivity, R.color.hint_gray)
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