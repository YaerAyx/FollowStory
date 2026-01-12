package com.example.course.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    fun getAll(): List<Course>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :day")
    fun getCoursesByDay(day: Int): List<Course>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg courses: Course)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(courses: List<Course>)

    @Delete
    fun delete(course: Course)

    @Query("DELETE FROM courses")
    fun deleteAll()
}
