package com.pagecloud.slack.bot

import me.ramswaroop.jbot.core.slack.models.Message

/**
 * @author Edward Smith
 */
class ProperMessage(private val messageText: String) : Message() {

    override fun toJSONString(): String {
        val prepped = messageText.replace(userTagRegex, { "ë@${it.groups["userIdentifier"]?.value}ë" })
        val encoded = prepped.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        this.text = encoded.replace(userRefRegex, { "<@${it.groups["userIdentifier"]?.value}>" })
        return super.toJSONString()
    }

    companion object {
        val userTagRegex = "<@(?<userIdentifier>[\\w-|]+)>".toRegex()
        val userRefRegex = "ë@(?<userIdentifier>[\\w-|]+)ë".toRegex()
    }
}