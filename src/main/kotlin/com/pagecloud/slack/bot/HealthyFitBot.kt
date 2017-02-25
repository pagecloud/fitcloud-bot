package com.pagecloud.slack.bot

import com.pagecloud.slack.SlackProperties
import me.ramswaroop.jbot.core.slack.Bot
import me.ramswaroop.jbot.core.slack.Controller
import me.ramswaroop.jbot.core.slack.EventType
import me.ramswaroop.jbot.core.slack.models.Event
import me.ramswaroop.jbot.core.slack.models.Message
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.*

/**
 * @author Edward Smith
 */
@Component
class HealthyFitBot(slackProperties: SlackProperties) : Bot() {

    val botToken = slackProperties.botToken!!
    val healthScheduler = slackProperties.healthScheduler!!
    val healthChannel = slackProperties.healthChannel!!
    var session: WebSocketSession? = null

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
        val msg = MESSAGE_REPLIES[Random().nextInt(MESSAGE_REPLIES.size)]
        reply(session, event, msg)
    })

    // Monday to Friday at 9:30 AM EST, HIT ME!
    @Scheduled(cron = "0 0/30 9 * * MON-FRI", zone = "EST")
    fun scheduleStretch() {
        val event = Event().apply { channelId = "@$healthScheduler" }
        startConversation(event, "confirmTime")
        reply(session, event, Message("Hey @$healthScheduler! What time is the stretch today?"))
    }

    @Controller(pattern = "(healthify schedule)", next = "confirmTime")
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
        reply(session, event, Message("OK, next stretch is set at ${event.text}. I'll send out reminders to #$healthChannel!"))
        stopConversation(event)
    })

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
        val MESSAGE_REPLIES = listOf(
            Message("Are my ears burning?"),
            Message("Did someone call my name?"),
            Message("What did you say?"),
            Message("Hmmmm?"),
            Message("I'm busy working on your health!"),
            Message("Is @guy trolling again?"),
            Message("What did @jim refactor this time?"),
            Message("Has @teejay's cookie evolved into a subhuman species yet?"),
            Message("@manbunnick should really keep the man bun going."),
            Message("What did Jim refactor this time?"),
            Message("Is @mgrouchy making bacon again soon?"),
            Message("Flexibility isn't useful, mobility is."),
            Message("Your body was designed to move, not sit idle. MOVE!")
        )
    }
}