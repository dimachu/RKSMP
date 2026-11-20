package edu.practice5

import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTest {
    @Test
    fun schedulesOnlyFutureLocal20() {
        val beforeReminder = ZonedDateTime.parse("2026-11-17T19:59:00+03:00[Europe/Moscow]")
        val atReminderTime = ZonedDateTime.parse("2026-11-17T20:00:00+03:00[Europe/Moscow]")
        val beforeClockChange = ZonedDateTime.parse("2026-03-28T21:00:00+01:00[Europe/Berlin]")

        assertEquals("2026-11-17T20:00+03:00[Europe/Moscow]", nextReminder(beforeReminder).toString())
        assertEquals("2026-11-18T20:00+03:00[Europe/Moscow]", nextReminder(atReminderTime).toString())
        assertEquals("2026-03-29T20:00+02:00[Europe/Berlin]", nextReminder(beforeClockChange).toString())
    }
}
