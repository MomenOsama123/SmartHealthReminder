package com.example.smarthealthreminder.features.stepsTracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.databinding.FragmentStepsTrackerBinding
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.example.smarthealthreminder.features.util.ImageUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class StepsTrackerFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentStepsTrackerBinding? = null
    private val binding get() = _binding!!

    private var sensorManager: SensorManager? = null
    private var stepDetectorSensor: Sensor? = null
    private var isTracking = false
    
    // Local session counter to ensure no steps are missed during the session
    private var sessionSteps = 0
    private var currentStepsDisplay = 0
    private var targetStepsDisplay = 10000

    private val viewModel: HealthViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupStepSensor()
        } else {
            Toast.makeText(context, "Permission denied. Step counting won't work.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepsTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle window insets for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setupClickListeners()
        observeData()
        checkPermissionsAndSetup()
    }

    private fun checkPermissionsAndSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                setupStepSensor()
            }
        } else {
            setupStepSensor()
        }
    }

    private fun setupStepSensor() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // TYPE_STEP_DETECTOR is much more responsive than TYPE_STEP_COUNTER for live sessions
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        
        if (stepDetectorSensor == null) {
            // Fallback to TYPE_STEP_COUNTER if detector is not available
            stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepDetectorSensor == null) {
                Toast.makeText(context, "No step sensor found on this device", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isTracking) {
            registerStepListener()
        }
    }

    private fun registerStepListener() {
        stepDetectorSensor?.let {
            // Using SENSOR_DELAY_FASTEST for real-time responsiveness
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onPause() {
        super.onPause()
        // We keep tracking if it's a "Session", but unregister to save battery if user leaves screen
        // Depending on requirements, you might want to use a Foreground Service to track steps when app is closed.
        if (isTracking) {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking || event == null) return

        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            // Step detector triggers for every single step (value is 1.0)
            if (event.values[0] == 1.0f) {
                updateStepCount(1)
                // updateLiveUI() is no longer called here to avoid double increments
            }
        }
    }
    
    private fun updateLiveUI(totalSteps: Int) {
        currentStepsDisplay = totalSteps
        binding.tvCurrentSteps.text = String.format("%,d", currentStepsDisplay)
        binding.progressBarSteps.progress = currentStepsDisplay
        
        // Update distance and calories locally too for instant feedback
        val dist = currentStepsDisplay * 0.0008
        val kcal = (currentStepsDisplay * 0.04).toInt()
        binding.tvDistanceValue.text = String.format("%.1f", dist)
        binding.tvCaloriesValue.text = kcal.toString()

        val remaining = targetStepsDisplay - currentStepsDisplay
        if (remaining > 0) {
            val formattedRemaining = String.format("%,d", remaining)
            val minutes = (remaining / 100).coerceAtLeast(1)
            binding.tvMotivationDesc.text = getString(R.string.motivation_desc_format, formattedRemaining, minutes)
        }
    }
    
    private fun updateStepCount(delta: Int) {
        sessionSteps += delta
        val currentSteps = viewModel.todaySteps.value?.steps ?: currentStepsDisplay
        val newSteps = currentSteps + delta
        
        // Update Active minutes: 1 minute for every 100 steps
        val currentMin = viewModel.todaySteps.value?.activeMinutes ?: 0
        val newMin = currentMin + (if (sessionSteps % 100 == 0) 1 else 0)
        
        viewModel.updateSteps(newSteps, newMin)
        updateLiveUI(newSteps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun setupClickListeners() {
        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.layoutTargetSteps.setOnClickListener {
            showTargetSelectionDialog()
        }

        binding.btnStartActivity.setOnClickListener {
            if (stepDetectorSensor == null) {
                Toast.makeText(context, "Sensor not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isTracking) {
                isTracking = true
                sessionSteps = 0
                registerStepListener()
                binding.btnStartActivity.text = "STOP ACTIVITY"
                binding.btnStartActivity.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.urgent))
                Toast.makeText(context, "Live tracking active! Walk to see steps.", Toast.LENGTH_SHORT).show()
            } else {
                isTracking = false
                sensorManager?.unregisterListener(this)
                binding.btnStartActivity.text = "START ACTIVITY"
                binding.btnStartActivity.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_stat_accent))
                Toast.makeText(context, "Activity tracking stopped", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnResetProgress.setOnClickListener {
            viewModel.resetSteps()
            sessionSteps = 0
        }
        
        // Removing the mock button "START EVENING" to keep it strictly sensor-based
        binding.btnStartEvening.visibility = View.GONE
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.todaySteps, viewModel.currentUser) { steps, user ->
                Pair(steps, user)
            }.collectLatest { (stepData, user) ->
                user?.let {
                    it.profileImage?.let { base64 ->
                        val bitmap = ImageUtils.base64ToBitmap(base64)
                        binding.ivProfile.setImageBitmap(bitmap)
                    } ?: run {
                        binding.ivProfile.setImageResource(R.drawable.profile)
                    }
                }

                if (stepData != null) {
                    targetStepsDisplay = stepData.targetSteps
                    
                    // Only update the main counter from DB if we are NOT currently tracking live
                    // This prevents the "jumping" effect caused by DB latency
                    if (!isTracking) {
                        currentStepsDisplay = stepData.steps
                        binding.tvCurrentSteps.text = String.format("%,d", stepData.steps)
                        binding.progressBarSteps.progress = stepData.steps
                        binding.tvDistanceValue.text = String.format("%.1f", stepData.distanceKm)
                        binding.tvCaloriesValue.text = stepData.calories.toString()
                    }
                    
                    binding.tvTargetSteps.text = "/ ${String.format("%,d", stepData.targetSteps)} steps"
                    binding.progressBarSteps.max = stepData.targetSteps
                    binding.tvActiveMinValue.text = stepData.activeMinutes.toString()
                    
                    val remaining = stepData.targetSteps - currentStepsDisplay
                    val userName = user?.name ?: "there"
                    
                    if (remaining > 0) {
                        binding.tvMotivationTitle.text = getString(R.string.quot_keep_it_up_quot)
                        val formattedRemaining = String.format("%,d", remaining)
                        val minutes = (remaining / 100).coerceAtLeast(1)
                        binding.tvMotivationDesc.text = getString(R.string.motivation_desc_format, formattedRemaining, minutes)
                    } else {
                        binding.tvMotivationTitle.text = "\"Goal Achieved!\""
                        binding.tvMotivationDesc.text = "Excellent work $userName! You've reached your daily step goal. Keep maintaining this healthy habit."
                    }
                } else {
                    // Default state when no data exists for today
                    if (!isTracking) {
                        currentStepsDisplay = 0
                        binding.tvCurrentSteps.text = "0"
                        binding.progressBarSteps.progress = 0
                        binding.tvDistanceValue.text = "0.0"
                        binding.tvCaloriesValue.text = "0"
                    }
                    binding.tvTargetSteps.text = "/ 10,000 steps"
                    binding.progressBarSteps.max = 10000
                    binding.tvActiveMinValue.text = "0"
                    
                    binding.tvMotivationTitle.text = "\"Get Started!\""
                    binding.tvMotivationDesc.text = "Start your day with a walk. Reach 10,000 steps to maintain your health goal."
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lastSevenDaysSteps.collectLatest { stepsList ->
                updateWeeklyChart(stepsList)
            }
        }
    }

    private fun updateWeeklyChart(stepsList: List<com.example.smarthealthreminder.features.data.local.entity.StepEntity>) {
        val days = listOf(
            binding.root.findViewById<View>(R.id.barMon),
            binding.root.findViewById<View>(R.id.barTue),
            binding.root.findViewById<View>(R.id.barWed),
            binding.root.findViewById<View>(R.id.barThu),
            binding.root.findViewById<View>(R.id.barFri),
            binding.root.findViewById<View>(R.id.barSat),
            binding.root.findViewById<View>(R.id.barSun)
        )
        
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        // Clear/Set default bars first
        days.forEachIndexed { index, view ->
            setupBar(view, dayLabels[index], 10, false)
        }

        stepsList.take(7).reversed().forEachIndexed { index, stepEntity ->
            if (index < days.size) {
                val height = (stepEntity.steps.toFloat() / stepEntity.targetSteps.toFloat() * 100).toInt().coerceIn(10, 120)
                setupBar(days[index], dayLabels[index], height, index == stepsList.size - 1)
            }
        }
        
        if (stepsList.isNotEmpty()) {
            val avg = stepsList.map { it.steps }.average().toInt()
            binding.tvWeeklyAverage.text = "Average: ${String.format("%,d", avg)} steps"
        } else {
            binding.tvWeeklyAverage.text = "No weekly data yet"
        }
    }

    private fun setupBar(view: View, day: String, heightDp: Int, isHighlight: Boolean = false) {
        val barProgress = view.findViewById<View>(R.id.barProgress)
        val tvDay = view.findViewById<android.widget.TextView>(R.id.tvDay)
        
        tvDay.text = day
        
        val params = barProgress.layoutParams
        params.height = (heightDp * resources.displayMetrics.density).toInt()
        barProgress.layoutParams = params
        
        if (isHighlight) {
            barProgress.backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.card_stat_accent, null)
            )
            barProgress.alpha = 1.0f
        } else {
            barProgress.backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.primary, null)
            )
            barProgress.alpha = 0.4f
        }
    }

    private fun showTargetSelectionDialog() {
        val targets = arrayOf("5,000", "8,000", "10,000", "12,000", "15,000", "20,000")
        val values = intArrayOf(5000, 8000, 10000, 12000, 15000, 20000)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Daily Step Goal")
            .setItems(targets) { _, which ->
                viewModel.updateDailyGoal(values[which])
                Toast.makeText(context, "Goal updated to ${targets[which]} steps", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = StepsTrackerFragment()
    }
}