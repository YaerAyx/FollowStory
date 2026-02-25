package com.example.course.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.course.R
import com.example.course.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class TodayFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private val adapter = CourseAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_today, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view_today)
        emptyView = view.findViewById(R.id.tv_empty)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // Refresh adapter to apply potential theme changes (text color)
        adapter.notifyDataSetChanged()
        loadTodayCourses() // Also reload data just in case
    }



    private fun loadTodayCourses() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java, "course-database"
            ).build()

            val calendar = Calendar.getInstance()
            val javaDay = calendar.get(Calendar.DAY_OF_WEEK)
            val dbDay = if (javaDay == Calendar.SUNDAY) 7 else javaDay - 1
            
            val appPrefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val startMillis = appPrefs.getLong("term_start_date", 0L)
            val currentWeek = com.example.course.utils.WeekUtils.calculateCurrentWeek(startMillis)
            
            val allCoursesForDay = db.courseDao().getCoursesByDay(dbDay)
            val courses = allCoursesForDay.filter { 
                com.example.course.utils.WeekUtils.isWeekInPattern(it.weeks, currentWeek)
            }.sortedBy { it.startNode }
            
            withContext(Dispatchers.Main) {
                if (courses.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.submitList(courses)
                }
            }
            db.close()
        }
    }
}
