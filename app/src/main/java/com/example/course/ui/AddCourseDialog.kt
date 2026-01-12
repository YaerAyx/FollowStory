package com.example.course.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.course.R
import com.example.course.data.AppDatabase
import com.example.course.data.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCourseDialog : DialogFragment() {

    private var defaultDay: Int = 1
    private var defaultStart: Int = 1
    
    companion object {
        const val TAG = "AddCourseDialog"
        private const val ARG_DAY = "day"
        private const val ARG_START = "start"

        fun newInstance(day: Int, start: Int): AddCourseDialog {
            val args = Bundle()
            args.putInt(ARG_DAY, day)
            args.putInt(ARG_START, start)
            val fragment = AddCourseDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defaultDay = arguments?.getInt(ARG_DAY) ?: 1
        defaultStart = arguments?.getInt(ARG_START) ?: 1
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            window.attributes = params
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_course, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.et_name)
        val etLocation = view.findViewById<EditText>(R.id.et_location)
        val etTeacher = view.findViewById<EditText>(R.id.et_teacher)
        val etWeeks = view.findViewById<EditText>(R.id.et_weeks)
        val etWeekDay = view.findViewById<EditText>(R.id.et_weekday)
        val etStart = view.findViewById<EditText>(R.id.et_start)
        val etEnd = view.findViewById<EditText>(R.id.et_end)
        val btnSave = view.findViewById<Button>(R.id.btn_save)

        // Pre-fill
        etWeekDay.setText(defaultDay.toString())
        etStart.setText(defaultStart.toString())
        etEnd.setText((defaultStart + 1).toString()) // Default 2 periods

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val location = etLocation.text.toString()
            val teacher = etTeacher.text.toString()
            val weeks = etWeeks.text.toString()
            val day = etWeekDay.text.toString().toIntOrNull() ?: 1
            val start = etStart.text.toString().toIntOrNull() ?: 1
            val end = etEnd.text.toString().toIntOrNull() ?: start

            if (name.isBlank()) {
                Toast.makeText(context, "课程名不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val db = Room.databaseBuilder(
                    requireContext(), 
                    AppDatabase::class.java, "course-database"
                ).build()
                
                val course = Course(
                    name = name,
                    dayOfWeek = day,
                    startNode = start,
                    endNode = end,
                    weeks = weeks,
                    location = location,
                    teacher = teacher,
                    className = "" 
                )
                
                db.courseDao().insertAll(course)
                db.close()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                    dismiss()
                    (parentFragment as? ScheduleFragment)?.onResume() // Trigger reload
                }
            }
        }
    }
}
