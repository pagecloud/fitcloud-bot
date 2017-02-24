package com.pagecloud.slack

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.webflux.WebFluxFunctionalAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.server.adapter.WebHttpHandlerBuilder

/**
 * @author Edward Smith
 */

@SpringBootApplication
@EnableConfigurationProperties(SlackProperties::class)
class RouterApplication

@ConfigurationProperties(prefix = "slack")
class SlackProperties {
    var webhookUrl: String? = ""
}

fun main(args: Array<String>) {
    SpringApplication.run(RouterApplication::class.java, *args)
}