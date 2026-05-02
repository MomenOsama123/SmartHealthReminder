package com.example.smarthealthreminder.features.Profileinfo.reports

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


class ProfileActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?)
    {

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ربط الكلاس بملف الـ XML بتاع الواجهة
        setContentView(R.layout.activity_profile_info)

        // 1. تعريف العناصر (Views) اللي هنتعامل معاها من الـ XML
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnSave = findViewById<Button>(R.id.save_btn)
        val genderInput = findViewById<EditText>(R.id.gender_input)
        val bloodTypeInput = findViewById<EditText>(R.id.blood_type_input)
        val icDarkMode = findViewById<ImageView>(R.id.icDarkMode)

        // 2. تشغيل زر الرجوع (Top Bar)
        btnBack.setOnClickListener {
            finish() // دي بتقفل الصفحة الحالية وترجع للصفحة اللي قبلها
        }

        // 3. تشغيل أيقونة الدارك مود (مؤقتاً بتظهر رسالة)
        icDarkMode.setOnClickListener {
            Toast.makeText(this, "Dark Mode Clicked!", Toast.LENGTH_SHORT).show()
            // هنا تقدر تضيف كود تحويل التطبيق للوضع الليلي مستقبلاً
        }

        // 4. تشغيل قائمة اختيار النوع (Gender)
        genderInput.setOnClickListener {
            showGenderDialog(genderInput)
        }

        // 5. تشغيل قائمة اختيار فصيلة الدم (Blood Type)
        bloodTypeInput.setOnClickListener {
            showBloodTypeDialog(bloodTypeInput)
        }

        // 6. تشغيل زر حفظ البيانات
        btnSave.setOnClickListener {
            // هنا تقدر تجيب الداتا من الحقول وتحفظها في الداتا بيز
            Toast.makeText(this, "saved", Toast.LENGTH_LONG).show()
        }
    }

    // ================= الدوال المساعدة (Helper Functions) =================

    // دالة لإظهار نافذة اختيار النوع
    private fun showGenderDialog(editText: EditText) {
        val options = arrayOf("Female", "Male", "Non-Binary", "Other")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Gender")
        builder.setItems(options) { dialog, which ->
            // لما المستخدم يختار، بنحط الاختيار جوه الحقل
            editText.setText(options[which])
        }
        builder.show()
    }

    // دالة لإظهار نافذة اختيار فصيلة الدم
    private fun showBloodTypeDialog(editText: EditText) {
        val options = arrayOf(
            "A Positive (A+)", "A Negative (A-)",
            "B Positive (B+)", "B Negative (B-)",
            "O Positive (O+)", "O Negative (O-)",
            "AB Positive (AB+)", "AB Negative (AB-)"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Blood Type")
        builder.setItems(options) { dialog, which ->
            // لما المستخدم يختار، بنحط الاختيار جوه الحقل
            editText.setText(options[which])
        }
        builder.show()
    }
}