package com.pagecloud.slack

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono

/**
 * @author Edward Smith
 */
@Component
class Router(val slackPropeties: SlackProperties) : RouterFunction<ServerResponse> {

    val webClient: WebClient = WebClient.create()

    override fun route(request: ServerRequest): Mono<HandlerFunction<ServerResponse>> {
        return route(POST("/notify/{channel}"), notifyChannel()).and(
            route(POST("/notify"), notifyChannel())).and(
            route(GET("/**"), sayHello()))
            .route(request)
    }

    fun sayHello() = HandlerFunction { req ->
        ok().contentType(MediaType.APPLICATION_JSON).body(fromObject("Hello!"))
    }

    fun notifyChannel() = HandlerFunction { req ->
        req.bodyToMono(IncomingMessage::class.java).then { incoming ->
            if (incoming.apiKey == "someApiKey") {

                val channel = req.pathVariables().getOrElse("channel", { "#eng" })
                val message = SlackMessage(
                    username = incoming.username,
                    channel = if (channel[0] == '#' || channel[0] =='@') channel else "#$channel",
                    text = incoming.text,
                    attachments = listOf(
                        Attachment(
                            title = incoming.title,
                            text = incoming.details
                        )
                    )
                )
                webClient.post()
                    .uri(slackPropeties.webhookUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .exchange(fromObject(message)).then { response ->
                        if (response.statusCode().is2xxSuccessful) ok().build()
                        else status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
            } else {
                status(HttpStatus.FORBIDDEN).build()
            }
        }.otherwise { badRequest().build() }
    }
}

data class IncomingMessage(val apiKey: String,
                           val text: String,
                           val title: String,
                           val details: String,
                           val username: String = "eng-notifications")

data class SlackMessage(val username: String,
                        val channel: String,
                        val text: String,
                        val attachments: List<Attachment>,
                        val markdwn: Boolean = true)

data class Attachment(val title: String,
                      val text: String)
