package com.pagecloud.slack

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.reactiveweb.FunctionalReactiveWebAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.server.adapter.WebHttpHandlerBuilder

/**
 * @author Edward Smith
 */

@SpringBootApplication(exclude = arrayOf(FunctionalReactiveWebAutoConfiguration::class))
@EnableConfigurationProperties(SlackProperties::class)
class RouterApplication {
    @Bean
    fun httpHandler(router: Router): HttpHandler
        = WebHttpHandlerBuilder
            .webHandler(RouterFunctions.toHttpHandler(router))
            .build()
}

@ConfigurationProperties(prefix = "slack")
class SlackProperties {
    var webhookUrl: String? = ""
}

fun main(args: Array<String>) {
    SpringApplication.run(RouterApplication::class.java, *args)
}