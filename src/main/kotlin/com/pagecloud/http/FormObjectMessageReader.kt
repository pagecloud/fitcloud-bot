package com.pagecloud.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.core.ResolvableType
import org.springframework.core.codec.Decoder
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpInputMessage
import org.springframework.http.codec.HttpMessageReader
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.util.StringUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets


class FormObjectMessageReader<T> : HttpMessageReader<T> {

    val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

    override fun canRead(elementType: ResolvableType, mediaType: MediaType): Boolean {
        return MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)
    }

    override fun getReadableMediaTypes(): List<MediaType> {
        return READABLE_MEDIA_TYPES
    }

    override fun read(elementType: ResolvableType, inputMessage: ReactiveHttpInputMessage, hints: Map<String, Any>): Flux<T> {
        val contentType = inputMessage.headers.contentType
        val charset = getMediaTypeCharset(contentType)

        val postParams = inputMessage.body
            .reduce { obj, buffers -> obj.write(buffers) }
            .map { buffer ->
                val charBuffer = charset.decode(buffer.asByteBuffer())
                val body = charBuffer.toString()
                DataBufferUtils.release(buffer)
                parseFormData(charset, body)
            }

        return postParams.flatMap { params ->
            val jsonEncoded = objectMapper.writeValueAsString(params.toSingleValueMap())
            Flux.just(objectMapper.readValue(jsonEncoded, elementType.resolve()))
        } as Flux<T>
    }

    override fun readMono(elementType: ResolvableType, inputMessage: ReactiveHttpInputMessage, hints: Map<String, Any>): Mono<T> {
        val contentType = inputMessage.headers.contentType
        val charset = getMediaTypeCharset(contentType)

        val postParams = inputMessage.body
            .reduce { obj, buffers -> obj.write(buffers) }
            .map { buffer ->
                val charBuffer = charset.decode(buffer.asByteBuffer())
                val body = charBuffer.toString()
                DataBufferUtils.release(buffer)
                parseFormData(charset, body)
            }

        return postParams.then { params ->
            val jsonEncoded = objectMapper.writeValueAsString(params.toSingleValueMap())
            Mono.just(objectMapper.readValue(jsonEncoded, elementType.resolve()))
        } as Mono<T>
    }

    private fun parseFormData(charset: Charset, body: String): MultiValueMap<String, String> {
        val pairs = StringUtils.tokenizeToStringArray(body, "&")
        val result = LinkedMultiValueMap<String, String>(pairs.size)
        try {
            for (pair in pairs) {
                val idx = pair.indexOf('=')
                if (idx == -1) {
                    result.add(URLDecoder.decode(pair, charset.name()), null)
                } else {
                    val name = URLDecoder.decode(pair.substring(0, idx), charset.name())
                    val value = URLDecoder.decode(pair.substring(idx + 1), charset.name())
                    result.add(name, value)
                }
            }
        } catch (ex: UnsupportedEncodingException) {
            throw IllegalStateException(ex)
        }

        return result
    }

    private fun getMediaTypeCharset(mediaType: MediaType): Charset {
        if (mediaType.charset != null) {
            return mediaType.charset
        } else {
            return StandardCharsets.UTF_8
        }
    }

    companion object {
        val READABLE_MEDIA_TYPES = listOf(MediaType.APPLICATION_FORM_URLENCODED)
    }

}