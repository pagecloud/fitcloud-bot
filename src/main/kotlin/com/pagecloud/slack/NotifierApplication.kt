package com.pagecloud.slack

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.pagecloud.http.FormObjectMessageReader
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.embedded.reactor.ReactorNettyReactiveWebServerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.reactive.function.client.WebClient

/**
 * @author Edward Smith
 */

@SpringBootApplication(scanBasePackages = arrayOf("me.ramswaroop.jbot", "com.pagecloud.slack"))
@EnableConfigurationProperties(SlackProperties::class)
@EnableScheduling
class RouterApplication {
    @Bean
    fun reactorNettyReactiveWebServerFactory(): ReactorNettyReactiveWebServerFactory
        = ReactorNettyReactiveWebServerFactory()

    @Bean
    fun formObjectMessageReader() = FormObjectMessageReader<Any>()

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule()
}

fun main(args: Array<String>) {
    when {
        // Cheap trick to prevent your free tier dyno from sleeping
        args.isNotEmpty() && args[0] == "refresh" ->
            System.getenv("HEROKU_APP_NAME")?.let {
                val herokuUrl = "https://$it.herokuapp.com"
                WebClient.create().get().uri(herokuUrl).exchange().then { response ->
                    response.bodyToMono(String::class.java)
                        .doOnSuccess(::println)
                }
            }
        else ->
            SpringApplicationBuilder()
                .sources(RouterApplication::class.java)
                .web(WebApplicationType.REACTIVE)
                .build()
                .run(*args)
    }
}

@ConfigurationProperties(prefix = "slack")
class SlackProperties {
    var healthScheduler: String? = ""
    var healthChannel: String? = ""
    var webhookUrl: String? = ""
    var botToken: String? = ""
    var slashCommandToken: String? = ""
}
