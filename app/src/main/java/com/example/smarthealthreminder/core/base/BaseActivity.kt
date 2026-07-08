package com.example.smarthealthreminder.core.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.core.utils.LocaleHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }
}
