package com.example.course.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.course.R
import com.example.course.data.Course

class CourseAdapter(private var courses: List<Course> = emptyList()) :
    RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    fun submitList(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.bind(course)
    }

    override fun getItemCount(): Int = courses.size

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTv: TextView = itemView.findViewById(R.id.tv_course_name)
        private val timeTv: TextView = itemView.findViewById(R.id.tv_course_time)
        private val locationTv: TextView = itemView.findViewById(R.id.tv_course_location)
        private val teacherTv: TextView = itemView.findViewById(R.id.tv_course_teacher)

        fun bind(course: Course) {
            nameTv.text = course.name
            timeTv.text = "星期${course.dayOfWeek} 第${course.startNode}-${course.endNode}节"
            locationTv.text = "${course.location} (${course.weeks})"
            teacherTv.text = "${course.teacher} ${course.className}"
            
            // Dynamic Text Color for Today View
            val context = itemView.context
            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val theme = prefs.getString("theme", "light")
            
            if (theme == "dark") {
                val white = android.graphics.Color.WHITE
                nameTv.setTextColor(white)
                timeTv.setTextColor(white)
                locationTv.setTextColor(white)
                teacherTv.setTextColor(android.graphics.Color.LTGRAY)
            } else {
                val darkUserRequest = android.graphics.Color.BLACK // Force Black for max contrast
                nameTv.setTextColor(darkUserRequest)
                timeTv.setTextColor(darkUserRequest)
                locationTv.setTextColor(darkUserRequest)
                teacherTv.setTextColor(android.graphics.Color.DKGRAY)
            }
        }
    }
}
