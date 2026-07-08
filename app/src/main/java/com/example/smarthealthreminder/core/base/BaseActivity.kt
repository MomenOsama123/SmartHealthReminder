package com.example.smarthealthreminder.core.base

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.core.utils.LocaleHelper
import com.google.android.material.snackbar.Snackbar

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    fun showSnakeBar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        findViewById<View>(android.R.id.content)?.let {
            Snackbar.make(it, message, duration).show()
        }
    }
}
