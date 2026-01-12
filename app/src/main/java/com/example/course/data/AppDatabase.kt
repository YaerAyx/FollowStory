package com.example.course.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Course::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
}
