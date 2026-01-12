package com.example.course.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.course.R
import com.example.course.data.AppDatabase
import com.example.course.data.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleFragment : Fragment() {

    private lateinit var gridLayout: GridLayout
    private val headers = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule_grid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gridLayout = view.findViewById(R.id.timetable_grid)
    }

    override fun onResume() {
        super.onResume()
        loadAllCourses()
    }

    private fun loadAllCourses() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java, "course-database"
            ).build()
            
            val courses = db.courseDao().getAll()
            
            withContext(Dispatchers.Main) {
                populateGrid(courses)
            }
            db.close()
        }
    }

    private fun populateGrid(courses: List<Course>) {
        gridLayout.removeAllViews()

        // 1. Check if we need to show weekends
        val hasWeekendCourses = courses.any { it.dayOfWeek == 6 || it.dayOfWeek == 7 }
        val daysToShow = if (hasWeekendCourses) 7 else 5
        
        // Adjust Grid Column Count
        val totalCols = daysToShow + 1 // +1 for time node column
        gridLayout.columnCount = totalCols
        
        // Calculate Column Width (Screen Width / Columns)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val nodeColWidth = 80 // Fixed width for first col
        val remainingWidth = screenWidth - nodeColWidth
        val cellWidth = remainingWidth / daysToShow

        // 2. Setup Headers (Row 0)
        // Add Empty Top-Left
        addHeaderView(0, 0, "", nodeColWidth)
        
        val headersToShow = headers.subList(1, daysToShow + 1)
        for (i in headersToShow.indices) {
            addHeaderView(0, i + 1, headersToShow[i], cellWidth)
        }

        // 3. Setup Node Numbers (Col 0, Rows 1-12)
        for (i in 1..12) {
             addHeaderView(i, 0, i.toString(), nodeColWidth)
        }
        
        // 4. Occupied Map
        val occupied = Array(13) { BooleanArray(totalCols) }
        
        for (course in courses) {
            if (course.dayOfWeek <= daysToShow && course.startNode in 1..12) {
                val end = minOf(course.endNode, 12)
                for (r in course.startNode..end) {
                    if (course.dayOfWeek < occupied[r].size) {
                        occupied[r][course.dayOfWeek] = true
                    }
                }
            }
        }

        // Add Courses
        for (course in courses) {
             if (course.dayOfWeek <= daysToShow && course.startNode in 1..12) {
                 addCourseCard(course, cellWidth)
             }
        }

        // Add Empty Cells for Grid Lines
        for (row in 1..12) {
            for (col in 1..daysToShow) {
                if (!occupied[row][col]) {
                    addEmptyCell(row, col, cellWidth)
                }
            }
        }
    }

    private fun addHeaderView(row: Int, col: Int, text: String, widthPx: Int) {
        val tv = TextView(context)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setPadding(4, 4, 4, 4)
        tv.textSize = 12f
        
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val theme = prefs.getString("theme", "light")
        if (theme == "dark") {
            tv.setTextColor(Color.parseColor("#CCCCCC"))
        } else {
            tv.setTextColor(Color.parseColor("#333333"))
        }
        
        val params = GridLayout.LayoutParams()
        params.rowSpec = GridLayout.spec(row)
        params.columnSpec = GridLayout.spec(col)
        params.width = widthPx
        params.height = 120 // Slightly Taller
        
        tv.layoutParams = params
        gridLayout.addView(tv)
    }

    private fun addEmptyCell(row: Int, col: Int, widthPx: Int) {
        val view = TextView(context)
        view.setBackgroundResource(R.drawable.bg_grid_border_only)
        view.isClickable = true
        view.isFocusable = true
        
        view.setOnClickListener {
            AddCourseDialog.newInstance(col, row)
                .show(childFragmentManager, AddCourseDialog.TAG)
        }

        val params = GridLayout.LayoutParams()
        params.rowSpec = GridLayout.spec(row)
        params.columnSpec = GridLayout.spec(col)
        params.width = widthPx
        params.height = 120
        params.setGravity(Gravity.FILL)
        
        view.layoutParams = params
        gridLayout.addView(view)
    }

    private fun addCourseCard(course: Course, widthPx: Int) {
        val cardView = CardView(requireContext())
        cardView.radius = 12f
        cardView.cardElevation = 2f
        cardView.useCompatPadding = true
        
        val color = getCourseColor(course.name)
        cardView.setCardBackgroundColor(color)
        
        val tv = TextView(context)
        tv.text = "${course.name}\n@${course.location}"
        tv.textSize = 10f
        
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val theme = prefs.getString("theme", "light")
        if (theme == "dark") {
            tv.setTextColor(Color.WHITE)
            tv.setShadowLayer(1.5f, -1f, 1f, Color.DKGRAY)
        } else {
            tv.setTextColor(Color.parseColor("#444444")) // Darker text for light cards
        }
        
        tv.gravity = Gravity.CENTER
        tv.setPadding(4, 4, 4, 4)
        
        cardView.addView(tv)

        val span = minOf(course.endNode, 12) - course.startNode + 1
        if (span < 1) return

        val params = GridLayout.LayoutParams()
        params.rowSpec = GridLayout.spec(course.startNode, span)
        params.columnSpec = GridLayout.spec(course.dayOfWeek)
        params.width = widthPx 
        params.height = 120 * span 
        params.setGravity(Gravity.FILL)
        
        cardView.layoutParams = params
        gridLayout.addView(cardView)
    }

    private fun getCourseColor(name: String): Int {
        val colors = listOf(
            0xFFEF9A9A.toInt(), 0xFFF48FB1.toInt(), 0xFFCE93D8.toInt(),
            0xFFB39DDB.toInt(), 0xFF9FA8DA.toInt(), 0xFF90CAF9.toInt(),
            0xFF81D4FA.toInt(), 0xFF80DEEA.toInt(), 0xFF80CBC4.toInt()
        )
        return colors[Math.abs(name.hashCode()) % colors.size]
    }
}
