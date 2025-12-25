package com.kelompok1.fandomhub.utils

import java.util.Date
import java.util.concurrent.TimeUnit

object DateUtils {
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago" // e.g., "5m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago" // e.g., "2h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago" // e.g., "3d ago"
            else -> {
                // Determine format using Calendar
                val calendar = java.util.Calendar.getInstance()
                calendar.time = Date(timestamp)
                
                val currentCalendar = java.util.Calendar.getInstance()
                currentCalendar.time = Date(now)

                // SimpleDateFormat is better but we can stick to logic
                val formatPattern = if (currentCalendar.get(java.util.Calendar.YEAR) == calendar.get(java.util.Calendar.YEAR)) {
                    "d/M"
                } else {
                    "d/M/yyyy"
                }
                
                val sdf = java.text.SimpleDateFormat(formatPattern, java.util.Locale.getDefault())
                sdf.format(calendar.time)
            }
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy h:mm a", java.util.Locale.getDefault())
        return format.format(date)
    }

    fun getRemainingDays(validUntil: Long): String {
        val now = System.currentTimeMillis()
        if (validUntil <= now) return "Expired"
        
        val diff = validUntil - now
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        
        return when {
            days > 0 -> "$days days left"
            hours > 0 -> "Expires in $hours hours"
            else -> "Expires soon"
        }
    }
}
