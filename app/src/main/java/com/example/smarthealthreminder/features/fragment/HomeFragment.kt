package com.example.smarthealthreminder.features.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smarthealthreminder.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views and set click listeners
        val btnMarkDone = view.findViewById<View>(R.id.btn_mark_done)
        val btnEdit = view.findViewById<View>(R.id.btn_edit)
        val btnDelete = view.findViewById<View>(R.id.btn_delete)

        btnMarkDone?.setOnClickListener {
            // Handle mark as done
        }

        btnEdit?.setOnClickListener {
            // Navigate to edit screen
        }

        btnDelete?.setOnClickListener {
            // Show delete confirmation
        }
    }
}