package com.example.smarthealthreminder.features.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.smarthealthreminder.features.stepsTracker.StepsTrackerFragment
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.MainActivity
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.alarm.ReminderReceiver
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.util.ImageUtils
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private val viewModel: HealthViewModel by activityViewModel()

    private var firebaseId: String = ""
    private lateinit var pbHomeAdherence: ProgressBar
    private lateinit var pbStepsProgress: ProgressBar
    private lateinit var tvGreeting: TextView
    private lateinit var tvAdherencePercent: TextView
    private lateinit var tvTotalMeds: TextView
    private lateinit var cardSteps: CardView
    private lateinit var tvTakenToday: TextView
    private lateinit var tvMissedToday: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvNextMedName: TextView
    private lateinit var tvNextMedTime: TextView
    private lateinit var btnMarkTaken: TextView
    private lateinit var btnSnooze: TextView
    private lateinit var ivProfile: ImageView

    private lateinit var layoutUpcomingMeds: View
    private lateinit var containerUpcomingItems: LinearLayout
    private lateinit var tvUpcomingTitle: TextView
    private lateinit var tvNoUpcoming: TextView
    private lateinit var fabAddMed: View
    private lateinit var tvViewDetails: TextView
    private lateinit var tvCurrentSteps: TextView
    private lateinit var tvTargetSteps: TextView

    private var reminderEntities: List<ReminderEntity> = emptyList()
    private var currentNextItem: ScheduleItem? = null

    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadNextDose()
            loadUpcomingList()
            view?.postDelayed(this, 30000)
        }
    }

    private companion object {
        private const val PREFS_NAME = "DashboardPrefs"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_LAST_RESET_WEEK = "last_reset_week"
        private const val KEY_LAST_RESET_MONTH = "last_reset_month"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val sharedPref = requireActivity().getSharedPreferences("HealthSyncPrefs", Context.MODE_PRIVATE)
        val auth = FirebaseAuth.getInstance()

        firebaseId = auth.currentUser?.uid ?: sharedPref.getString("FIREBASE_ID", "") ?: ""

        if (firebaseId.isEmpty()) {
            // Handled by Activity usually, but for fragment safety:
            return
        }

        initViews(view)
        setupClickListeners()
        setupProfileObservation()
        observeSteps()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
    }private fun observeSteps() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todaySteps.collect { stepData ->
                if (stepData != null) {
                    val steps = stepData.steps
                    val target = stepData.targetSteps

                    tvCurrentSteps.text = steps.toString()
                    tvTargetSteps.text = "/$target"

                    // خلي الـ max بتاع الـ ProgressBar هو الـ target نفسه
                    // مش نسبة مئوية ثابتة (100)
                    pbStepsProgress.max = target
                    pbStepsProgress.progress = steps.coerceAtMost(target)

                    android.util.Log.d("Steps", "steps=$steps target=$target")
                } else {
                    tvCurrentSteps.text = "0"
                    tvTargetSteps.text = "/10000"
                    pbStepsProgress.max = 10000
                    pbStepsProgress.progress = 0
                }
            }
        }
    }
    private fun initViews(view: View) {
        cardSteps = view.findViewById(R.id.cardSteps)
        pbHomeAdherence = view.findViewById(R.id.pbHomeAdherence)
        pbStepsProgress = view.findViewById(R.id.pbStepsProgress)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvUserName = view.findViewById(R.id.tvUserName)
        ivProfile = view.findViewById(R.id.ivProfile)
        tvCurrentSteps = view.findViewById(R.id.tvCurrentSteps)
        tvTargetSteps = view.findViewById(R.id.tvTargetSteps)
        tvAdherencePercent = view.findViewById(R.id.tvAdherencePercent)
        tvTotalMeds = view.findViewById(R.id.tvTotalMeds)
        tvTakenToday = view.findViewById(R.id.tvTakenToday)
        tvMissedToday = view.findViewById(R.id.tvMissedToday)

        tvNextMedName = view.findViewById(R.id.tvNextMedName)
        tvNextMedTime = view.findViewById(R.id.tvNextMedTime)
        btnMarkTaken = view.findViewById(R.id.btnMarkTaken)
        btnSnooze = view.findViewById(R.id.btnSnooze)

        layoutUpcomingMeds = view.findViewById(R.id.layoutUpcomingMeds)
        containerUpcomingItems = view.findViewById(R.id.containerUpcomingItems)
        tvUpcomingTitle = view.findViewById(R.id.tvUpcomingTitle)
        tvNoUpcoming = view.findViewById(R.id.tvNoUpcoming)
        fabAddMed = view.findViewById(R.id.fabAddMed)
        tvViewDetails = view.findViewById(R.id.tvViewDetails)
    }

    private fun setupProfileObservation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentUser.collect { user ->
                    user?.let {
                        tvUserName.text = it.name
                        it.profileImage?.let { base64 ->
                            val bitmap = ImageUtils.base64ToBitmap(base64)
                            ivProfile.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }
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
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
            startActivity(Intent(requireContext(), AddReminderActivity::class.java))
        }
        tvViewDetails.setOnClickListener {
            (activity as? MainActivity)?.navigateToDestination(MainActivity.DESTINATION_INSIGHTS)
        }
        btnMarkTaken.setOnClickListener {
            currentNextItem?.let { item ->
                if (isItemDue(item)) {
                    val reminder = reminderEntities.find { it.id == item.id }
                    reminder?.let {
                        viewModel.markReminderDone(it.id)
                        Toast.makeText(requireContext(), getString(R.string.marked_done), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.not_yet_time, item.time), Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), getString(R.string.no_medication), Toast.LENGTH_SHORT).show()
            }
        }

        btnSnooze.setOnClickListener {
            currentNextItem?.let { item ->
                if (isItemDue(item)) {
                    val reminder = reminderEntities.find { it.id == item.id }
                    reminder?.let {
                        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                            action = ReminderReceiver.ACTION_SNOOZE
                            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                            putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
                            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
                            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, reminder.description.orEmpty())
                        }
                        requireActivity().sendBroadcast(intent)
                        Toast.makeText(requireContext(), getString(R.string.snoozed), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.not_yet_time, item.time), Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), getString(R.string.no_medication_to_snooze), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserGreeting() {
        val sharedPref = requireActivity().getSharedPreferences("HealthSyncPrefs", Context.MODE_PRIVATE)
        val userName = sharedPref.getString("USER_NAME", "User") ?: "User"

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            in 17..20 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }

        tvGreeting.text = getString(greetingRes)
        tvUserName.text = userName
    }

    private fun shouldShowToday(reminder: ReminderEntity): Boolean {
        return RecurrenceHelper.isDueOnDate(
            reminderDate = reminder.date,
            recurrenceType = reminder.recurrenceType,
            isRecurring = reminder.isRecurring,
            targetDate = RecurrenceHelper.getTodayString()
        )
    }

    private fun loadStats() {
        val todayReminders = reminderEntities.filter { shouldShowToday(it) }

        val total = todayReminders.size
        val done = todayReminders.count { it.status == "Completed" }
        val missed = todayReminders.count { it.status == "Missed" }

        tvTotalMeds.text = total.toString()
        tvTakenToday.text = done.toString()
        tvMissedToday.text = missed.toString()

        val percentage = if (total > 0) (done * 100 / total) else 0
        tvAdherencePercent.text = getString(R.string.percentage_format, percentage)
        pbHomeAdherence.progress = percentage

        requireContext().getSharedPreferences("health_prefs", Context.MODE_PRIVATE).edit {
            putInt("adherence_percent", percentage)
        }
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
            allItems.any { it.status == "Snoozed" && currentMinutes >= it.minutes } -> {
                allItems.filter { it.status == "Snoozed" && currentMinutes >= it.minutes }.minByOrNull { it.minutes }
            }
            allItems.any { it.status == "Pending" && currentMinutes >= it.minutes } -> {
                allItems.filter { it.status == "Pending" && currentMinutes >= it.minutes }.minByOrNull { it.minutes }
            }
            else -> {
                allItems.filter { it.status == "Pending" && it.minutes > currentMinutes }.minByOrNull { it.minutes }
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

            val reminder = reminderEntities.find { it.id == nextItem.id }
            val snoozeUsed = reminder?.snoozeUsed == true
            val isDue = isItemDue(nextItem)

            btnMarkTaken.visibility = View.VISIBLE
            btnMarkTaken.isEnabled = isDue
            btnMarkTaken.alpha = if (isDue) 1f else 0.5f

            btnSnooze.visibility = if (snoozeUsed) View.GONE else View.VISIBLE
            btnSnooze.isEnabled = isDue && !snoozeUsed
            btnSnooze.alpha = if (isDue && !snoozeUsed) 1f else 0.5f
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
            if (!shouldShowToday(reminder)) return@forEach

            val reminderMinutes = timeToMinutes(reminder.time)
            if (reminderMinutes == 0 && reminder.time.isNullOrEmpty()) return@forEach

            val status = when (reminder.status) {
                "Completed" -> "Taken"
                "Missed" -> "Missed"
                "Snoozed" -> "Snoozed"
                else -> "Pending"
            }

            allItems.add(ScheduleItem(
                id = reminder.id,
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
                    val separator = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        ).apply { setMargins(0, 16, 0, 16) }
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.hint_gray))
                    }
                    containerUpcomingItems.addView(separator)
                }
            }
        }
    }

    private fun showReminderActions(reminder: ReminderEntity, item: ScheduleItem) {
        val snoozeDisabled = item.status.equals("Snoozed", true) || reminder.snoozeUsed
        val options = if (snoozeDisabled) {
            arrayOf(getString(R.string.mark_as_done), getString(R.string.cancel))
        } else {
            arrayOf(getString(R.string.mark_as_done), getString(R.string.snooze_10_min), getString(R.string.cancel))
        }

        AlertDialog.Builder(requireContext())
            .setTitle(reminder.title)
            .setItems(options) { _, which ->
                if (snoozeDisabled) {
                    when (which) {
                        0 -> handleMarkDone(reminder, item)
                    }
                } else {
                    when (which) {
                        0 -> handleMarkDone(reminder, item)
                        1 -> handleSnooze(reminder, item)
                    }
                }
            }
            .show()
    }

    private fun handleMarkDone(reminder: ReminderEntity, item: ScheduleItem) {
        if (!isItemDue(item)) {
            Toast.makeText(requireContext(), getString(R.string.not_yet_time_short), Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.markReminderDone(reminder.id)
        Toast.makeText(requireContext(), getString(R.string.marked_done), Toast.LENGTH_SHORT).show()
    }

    private fun handleSnooze(reminder: ReminderEntity, item: ScheduleItem) {
        if (!isItemDue(item)) {
            Toast.makeText(requireContext(), getString(R.string.not_yet_time_short), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, reminder.description.orEmpty())
        }
        requireActivity().sendBroadcast(intent)
        Toast.makeText(requireContext(), getString(R.string.snoozed), Toast.LENGTH_SHORT).show()
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
        val context = requireContext()
        val rowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val iconView = ImageView(context).apply {
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

        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val displayName = when {
            item.status.equals("Taken", true) -> getString(R.string.taken_prefix, item.name)
            item.status.equals("Snoozed", true) -> getString(R.string.snoozed_prefix, item.name)
            item.status.equals("Missed", true) -> getString(R.string.missed_prefix, item.name)
            else -> item.name
        }

        val nameView = TextView(context).apply {
            text = displayName
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }

        val displayDosage = when {
            item.status.equals("Taken", true) -> getString(R.string.taken_dosage_suffix, item.dosage)
            item.status.equals("Snoozed", true) -> getString(R.string.snoozed_dosage_suffix, item.dosage)
            item.status.equals("Missed", true) -> getString(R.string.missed_dosage_suffix, item.dosage)
            else -> item.dosage
        }

        val dosageView = TextView(context).apply {
            text = displayDosage
            setTextColor(ContextCompat.getColor(context, R.color.hint_gray))
            textSize = 12f
        }

        textLayout.addView(nameView)
        textLayout.addView(dosageView)

        val timeView = TextView(context).apply {
            text = item.time
            setTextColor(
                when {
                    item.status.equals("Snoozed", true) -> ContextCompat.getColor(context, R.color.primary)
                    item.status.equals("Missed", true) -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                    else -> ContextCompat.getColor(context, R.color.hint_gray)
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

    override fun onResume() {
        super.onResume()
        view?.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        view?.removeCallbacks(refreshRunnable)
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