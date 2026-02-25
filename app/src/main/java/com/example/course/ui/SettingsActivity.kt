package com.example.course.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.course.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.app.AlertDialog

class SettingsActivity : AppCompatActivity() {

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            processAndSetBackground(it)
        }
    }

    private fun processAndSetBackground(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Center Crop to 9:16
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val targetAspect = 9f / 16f
                    val currentAspect = width.toFloat() / height.toFloat()

                    var cropWidth = width
                    var cropHeight = height
                    var x = 0
                    var y = 0

                    if (currentAspect > targetAspect) {
                        cropWidth = (height * targetAspect).toInt()
                        x = (width - cropWidth) / 2
                    } else {
                        cropHeight = (width / targetAspect).toInt()
                        y = (height - cropHeight) / 2
                    }

                    val croppedBitmap = android.graphics.Bitmap.createBitmap(originalBitmap, x, y, cropWidth, cropHeight)
                    
                    // Save to internal storage
                    val file = java.io.File(filesDir, "custom_bg.jpg")
                    val out = java.io.FileOutputStream(file)
                    croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                    out.close()

                    withContext(Dispatchers.Main) {
                        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("custom_bg_path", file.absolutePath).apply()
                        Toast.makeText(this@SettingsActivity, "背景图片已设置 (建议重启应用应用生效)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "设置图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

        // Functional Implementations
        val tvTermStart = findViewById<android.widget.TextView>(R.id.tv_term_start_value)
        val termStartMillis = prefs.getLong("term_start_date", 0L)
        if (termStartMillis != 0L) {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = termStartMillis
            tvTermStart.text = "开学日期: ${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH) + 1}-${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
        }

        findViewById<android.view.View>(R.id.btn_term_start).setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            if (termStartMillis != 0L) cal.timeInMillis = termStartMillis
            android.app.DatePickerDialog(this, { _, year, month, day ->
                val newCal = java.util.Calendar.getInstance()
                newCal.set(year, month, day, 0, 0, 0)
                prefs.edit().putLong("term_start_date", newCal.timeInMillis).apply()
                tvTermStart.text = "开学日期: $year-${month + 1}-$day"
                Toast.makeText(this, "开学日期已更新", Toast.LENGTH_SHORT).show()
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        val tvReminder = findViewById<android.widget.TextView>(R.id.tv_reminder_value)
        val reminderHour = prefs.getInt("reminder_hour", 7)
        val reminderMin = prefs.getInt("reminder_minute", 0)
        tvReminder.text = "提醒时间: ${String.format("%02d:%02d", reminderHour, reminderMin)}"

        findViewById<android.view.View>(R.id.btn_reminder).setOnClickListener {
            android.app.TimePickerDialog(this, { _, hour, minute ->
                prefs.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
                tvReminder.text = "提醒时间: ${String.format("%02d:%02d", hour, minute)}"
                // trigger worker update in MainActivity or here? Let's suggest restart or handled in Workers.
                Toast.makeText(this, "提醒时间已更新", Toast.LENGTH_SHORT).show()
            }, reminderHour, reminderMin, true).show()
        }

        findViewById<android.view.View>(R.id.btn_clear_data).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("警告")
                .setMessage("确定要清除所有课程数据吗？此操作不可撤销。")
                .setPositiveButton("清除") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            val db = androidx.room.Room.databaseBuilder(
                                applicationContext,
                                com.example.course.data.AppDatabase::class.java, "course-database"
                            ).build()
                            db.courseDao().deleteAll()
                            db.close()
                        }
                        Toast.makeText(this@SettingsActivity, "数据已清除", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        findViewById<android.view.View>(R.id.btn_custom_bg).setOnClickListener {
            // Pick image
            pickImageLauncher.launch("image/*")
        }

        val tvBlurLabel = findViewById<android.widget.TextView>(R.id.tv_blur_label)
        val sbBlur = findViewById<android.widget.SeekBar>(R.id.sb_blur)
        val currentBlur = prefs.getInt("bg_blur", 0)
        sbBlur.progress = currentBlur
        tvBlurLabel.text = "背景模糊度: $currentBlur"

        sbBlur.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvBlurLabel.text = "背景模糊度: $progress"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                prefs.edit().putInt("bg_blur", seekBar?.progress ?: 0).apply()
            }
        })

        findViewById<android.view.View>(R.id.btn_reset_bg).setOnClickListener {
            prefs.edit().remove("custom_bg_path").remove("bg_blur").apply()
            sbBlur.progress = 0
            tvBlurLabel.text = "背景模糊度: 0"
            Toast.makeText(this, "背景已重置为默认", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<android.view.View>(R.id.btn_course_color).setOnClickListener {
             val options = arrayOf("默认多彩", "清新糖果", "深邃星空", "随机色")
             val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
             val current = prefs.getInt("color_scheme", 0)
             
             AlertDialog.Builder(this)
                 .setTitle("选择课程配色方案")
                 .setSingleChoiceItems(options, current) { dialog, which ->
                     prefs.edit().putInt("color_scheme", which).apply()
                     Toast.makeText(this, "配色方案已更新: ${options[which]}", Toast.LENGTH_SHORT).show()
                     dialog.dismiss()
                 }
                 .show()
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

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Go back
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
