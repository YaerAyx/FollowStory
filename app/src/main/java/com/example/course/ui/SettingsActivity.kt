package com.example.course.ui

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.course.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Setup Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentTheme = prefs.getString("theme", "light")

        // Initial Checks
        updateChecks(currentTheme ?: "light")

        findViewById<android.view.View>(R.id.row_light_theme).setOnClickListener {
            setTheme("light")
        }
        
        findViewById<android.view.View>(R.id.row_dark_theme).setOnClickListener {
            setTheme("dark")
        }

        // Placeholder Implementations
        findViewById<android.view.View>(R.id.btn_custom_bg).setOnClickListener {
            Toast.makeText(this, "自定义背景功能开发中...", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btn_course_color).setOnClickListener {
             Toast.makeText(this, "课程颜色配置功能开发中...", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btn_reminder).setOnClickListener {
             Toast.makeText(this, "提醒时间设置功能开发中...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setTheme(themeIndex: String) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (prefs.getString("theme", "light") != themeIndex) {
             prefs.edit().putString("theme", themeIndex).apply()
             updateChecks(themeIndex)
             Toast.makeText(this, "主界面主题已更新", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateChecks(theme: String) {
        val lightCheck = findViewById<android.view.View>(R.id.img_check_light)
        val darkCheck = findViewById<android.view.View>(R.id.img_check_dark)
        
        if (theme == "dark") {
            lightCheck.visibility = android.view.View.INVISIBLE
            darkCheck.visibility = android.view.View.VISIBLE
        } else {
            lightCheck.visibility = android.view.View.VISIBLE
            darkCheck.visibility = android.view.View.INVISIBLE
        }
    }

    // Removed updateSettingsTheme as iOS style has fixed white blocks
    
    // Interface for future settings expansion
    
    // Interface for future settings expansion
    interface SettingsHandler {
        fun onCustomBackgroundSelected(uri: String)
        fun onCourseColorChanged(courseName: String, color: Int)
        fun onReminderTimeChanged(hour: Int, minute: Int)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Go back
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
