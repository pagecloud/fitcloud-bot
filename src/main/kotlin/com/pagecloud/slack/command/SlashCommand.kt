package com.pagecloud.slack.command

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.pagecloud.slack.SlackProperties
import com.pagecloud.slack.logger
import me.ramswaroop.jbot.core.slack.models.Attachment
import me.ramswaroop.jbot.core.slack.models.RichMessage
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyExtractor
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.RouterFunctions.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono

/**
 * Currently unused.
 *
 * @author Edward Smith
 */
@Component
class SlackSlashCommandHandler(slackProperties: SlackProperties,
                               val objectMapper: ObjectMapper) : RouterFunction<ServerResponse> {

    val slashCommandToken = slackProperties.slashCommandToken

    override fun route(request: ServerRequest): Mono<HandlerFunction<ServerResponse>> {
        return route(POST("/healthify/slash-command"), handleCommand())
            .route(request)
    }

    fun handleCommand() = HandlerFunction { req ->
        req.bodyToMono(SlashCommandData::class).then { command ->
            if (command.token != slashCommandToken) {
                ok().body(RichMessage("Sorry! You're not lucky enough to use our slack command."))
            } else {
                val richMessage = RichMessage("The is Slash Commander!").apply {
                    responseType = "in_channel"
                    attachments = arrayOf(
                        Attachment().apply {
                            text = "I will perform all tasks for you."
                        }
                    )
                }
                log.info("Replying with {}", objectMapper.writeValueAsString(richMessage))

                ok().body(richMessage.encodedMessage())
            }
        }
    }

    companion object { val log = logger() }
}

data class SlashCommandData(val token: String,
                            @JsonProperty("team_id") val teamId: String,
                            @JsonProperty("team_domain") val teamDomain: String,
                            @JsonProperty("channel_id") val channelId: String,
                            @JsonProperty("channel_name") val channelName: String,
                            @JsonProperty("user_id") val userId: String,
                            @JsonProperty("user_name") val userName: String,
                            val command: String,
                            val text: String,
                            @JsonProperty("response_url") val responseUrl: String)

fun MultiValueMap<String,String>.toSlashCommandData(): SlashCommandData
    = SlashCommandData(
        this["token"]?.first() ?: "",
        this["teamId"]?.first() ?: "",
        this["teamDomain"]?.first() ?: "",
        this["channelId"]?.first() ?: "",
        this["channelName"]?.first() ?: "",
        this["userId"]?.first() ?: "",
        this["userName"]?.first() ?: "",
        this["command"]?.first() ?: "",
        this["text"]?.first() ?: "",
        this["responseUrl"]?.first() ?: ""
    )

fun ServerResponse.BodyBuilder.body(richMessage: RichMessage): Mono<ServerResponse> = body(fromObject(richMessage))