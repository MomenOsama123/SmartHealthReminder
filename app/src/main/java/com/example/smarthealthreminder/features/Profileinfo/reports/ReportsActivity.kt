package com.example.smarthealthreminder.features.Profileinfo.reports

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.navigation.BottomNavHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class ReportsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ربط الكلاس بملف الـ XML الخاص بصفحة التقارير
        // تأكد أن اسم ملف الـ XML الخاص بهذه الصفحة هو activity_reports.xml
        setContentView(R.layout.activity_reportsactivity)

        // 1. تعريف العناصر الموجودة في الشاشة
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnDownload = findViewById<Button>(R.id.download_btn)
        setupBottomNavigation()

        // 2. تشغيل زر الرجوع (السهم أعلى الشاشة)
        btnBack.setOnClickListener {
            // دالة finish تقوم بإغلاق الصفحة الحالية والعودة للصفحة السابقة
            finish()
        }

        // 3. تشغيل زر تحميل التقرير (Download Report)
        btnDownload.setOnClickListener {
            // مؤقتاً سنقوم بإظهار رسالة تفيد بنجاح التحميل
            Toast.makeText(this, "Downloading Report...", Toast.LENGTH_SHORT).show()
        }


    }

    private fun setupBottomNavigation() {
        BottomNavHelper.setup(this, findViewById<BottomNavigationView>(R.id.bottom_navigation))
    }
}
