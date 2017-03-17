package com.pagecloud.slack

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.math.BigDecimal

/**
 * @author Edward Smith
 */
@Service
class Slack(slackProperties: SlackProperties) {

    val webClient: WebClient = WebClient.create()
    val token = slackProperties.botToken

    // TODO - replace with actual @Cacheable / ttl
    val users by lazy { fetchUsers() }
    val channels by lazy { fetchChannels() }

    fun getUser(username: String): User? = users[username]
    fun getChannel(name: String): Channel? = channels[name]

    fun fetchUsers(): Map<String, User> {
        return webClient.get()
            .uri("$BASE_URL/users.list?token=$token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange().then { resp ->
                if (resp.statusCode().is2xxSuccessful) {
                    resp.bodyToMono(UserListResponse::class.java).then { response ->
                        when {
                            response.ok -> Mono.just(response.users.associateBy(User::name, { it }))
                            else -> {
                                log.error("Could not fetch Users. Error: ${response.error}; Warning: ${response.warning}")
                                Mono.just(emptyMap<String, User>())
                            }
                        }
                    }
                } else {
                    log.error("Could not fetch Users, HTTP status ${resp.statusCode()}")
                    Mono.just(emptyMap<String, User>())
                }
            }.block() // Sadness - should be Mono return type but whatever
    }

    fun fetchChannels(): Map<String, Channel> {
        return webClient.get()
            .uri("$BASE_URL/channels.list?token=$token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange().then { resp ->
            if (resp.statusCode().is2xxSuccessful) {
                resp.bodyToMono(ChannelListResponse::class.java).then { response ->
                    when {
                        response.ok -> Mono.just(response.channels.associateBy({ it.name }, { it }))
                        else -> {
                            log.error("Could not fetch Channels. Error: ${response.error}; Warning: ${response.warning}")
                            Mono.just(emptyMap<String, Channel>())
                        }
                    }
                }
            } else {
                log.error("Could not fetch Users, HTTP status ${resp.statusCode()}")
                Mono.just(emptyMap<String, Channel>())
            }
        }.block()
    }

    companion object {
        val log = logger()
        const val BASE_URL = "https://slack.com/api"
    }
}


data class User(val id: String,
                @JsonProperty("team_id") val teamId: String,
                val name: String,
                val status: String?,
                val deleted: Boolean,
                val color: String,
                val profile: Profile,
                @JsonProperty("is_admin") val admin: Boolean,
                @JsonProperty("is_owner") val owner: Boolean,
                @JsonProperty("is_primary_owner") val primaryOwner: Boolean?,
                @JsonProperty("is_restricted") val restricted: Boolean?,
                @JsonProperty("is_ultra_restricted") val ultraRestricted: Boolean?,
                @JsonProperty("has_2fa") val has2FactorAuth: Boolean,
                @JsonProperty("two_factor_type") val twoFactorType: String?)

data class Profile(@JsonProperty("first_name")
                   val firstName: String,
                   @JsonProperty("last_name")
                   val lastName: String,
                   @JsonProperty("real_name")
                   val realName: String,
                   val skype: String?,
                   val email: String,
                   val phone: String?,
                   val image_24: String,
                   val image_32: String,
                   val image_48: String,
                   val image_72: String,
                   val image_192: String,
                   val image_512: String?)

data class Channel(val id: String,
                   val name: String,
                   @JsonProperty("is_channel") val channel: Boolean?,
                   val created: Long,
                   val creator: String,
                   val numMembers: Int?,
                   @JsonProperty("is_archived") val archived: Boolean,
                   @JsonProperty("is_general") val general: Boolean?,
                   val members: List<String>?,
                   val topic: Topic?,
                   val purpose: Purpose,
                   @JsonProperty("is_member") val member: Boolean? = false,
                   @JsonProperty("last_read") val lastRead: BigDecimal?,
                   val unreadCount: Int?,
                   val unreadCountDisplay: Int?)

data class Topic(val value: String,
                 val creator: String,
                 val lastSet: Long)

data class Purpose(val value: String,
                   val creator: String,
                   val lastSet: Long)

class UserListResponse(ok: Boolean,
                       error: String?,
                       warning: String?,
                       @JsonProperty("members") val users: List<User>) : BaseResponse(ok, error, warning)

class ChannelListResponse(ok: Boolean,
                          error: String?,
                          warning: String?,
                          val channels: List<Channel>) : BaseResponse(ok, error, warning)

abstract class BaseResponse(val ok: Boolean,
                            val error: String?,
                            val warning: String?)