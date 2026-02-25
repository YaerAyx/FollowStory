package com.example.course.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.course.R
import com.example.course.data.AppDatabase
import java.util.Calendar

class DailyCourseWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "course-database"
        ).build()

        val calendar = Calendar.getInstance()
        // Calendar.DAY_OF_WEEK: Sun=1, Mon=2 ... Sat=7.
        // Our DB: Mon=1 ... Sun=7.
        // Conversion:
        val javaDay = calendar.get(Calendar.DAY_OF_WEEK)
        val dbDay = if (javaDay == Calendar.SUNDAY) 7 else javaDay - 1

        val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val startMillis = prefs.getLong("term_start_date", 0L)
        val currentWeek = com.example.course.utils.WeekUtils.calculateCurrentWeek(startMillis)

        val allCourses = database.courseDao().getCoursesByDay(dbDay)
        val todayCourses = allCourses.filter { 
            com.example.course.utils.WeekUtils.isWeekInPattern(it.weeks, currentWeek)
        }.sortedBy { it.startNode }
        
        if (todayCourses.isNotEmpty()) {
            sendNotification("今日课程提醒", "今天有 ${todayCourses.size} 节课，第一节是 ${todayCourses[0].name}")
        }

        database.close()
        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "course_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "课程提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon if available
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
