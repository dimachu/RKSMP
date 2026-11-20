package edu.practice5

import java.time.ZonedDateTime

fun nextReminder(now: ZonedDateTime): ZonedDateTime {
    val today = now.toLocalDate()
    val evening = today.atTime(20, 0).atZone(now.zone)
    return if (evening.isAfter(now)) {
        evening
    } else {
        today.plusDays(1).atTime(20, 0).atZone(now.zone)
    }
}
