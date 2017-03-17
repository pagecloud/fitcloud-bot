package com.pagecloud.slack.bot

import com.pagecloud.slack.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Edward Smith
 */
@Service
class MovementSchedule {
    // Because... concurrency!
    private val nextSession: AtomicReference<LocalTime> = AtomicReference(LocalTime.of(11, 30))
    private val reminder: AtomicReference<Reminder> = AtomicReference(Reminder(LocalTime.of(11, 15)))

    @Scheduled(cron = "0 1 1 * * MON-FRI", zone = "America/Toronto")
    fun resetReminders() {
        val newReminder = reminder.get().copy(firedToday = false)
        reminder.set(newReminder)
        log.info("Resetting reminder state; now $newReminder.")
    }

    fun scheduleNext(nextTime: LocalTime) {
        nextSession.set(nextTime)
        reminder.set(Reminder(nextTime.minus(FIRST_REMINDER)))
    }

    fun shouldSendReminders(): Boolean {
        val next = nextSession.get()
        val now = LocalTime.now(ZoneId.of("America/Toronto"))

        // We'll just have to wait until next time I guess...
        if (now.isAfter(next)) {
            return false
        }
        val reminder = this.reminder.get()
        if (now.hour == reminder.time.hour && now.minute == reminder.time.minute && !reminder.firedToday) {
            return true
        }
        return false
    }

    fun sendReminder(reminderFunction: (String) -> Unit) {
        val reminder = this.reminder.get()
        if (!reminder.firedToday) reminder.fire(reminderFunction)
    }

    companion object {
        val WINDOW: Duration = Duration.ofSeconds(60L)
        val FIRST_REMINDER = Duration.ofMinutes(60L)
        val SECOND_REMINDER = Duration.ofMinutes(15L)
        val LAST_REMINDER = Duration.ofMinutes(1L)

        val log = logger()
    }
}

data class Reminder(val time: LocalTime,
                    var firedToday: Boolean = false) {
    fun fire(reminderFunction: (String) -> Unit) {
        val prettyTime = time.format(PRETTY_FORMAT)
        val message = "Hey Healthy and Fit team! Just a reminder that the next stretch will be at $prettyTime!"
        reminderFunction(message)
        firedToday = true
    }
}


fun LocalTime.isAfter(otherTime: LocalTime, threshold: Duration): Boolean =
    this.isAfter(otherTime) && Duration.between(this, otherTime) <= threshold

val PRETTY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")