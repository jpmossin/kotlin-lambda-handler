package com.jpmossin.eventbot.lambda

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.test.assertEquals


class HttpEventHandlerTest {

    @Test
    fun testReturnedApiGatewayResponseIsSerialized() {
        val handler = object : HttpEventHandler() {
            override fun handleHttpEvent(event: HttpEvent): ApiGatewayResponse {
                val body = event.bodyObject<Map<String, String>>()
                return ApiGatewayResponse.json(body)
            }
        }
        val resultStream = ByteArrayOutputStream()
        handler.handleRequest(getEventInputStream(), resultStream, null)

        val response = jacksonObjectMapper().readValue<Map<String, Any>>(resultStream.toByteArray())
        val expectedResp = mapOf(
            "statusCode" to 200,
            "body" to "{\"foo\":\"bar\"}",
            "headers" to mapOf("Content-Type" to "application/json"),
            "isBase64Encoded" to false
        )
        assertEquals(expectedResp, response)
    }

    @Test
    fun testLambdaEventJsonIsParsedAndPassedToHandler() {
        var lastEvent: HttpEvent? = null
        val lastEventRecorder = object : HttpEventHandler() {
            override fun handleHttpEvent(event: HttpEvent): ApiGatewayResponse {
                lastEvent = event
                return ApiGatewayResponse(200)
            }
        }

        lastEventRecorder.handleRequest(getEventInputStream(), ByteArrayOutputStream(), null)
        val received = lastEvent!!
        assertEquals("/debug", received.path)
        assertEquals("GET", received.httpMethod)
        assertEquals(mapOf("x" to "y"), received.queryStringParameters)
    }

    @Test
    fun testApiExceptionInHandlerIsReturnedCorrectly() {
        val errorMsg = "some error msg"
        val throwHandler = object : HttpEventHandler() {
            override fun handleHttpEvent(event: HttpEvent): ApiGatewayResponse {
                throw InvalidClientRequestException(errorMsg)
            }
        }
        val resultStream = ByteArrayOutputStream()
        throwHandler.handleRequest(getEventInputStream(), resultStream, null)

        val response = jacksonObjectMapper().readValue<Map<String, Any>>(resultStream.toByteArray())
        val expectedResp = mapOf(
            "statusCode" to 400,
            "body" to "{\"error\":\"${errorMsg}\"}",
            "headers" to mapOf("Content-Type" to "application/json"),
            "isBase64Encoded" to false
        )
        assertEquals(expectedResp, response)
    }


    private fun getEventInputStream(): InputStream {
        val initialFile = File("src/test/resources/example-http-event.json")
        return FileInputStream(initialFile)
    }

}
