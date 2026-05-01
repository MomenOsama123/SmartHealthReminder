package com.example.smarthealthreminder.features.activity



import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.R

class AlarmRingingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm_ringing)

        val btnSnooze = findViewById<Button>(R.id.btn_snooze)
        val btnStop = findViewById<Button>(R.id.btn_stop_alarm)

        btnSnooze.setOnClickListener {
            // Handle snooze
            finish()
        }

        btnStop.setOnClickListener {
            // Handle stop alarm
            // If cognitive lock enabled, show math challenge first
            finish()
        }
    }
}