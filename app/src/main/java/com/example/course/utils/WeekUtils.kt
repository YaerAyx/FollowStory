package com.example.course.utils

import java.util.Calendar

object WeekUtils {

    /**
     * Calculates the current week number based on the term start date.
     * @param startMillis The term start date in milliseconds.
     * @return 1-indexed week number.
     */
    fun calculateCurrentWeek(startMillis: Long): Int {
        if (startMillis == 0L) return 1
        
        val now = System.currentTimeMillis()
        if (now < startMillis) return 0 // Term hasn't started yet
        
        val diff = now - startMillis
        val weeks = diff / (1000 * 60 * 60 * 24 * 7)
        return weeks.toInt() + 1
    }

    /**
     * Checks if a given week number is included in the raw week string.
     * @param rawWeeks e.g. "3-9周,13周" or "1-16周(双)"
     * @param currentWeek The week number to check.
     */
    fun isWeekInPattern(rawWeeks: String, currentWeek: Int): Boolean {
        if (rawWeeks.isEmpty()) return true
        
        // Clean up string: remove "周" and handle special cases like (单), (双)
        val clean = rawWeeks.replace("周", "").trim()
        
        // Handle (单) / (双) 
        val isSingleOnly = clean.contains("(单)")
        val isDoubleOnly = clean.contains("(双)")
        val baseWeeks = clean.replace("(单)", "").replace("(双)", "").trim()
        
        if (isSingleOnly && currentWeek % 2 == 0) return false
        if (isDoubleOnly && currentWeek % 2 != 0) return false
        
        // Split by comma for multiple ranges/weeks
        val parts = baseWeeks.split(",")
        for (part in parts) {
            val range = part.trim()
            if (range.contains("-")) {
                val startEnd = range.split("-")
                if (startEnd.size == 2) {
                    val start = startEnd[0].toIntOrNull() ?: 0
                    val end = startEnd[1].toIntOrNull() ?: 0
                    if (currentWeek in start..end) return true
                }
            } else {
                val week = range.toIntOrNull()
                if (week == currentWeek) return true
            }
        }
        
        // Fallback for some strange strings if not matched yet
        if (rawWeeks.contains("-$currentWeek") || rawWeeks.contains("$currentWeek-")) return true
        
        return false
    }
}
