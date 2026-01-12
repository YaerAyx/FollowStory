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
        val prefs = getPreferences(MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)
        if (isFirstRun) {
            android.widget.Toast.makeText(this, "左右滑动切换视图", android.widget.Toast.LENGTH_LONG).show()
            prefs.edit().putBoolean("is_first_run", false).apply()
        }

        val btnSettings = findViewById<android.view.View>(R.id.btn_settings)
        btnSettings.setOnClickListener {
            // SettingsDialog().show(supportFragmentManager, SettingsDialog.TAG)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                btnSettings.visibility = if (position == 0) android.view.View.VISIBLE else android.view.View.GONE
            }
        })
        
        fabAdd.setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java))
        }

        setupDailyNotification()
        
        // Initial Theme Apply
        updateTheme(prefs.getString("theme", "light") ?: "light")
        
        // Week Number Logic
        val currentWeek = calculateCurrentWeek()
        supportActionBar?.subtitle = "第${currentWeek}周"
        // Or if No ActionBar (Theme.Course might be NoActionBar), show a toast or subtitle in ViewPager? 
        // Let's add a TextView to Main Layout for Week info if needed, but Title is cleanest.
        // Assuming standard ActionBar exists, otherwise use a TextView.
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        updateTheme(prefs.getString("theme", "light") ?: "light")
        
        // Also update sub-fragments if needed? 
        // Fragments adhere to theme logic in their own onResume/views usually.
    }

    private fun calculateCurrentWeek(): Int {
        val prefs = getPreferences(MODE_PRIVATE)
        val startMillis = prefs.getLong("term_start_date", 0L)
        
        if (startMillis == 0L) {
             // Default to now if not set (or could prompt user), let's set it to today for now
             prefs.edit().putLong("term_start_date", System.currentTimeMillis()).apply()
             return 1
        }
        
        val diff = System.currentTimeMillis() - startMillis
        val weeks = diff / (1000 * 60 * 60 * 24 * 7)
        return weeks.toInt() + 1
    }

    fun updateTheme(themeName: String) {
        val rootLayout = findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
        if (themeName == "dark") {
            rootLayout.setBackgroundResource(R.drawable.bg_deep_space)
        } else {
            rootLayout.setBackgroundResource(R.drawable.bg_light_pastel)
        }
    }

    private fun setupDailyNotification() {
        // Schedule notification for 7:00 AM every day
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        dueDate.set(Calendar.HOUR_OF_DAY, 7)
        dueDate.set(Calendar.MINUTE, 0)
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
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}
