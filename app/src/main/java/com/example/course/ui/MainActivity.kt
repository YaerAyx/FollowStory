package com.example.course.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.course.R
import com.example.course.worker.DailyCourseWorker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.TimeUnit
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int) = when (position) {
                0 -> TodayFragment()
                else -> ScheduleFragment()
            }
        }

        // Minimalist UI: Removed TabLayout
        // Hint user on first run
        val appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstRun = appPrefs.getBoolean("is_first_run", true)
        if (isFirstRun) {
            android.widget.Toast.makeText(this, "左右滑动切换视图", android.widget.Toast.LENGTH_LONG).show()
            appPrefs.edit().putBoolean("is_first_run", false).apply()
        }

        val btnSettings = findViewById<android.view.View>(R.id.btn_settings)
        btnSettings.setOnClickListener {
            // SettingsDialog().show(supportFragmentManager, SettingsDialog.TAG)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Keep settings button visible on both pages for easier access
                btnSettings.visibility = android.view.View.VISIBLE
            }
        })
        
        fabAdd.setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java))
        }
        
        setupDailyNotification()
        
        // Initial Theme Apply
        updateTheme(appPrefs.getString("theme", "light") ?: "light")
        
        // Week Number Logic
        updateWeekSubtitle()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        updateTheme(prefs.getString("theme", "light") ?: "light")
        setupDailyNotification() // Re-sync in case time changed
        updateWeekSubtitle()
    }

    private fun updateWeekSubtitle() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val startMillis = prefs.getLong("term_start_date", 0L)
        val currentWeek = com.example.course.utils.WeekUtils.calculateCurrentWeek(startMillis)
        supportActionBar?.subtitle = if (currentWeek > 0) "第${currentWeek}周" else "未开学"
    }

    fun updateTheme(themeName: String) {
        val rootLayout = findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
        val bgImage = findViewById<android.widget.ImageView>(R.id.iv_background)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val customBgPath = prefs.getString("custom_bg_path", null)

        if (customBgPath != null) {
            try {
                val file = java.io.File(customBgPath)
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val blurRadius = prefs.getInt("bg_blur", 0)
                        rootLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && blurRadius > 0) {
                            bgImage.setImageBitmap(bitmap)
                            bgImage.setRenderEffect(
                                android.graphics.RenderEffect.createBlurEffect(
                                    blurRadius.toFloat() * 2,
                                    blurRadius.toFloat() * 2,
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                            )
                        } else if (blurRadius > 0) {
                            val blurredBitmap = blurBitmap(bitmap, blurRadius.toFloat())
                            bgImage.setImageBitmap(blurredBitmap)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                bgImage.setRenderEffect(null)
                            }
                        } else {
                            bgImage.setImageBitmap(bitmap)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                bgImage.setRenderEffect(null)
                            }
                        }
                        return
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Default backgrounds
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            bgImage.setRenderEffect(null)
        }
        bgImage.setImageDrawable(null)

        if (themeName == "dark") {
            rootLayout.setBackgroundResource(R.drawable.bg_deep_space)
        } else {
            rootLayout.setBackgroundResource(R.drawable.bg_light_pastel)
        }
    }

    private fun setupDailyNotification() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hour = prefs.getInt("reminder_hour", 7)
        val min = prefs.getInt("reminder_minute", 0)

        // Schedule notification for customized time every day
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        dueDate.set(Calendar.HOUR_OF_DAY, hour)
        dueDate.set(Calendar.MINUTE, min)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyCourseWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyCourseNotification",
            ExistingPeriodicWorkPolicy.UPDATE, // Allow update on change
            dailyWorkRequest
        )
    }

    private fun blurBitmap(bitmap: android.graphics.Bitmap, radius: Float): android.graphics.Bitmap {
        if (radius <= 0) return bitmap
        
        // Fast blur via scaling down then back up
        val scaleFactor = 1f / (radius / 2).coerceAtLeast(1f)
        val width = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
        
        val smallBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
        return android.graphics.Bitmap.createScaledBitmap(smallBitmap, bitmap.width, bitmap.height, true)
    }
}
