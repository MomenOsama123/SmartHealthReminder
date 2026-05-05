package com.example.smarthealthreminder.features.Profileinfo.reports

// ⚠️ لا تنسَ تغيير هذا السطر لاسم مشروعك الفعلي

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.smarthealthreminder.R

class ReportsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ربط الكلاس بملف الـ XML الخاص بصفحة التقارير
        // تأكد أن اسم ملف الـ XML الخاص بهذه الصفحة هو activity_reports.xml
        setContentView(R.layout.activity_reportsactivity)

        // 1. تعريف العناصر الموجودة في الشاشة
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnDownload = findViewById<Button>(R.id.download_btn)

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
}