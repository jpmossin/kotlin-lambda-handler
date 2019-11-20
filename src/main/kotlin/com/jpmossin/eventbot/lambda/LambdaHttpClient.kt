package com.jpmossin.eventbot.lambda

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream


/**
 * A very basic and light weight http client with no external dependencies.
 * This avoids dragging in things like Apache http, which has higher throughput
 * but adds more start-up overhead to the Lambda function.
 */
interface LambdaHttpClient {

    fun request(
        method: String,
        url: String,
        headers: Map<String, String> = mapOf(),
        body: String? = null,
        connectionConfig: ConnectionConfig = ConnectionConfig()
    ): HttpResponse

    fun get(
        url: String,
        headers: Map<String, String> = mapOf(),
        connectionConfig: ConnectionConfig = ConnectionConfig()
    ): HttpResponse {
        return request("GET", url, headers, null, connectionConfig)
    }

    fun post(
        url: String,
        headers: Map<String, String> = mapOf(),
        body: String? = null,
        connectionConfig: ConnectionConfig = ConnectionConfig()
    ): HttpResponse {
        return request("POST", url, headers, body, connectionConfig)
    }

    fun put(
        url: String,
        headers: Map<String, String> = mapOf(),
        body: String? = null,
        connectionConfig: ConnectionConfig = ConnectionConfig()
    ): HttpResponse {
        return request("PUT", url, headers, body, connectionConfig)
    }

    fun delete(
        url: String,
        headers: Map<String, String> = mapOf(),
        connectionConfig: ConnectionConfig = ConnectionConfig()
    ): HttpResponse {
        return request("DELETE", url, headers, null, connectionConfig)
    }


    companion object {
        fun create(): LambdaHttpClient {
            return HttpUrlConnectionClient()
        }
    }


    class HttpResponse(val statusCode: Int, val body: String, val headers: Map<String, String>) {

        inline fun <reified T> bodyObject(): T = LambdaCommon.objectMapper.readValue(body)

        fun json(): JsonNode {
            return LambdaCommon.objectMapper.readTree(body)
        }

        override fun toString(): String {
            return "HttpResponse(statusCode=$statusCode, body='$body', headers=$headers)"
        }
    }

    class ConnectionConfig(
        val connectTimeout: Int = 2000,  // See URLConnection.connectTimeout
        val readTimeout: Int = 4000,     // See URLConnection.readTimeout
        val followRedirects: Boolean = false
    )

    private class HttpUrlConnectionClient : LambdaHttpClient {

        override fun request(
            method: String,
            url: String,
            headers: Map<String, String>,
            body: String?,
            connectionConfig: ConnectionConfig
        ): HttpResponse {
            val urlConnection = createConnection(url, method, headers, body, connectionConfig)

            val statusCode = urlConnection.responseCode
            val stream = if (statusCode < 400) urlConnection.inputStream else urlConnection.errorStream
            val responseBody = readResponseBody(stream)
            val responseHeaders = urlConnection.headerFields.mapValues { it.value.joinToString(",") }
            return HttpResponse(statusCode, responseBody, responseHeaders)
        }


        private fun createConnection(
            url: String,
            method: String,
            headers: Map<String, String>,
            body: String?,
            connectionConfig: ConnectionConfig
        ): HttpURLConnection {
            val httpUrl = if (!url.startsWith("http")) "https://$url" else url
            val urlConnection = URL(httpUrl).openConnection() as HttpURLConnection

            urlConnection.requestMethod = method
            headers.forEach {
                urlConnection.setRequestProperty(it.key, it.value)
            }

            urlConnection.instanceFollowRedirects = connectionConfig.followRedirects
            urlConnection.connectTimeout = connectionConfig.connectTimeout
            urlConnection.readTimeout = connectionConfig.readTimeout

            if (body != null) {
                urlConnection.doOutput = true
                BufferedOutputStream(urlConnection.outputStream).use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            return urlConnection
        }

        private fun readResponseBody(stream: InputStream): String {
            val result = ByteArrayOutputStream()
            stream.use {
                val buffered = BufferedInputStream(stream)
                val buffer = ByteArray(1024)
                while (true) {
                    val length = buffered.read(buffer)
                    if (length < 0) break
                    result.write(buffer, 0, length)
                }
            }
            return result.toString("UTF-8")
        }
    }
}
