package com.example.course.utils

import com.example.course.data.Course
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.regex.Pattern

object CourseParser {

    fun parseHtml(html: String): List<Course> {
        val courses = mutableListOf<Course>()
        val doc = Jsoup.parse(html)
        
        // Iterate through days (1-7) and nodes (1-12 typically)
        // IDs are in format "day-node", e.g. "1-1" is Mon Node 1
        // Max nodes usually 12 or 14. Let's iterate widely to be safe.
        
        for (day in 1..7) {
            for (node in 1..15) {
                val elementId = "$day-$node"
                val td = doc.getElementById(elementId) ?: continue
                
                val contents = td.select("div.timetable_con")
                for (content in contents) {
                    val course = parseCourseFromDiv(content, day, node)
                    if (course != null) {
                        courses.add(course)
                    }
                }
            }
        }
        return courses
    }

    private fun parseCourseFromDiv(div: Element, day: Int, startNodeHint: Int): Course? {
        try {
            val titleElement = div.selectFirst("span.title font") ?: return null
            val name = titleElement.text().replace("★", "").trim()

            val ps = div.select("p")
            var weeks = ""
            var location = ""
            var teacher = ""
            var className = ""
            var startNode = startNodeHint
            var endNode = startNodeHint

            for (p in ps) {
                val text = p.text()
                if (text.contains("节/周") || text.contains("周")) { // (3-4节)3-9周,13周
                    // Extract nodes
                    val nodeMatcher = Pattern.compile("\\((\\d+)-(\\d+)节\\)").matcher(text)
                    if (nodeMatcher.find()) {
                        startNode = nodeMatcher.group(1)?.toIntOrNull() ?: startNodeHint
                        endNode = nodeMatcher.group(2)?.toIntOrNull() ?: startNodeHint
                    }
                    // Extract weeks
                    weeks = text.substringAfter(")").trim()
                } else if (text.contains("上课地点") || p.html().contains("map-marker")) {
                    location = text.replace("上课地点", "").trim()
                } else if (text.contains("教师") || p.html().contains("user")) {
                    teacher = text.replace("教师", "").trim()
                } else if (text.contains("教学班") || p.html().contains("home")) {
                     // Might appear multiple times, one for code, one for class composition
                     if (className.isEmpty()) {
                         className = text.replace("教学班名称", "").replace("教学班组成", "").trim()
                     }
                }
            }
            
            return Course(
                name = name,
                dayOfWeek = day,
                startNode = startNode,
                endNode = endNode,
                weeks = weeks,
                location = location,
                teacher = teacher,
                className = className
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
