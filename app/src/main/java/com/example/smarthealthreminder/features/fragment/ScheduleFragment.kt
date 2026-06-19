package com.example.smarthealthreminder.features.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.data.local.AppDatabase
import com.example.smarthealthreminder.data.repository.HealthRepository
import com.example.smarthealthreminder.features.adapter.ScheduleAdapter
import com.example.smarthealthreminder.features.model.ScheduleItem
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.ui.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch


class ScheduleFragment : Fragment() {

    private var recyclerSchedule: RecyclerView? = null
    private var scheduleAdapter: ScheduleAdapter? = null

    private val viewModel: HealthViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = HealthRepository(db)
        HealthViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerSchedule = view.findViewById(R.id.recycler_schedule)
        recyclerSchedule?.layoutManager = LinearLayoutManager(requireContext())

        scheduleAdapter = ScheduleAdapter()
        recyclerSchedule?.adapter = scheduleAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allReminders.collect { reminders ->
                    scheduleAdapter?.submitList(
                        reminders
                            .sortedWith(compareBy({ it.date.orEmpty() }, { it.time.orEmpty() }))
                            .map {
                                ScheduleItem(
                                    title = it.title,
                                    date = it.date ?: "No date",
                                    time = it.time ?: "No time"
                                )
                            }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerSchedule = null
        scheduleAdapter = null
    }
}
