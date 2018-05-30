package com.pagecloud.slack.bot

import com.pagecloud.slack.Slack
import com.pagecloud.slack.SlackProperties
import com.pagecloud.slack.isHoliday
import com.pagecloud.slack.logger
import me.ramswaroop.jbot.core.common.JBot
import me.ramswaroop.jbot.core.slack.Bot
import me.ramswaroop.jbot.core.common.EventType
import me.ramswaroop.jbot.core.common.Controller;
import me.ramswaroop.jbot.core.slack.models.Event
import me.ramswaroop.jbot.core.slack.models.Message
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.socket.WebSocketSession
import java.security.SecureRandom
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

/**
 * @author Edward Smith
 */
@JBot
class HealthyFitBot(slackProperties: SlackProperties,
                    val slack: Slack,
                    val movementSchedule: MovementSchedule,
                    val redisTemplate: ReactiveRedisTemplate<String, String>) : Bot() {

    val botToken = slackProperties.botToken!!
    val healthScheduler = slackProperties.healthScheduler!!
    val healthChannel = slackProperties.healthChannel!!
    lateinit var session: WebSocketSession

    var lastIndex: Int = -1
    val random by lazy { SecureRandom() }

    // Delegate the paused property to a Redis key to persist across restarts
    var paused: Boolean
        get() = "true".equals(redisTemplate.opsForValue().get("paused").block(), ignoreCase = true)
        set(it) {
            redisTemplate.opsForValue().set("paused", "$it").subscribe()
        }

    @Controller(events = [EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE])
    fun onDirectMessage(session: WebSocketSession, event: Event) = handleMessage(event, {
        if (event.text.endsWith("pause") || event.text.endsWith("unpause")) {
            val unpause = event.text.endsWith("unpause")
            val msg = if (unpause)
                "*start* asking you about the next stretch time again."
            else
                "_stop_ asking you about the next stretch time for now."
            paused = !unpause
            reply(session, event, Message("OK, I'll $msg"))
        } else if (event.text.endsWith("cancel")) {
            movementSchedule.scheduleNext(OFF)
            reply(session, event, Message("OK! I've *cancelled the stretch* for now."))
        } else {
            val message = randomReply()
            reply(session, event, message)
        }
    })

    private fun randomReply(): Message {
        var index = random.nextInt(MESSAGE_REPLIES.size)
        if (lastIndex == index) {
            index = random.nextInt(MESSAGE_REPLIES.size)
        }
        val msg = MESSAGE_REPLIES[index]
        if ("<@" in msg) {
            return ProperMessage(msg.replace(userTagPattern, { matchResult ->
                val username = matchResult.groups["username"]!!.value
                slack.getUser(username)?.let { user ->
                    "<@${user.id}|$username>"
                } ?: matchResult.value
            }))
        }
        return ProperMessage(msg)
    }

    // Monday to Friday at 9:30 AM, find out what time the stretch should be
    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "America/Toronto")
    fun scheduleStretch() {
        log.info("Asking @$healthScheduler for next stretch time")
        if (paused) return

        if (!isHoliday(LocalDate.now())) {
            val channel = slack.getChannel(healthChannel)
            channel?.let {
                val event = Event().apply {
                    id = Random().nextInt() + 1
                    channelId = channel.id
                }
                startConversation(event, "confirmTime")
                reply(session, event, Message("Hey @$healthScheduler! What time is the stretch today?"))
            } ?: log.error("No such user $healthScheduler")
        } else {
            log.info("It's a holiday! No stretching today.")
        }
    }

    // Check every minute, Monday to Friday EST
    @Scheduled(cron = "* 0-59 * * * MON-FRI", zone = "America/Toronto")
    fun sendReminders() {
        log.trace("Checking whether reminders should be sent...")
        movementSchedule.getRemindersToSend().forEach { reminder ->
            log.info("OK! Sending $reminder!")
            reminder.fire { message ->
                val channel = slack.getChannel(healthChannel)
                channel?.let {
                    val event = Event().apply {
                        id = Random().nextInt() + 1
                        channelId = channel.id
                    }
                    reply(session, event, Message(message))
                } ?: log.error("No such channel $healthChannel")
            }
        }
    }

    @Controller(
        events = [EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE],
        pattern = "(schedule)",
        next = "confirmTime")
    fun rescheduleStretch(session: WebSocketSession, event: Event) = handleMessage(event, {
        reply(session, event, Message("Hey! What time is the stretch today?"))
        if (!isConversationOn(event)) {
            startConversation(event, "confirmTime")
        } else {
            nextConversation(event)
        }
    })

    @Controller
    fun confirmTime(session: WebSocketSession, event: Event) = handleConversation(event, {
        val nextTime = parseNextTime(event.text)
        val pretty =
            if (OFF == nextTime) "stretch is off for now."
            else "next stretch is set at ${nextTime.format(PRETTY_FORMAT)}. I'll send out reminders to #$healthChannel!"

        reply(session, event, Message("OK, $pretty"))
        movementSchedule.scheduleNext(nextTime)
        stopConversation(event)
    })

    private fun parseNextTime(timeInput: String): LocalTime {
        if ("off".equals(timeInput, ignoreCase = true)) return OFF

        for (parser in TIME_PARSERS) {
            try {
                return LocalTime.parse(timeInput.toUpperCase(), parser)
            } catch (e: DateTimeParseException) {
                continue
            }
        }
        val defaultTime = LocalTime.of(11, 30)
        log.error("Unable to parse $timeInput; defaulting to $defaultTime")
        return defaultTime
    }

    /**
     * Only executes the Slack message handler function if it isn't a message "from the bot itself"
     */
    fun <T> handleMessage(event: Event, handlerFunc: () -> T) {
        if (event.userId != slackService.currentUser.id) {
            handlerFunc()
        }
    }

    fun <T> handleConversation(event: Event, handlerFunc: () -> T) {
        if (isConversationOn(event) && event.userId != slackService.currentUser.id) {
            handlerFunc()
        }

    }

    companion object {
        val PRETTY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        val TIME_PARSERS = listOf(
            DateTimeFormatter.ofPattern("hh:mm a", Locale.CANADA),
            DateTimeFormatter.ofPattern("h:mm a", Locale.CANADA),
            DateTimeFormatter.ofPattern("hh:mm", Locale.CANADA),
            DateTimeFormatter.ofPattern("kk:mm", Locale.CANADA),
            DateTimeFormatter.ofPattern("k:mm", Locale.CANADA),
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ISO_TIME,
            DateTimeFormatter.ISO_OFFSET_TIME
        )
        val MESSAGE_REPLIES = listOf(
            "<@tednology> wrote a mean Bot, didn't he?",
            "Are my ears burning?",
            "Did someone call my name?",
            "What did you say?",
            "Hmmmm?",
            "I'm busy working on your health!",
            "Has <@teejay>'s cookie evolved into a subhuman species yet?",
            "<@justnick> should turn that mop into a top-knot.",
            "Is <@mgrouchy> making bacon again soon?",
            "Flexibility isn't useful, mobility is.",
            "Your body was designed to move, not sit idle. MOVE!"
        )
        val log = logger()
        val userTagPattern = "<@(?<username>[\\w-]+)>".toRegex()
    }

    override fun getSlackBot() = this
    override fun getSlackToken() = botToken
    override fun afterConnectionEstablished(session: WebSocketSession) { this.session = session }
}
