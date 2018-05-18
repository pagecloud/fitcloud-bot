package com.pagecloud.slack

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.pagecloud.slack.bot.MovementSchedule
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.http.codec.FormHttpMessageReader
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * @author Edward Smith
 */

@SpringBootApplication(scanBasePackages = ["me.ramswaroop.jbot", "com.pagecloud.slack"])
@EnableConfigurationProperties(SlackProperties::class)
@EnableScheduling @EnableCaching
class RouterApplication {
    @Bean
    fun reactorNettyReactiveWebServerFactory() = NettyReactiveWebServerFactory()

    @Bean
    fun formObjectMessageReader() = FormHttpMessageReader()

    @Bean
    fun kotlinModule() = KotlinModule()

    @Bean
    fun movementSchedule() = MovementSchedule()
}

fun main(args: Array<String>) {
    SpringApplicationBuilder()
        .sources(RouterApplication::class.java)
        .web(WebApplicationType.REACTIVE)
        .build()
        .run(*args)
}

@ConfigurationProperties(prefix = "slack")
class SlackProperties {
    var healthScheduler: String? = ""
    var healthChannel: String? = ""
    var webhookUrl: String? = ""
    var botToken: String? = ""
    var slashCommandToken: String? = ""
}
