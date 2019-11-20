package com.jpmossin.eventbot.lambda

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * API Gateway requires a response object of this structure
 */
data class ApiGatewayResponse(
    val statusCode: Int = 200,
    val body: String? = null,
    val headers: Map<String, String> = Collections.emptyMap()
) {

    @Suppress("unused")
    @JsonProperty("isBase64Encoded")
    fun isBase64Encoded():Boolean {
        return false
    }

    companion object {

        fun json(body: Any?, statusCode: Int = 200): ApiGatewayResponse {
            return ApiGatewayResponse(
                statusCode,
                LambdaCommon.objectMapper.writeValueAsString(body),
                mapOf("Content-Type" to "application/json")
            )
        }

        fun text(body: String, statusCode: Int = 200): ApiGatewayResponse {
            return ApiGatewayResponse(statusCode, body, mapOf("Content-Type" to "text/plain"))
        }

        fun error(cause: ApiException): ApiGatewayResponse {
            return error(cause.httpStatusCode, cause.clientFriendlyDescription)
        }

        fun error(httpStatusCode: Int, clientFriendlyDescription: String): ApiGatewayResponse {
            val errorData = mapOf("error" to clientFriendlyDescription)
            return json(errorData, httpStatusCode)
        }
    }
}
