package com.pagecloud.slack.bot

import com.pagecloud.slack.Slack
import com.pagecloud.slack.SlackProperties
import com.pagecloud.slack.logger
import me.ramswaroop.jbot.core.slack.Bot
import me.ramswaroop.jbot.core.slack.Controller
import me.ramswaroop.jbot.core.slack.EventType
import me.ramswaroop.jbot.core.slack.models.Event
import me.ramswaroop.jbot.core.slack.models.Message
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.security.SecureRandom
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

/**
 * @author Edward Smith
 */
@Component
class HealthyFitBot(slackProperties: SlackProperties,
                    val slack: Slack,
                    val movementSchedule: MovementSchedule) : Bot() {

    val botToken = slackProperties.botToken!!
    val healthScheduler = slackProperties.healthScheduler!!
    val healthChannel = slackProperties.healthChannel!!
    var session: WebSocketSession? = null
    var lastReply: String = ""
    val random by lazy { SecureRandom() }

    override fun getSlackBot(): Bot {
        return this
    }

    override fun getSlackToken(): String {
        return botToken
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        this.session = session
    }

    @Controller(events = arrayOf(EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE))
    fun onDirectMessage(session: WebSocketSession, event: Event) = handleMessage(event, {
        val message = randomReply(lastReply)
        lastReply = message.text
        reply(session, event, message)
    })

    private fun randomReply(lastText: String): Message {
        val msg = MESSAGE_REPLIES[random.nextInt(MESSAGE_REPLIES.size)]
        if (msg.text == lastText) {
            return randomReply(lastText)
        }
        return msg
    }

    // Monday to Friday at 9:30 AM, find out what time the stretch should be
    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "America/Toronto")
    fun scheduleStretch() {
        log.info("Asking @$healthScheduler for next stretch time")
        val channel = slack.getChannel(healthChannel)
        channel?.let {
            val event = Event().apply {
                id = Random().nextInt() + 1
                channelId = channel.id
            }
            startConversation(event, "confirmTime")
            reply(session, event, Message("Hey @$healthScheduler! What time is the stretch today?"))
        } ?: log.error("No such user $healthScheduler")
    }

    // Check every minute, Monday to Friday EST
    @Scheduled(cron = "* 0-59 * * * MON-FRI", zone = "America/Toronto")
    fun sendReminders() {
        log.trace("Checking whether reminders should be sent...")
        if (movementSchedule.shouldSendReminders()) {
            log.info("OK! Sending reminder!")
            movementSchedule.sendReminder { message ->
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
        events = arrayOf(EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE),
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
        val pretty = nextTime.format(PRETTY_FORMAT)
        reply(session, event, Message("OK, next stretch is set at $pretty. I'll send out reminders to #$healthChannel!"))
        movementSchedule.scheduleNext(nextTime)
        stopConversation(event)
    })

    private fun parseNextTime(timeInput: String): LocalTime {
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
            Message("Are my ears burning?"),
            Message("Did someone call my name?"),
            Message("What did you say?"),
            Message("Hmmmm?"),
            Message("I'm busy working on your health!"),
            Message("Is @guy trolling again?"),
            Message("Has @teejay's cookie evolved into a subhuman species yet?"),
            Message("@manbunnick should really keep the man bun going."),
            Message("Is @mgrouchy making bacon again soon?"),
            Message("Flexibility isn't useful, mobility is."),
            Message("Your body was designed to move, not sit idle. MOVE!"),
            Message("Your body was designed to move, not sit idle. MOVE!")
        )
        val log = logger()
    }
}
