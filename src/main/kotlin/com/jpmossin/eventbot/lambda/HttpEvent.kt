package com.jpmossin.eventbot.lambda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * The data of the input event sent by API Gateway
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HttpEvent(
        val path: String,
        val httpMethod: String,
        val body: String? = null,
        val headers: Map<String, String>? = emptyMap(),
        val queryStringParameters: Map<String, String>? = emptyMap(),
        val pathParameters: Map<String, String>? = emptyMap()
) {

    private val lowerCaseHeaders = headers?.mapKeys { it.key.toLowerCase() }

    inline fun <reified T> bodyObject(): T? {
        if (body == null) {
            return null
        }
        try {
            return LambdaCommon.objectMapper.readValue<T>(body)
        } catch (e: JsonProcessingException) {
            throw InvalidClientRequestException("Could not parse request body", e)
        }
    }

    fun getHeader(header: String): String? {
        return lowerCaseHeaders?.get(header.toLowerCase())
    }

    fun getQueryParam(key: String): String? {
        return queryStringParameters?.get(key)
    }

    fun getPathParam(key: String): String? {
        return pathParameters?.get(key)
    }

}

