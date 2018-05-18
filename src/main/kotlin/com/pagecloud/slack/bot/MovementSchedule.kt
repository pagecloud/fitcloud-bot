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
    private var nextSession: LocalTime = LocalTime.of(11, 30)
    private var reminder: Reminder = Reminder(LocalTime.of(11, 15), LocalTime.of(11, 30))
    private var imminentReminder: Reminder = Reminder(
        LocalTime.of(11, 30),
        LocalTime.of(11, 30),
        messageTemplate=IMMINENT_MESSAGE)

    @Scheduled(cron = "0 1 1 * * MON-FRI", zone = "America/Toronto")
    fun resetReminders() {
        reminder = reminder.copy(firedToday = false)
        imminentReminder = imminentReminder.copy(firedToday = false)
        log.info("Resetting reminder states; now $reminder and $imminentReminder.")
    }

    fun scheduleNext(nextTime: LocalTime) {
        nextSession = nextTime
        reminder = Reminder(nextTime.minus(SECOND_REMINDER), nextTime)
        imminentReminder = Reminder(nextTime, nextTime)
    }

    fun getRemindersToSend(): List<Reminder> =
        when {
            // We'll just have to wait until next time I guess...
            now().isAfter(nextSession) -> emptyList<Reminder>()
            reminder.shouldFire() -> listOf(reminder)
            imminentReminder.shouldFire() -> listOf(imminentReminder)
            else -> emptyList<Reminder>()
        }

    companion object {
        val IMMINENT_MESSAGE = "Hey Healthy and Fit team! It's time to move, stretch and feel good!"
        val SECOND_REMINDER: Duration = Duration.ofMinutes(15L)

        val log = logger()
    }
}

data class Reminder(val time: LocalTime,
                    val moveTime: LocalTime,
                    var firedToday: Boolean = false,
                    val messageTemplate: String = "Hey Healthy and Fit team! Just a reminder that the next stretch will be at {prettyTime}!") {

    fun shouldFire(): Boolean {
        val now = LocalTime.now(ZoneId.of("America/Toronto"))
        return now.hour == time.hour && now.minute == time.minute && !firedToday
    }

    fun fire(reminderFunction: (String) -> Unit) {
        val message = messageTemplate.replace("{prettyTime}", moveTime.format(PRETTY_FORMAT))
        reminderFunction(message)
        firedToday = true
    }
}

fun now() = LocalTime.now(ZoneId.of("America/Toronto"))
val PRETTY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")