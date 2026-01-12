package com.example.course.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val startNode: Int,
    val endNode: Int, // Inclusive
    val weeks: String, // Raw string e.g. "1-16周"
    val location: String,
    val teacher: String,
    val className: String = "" // e.g. "Teaching Class Name"
)
