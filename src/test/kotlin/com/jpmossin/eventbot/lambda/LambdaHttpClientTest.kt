package com.jpmossin.eventbot.lambda

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.jpmossin.eventbot.lambda.LambdaHttpClient.ConnectionConfig
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.net.SocketTimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LambdaHttpClientTest {

    @get:Rule
    val wireMockRule: WireMockRule = WireMockRule()

    private val httpClient = LambdaHttpClient.create()

    @Test
    fun testSendManySequentialGetRequests() {
        stubFor(
            get(urlEqualTo("/"))
                .willReturn(
                    ok("Hello")
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                )
        )

        (1..50).forEach { _ ->
            val response = httpClient.get("http://localhost:8080")
            assertEquals(200, response.statusCode)
            assertEquals("Hello", response.body)
        }
    }

    @Test
    fun testParseResponseJson() {
        stubFor(
            get(urlEqualTo("/json"))
                .willReturn(
                    ok("{\"test\": 123}")
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                )
        )
        val response = httpClient.get("http://localhost:8080/json")
        val parsed = response.bodyObject<Map<String, Int>>()
        assertEquals(mapOf("test" to 123), parsed)
    }

    @Test
    fun testGetWithErrorResponse() {
        listOf(400, 404, 500, 503).forEach { status ->
            stubFor(
                get(urlEqualTo("/json"))
                    .willReturn(
                        ok("{\"error\": \"something\"}")
                            .withStatus(status)
                            .withHeader("Content-Type", "text/plain")
                    )
            )
            val response = httpClient.get("http://localhost:8080/json")
            val errorData = response.bodyObject<Map<String, String>>()
            assertEquals(status, response.statusCode)
            assertEquals(setOf("error"), errorData.keys)
        }
    }

    @Test
    fun testPostRequestWithBodyAndHeaders() {
        stubFor(
            post(urlEqualTo("/post"))
                .withRequestBody(equalTo("some_msg"))
                .withHeader("content-type", equalTo("text/plain"))
                .willReturn(
                    ok("created")
                        .withStatus(201)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("custom-header", "hello")
                )
        )

        val response = httpClient.post(
            url = "http://localhost:8080/post",
            headers = mapOf("content-type" to "text/plain"),
            body = "some_msg"
        )

        assertEquals(201, response.statusCode)
        assertEquals("created", response.body)
        assertTrue(response.headers.containsKey("custom-header"))
    }

    @Test(expected = SocketTimeoutException::class)
    fun testRequestTimesOutIfServerDoesNotRespondInTime() {
        stubFor(
            get(urlEqualTo("/delayed")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withFixedDelay(100)
            )
        )
        httpClient.get("http://localhost:8080/delayed", connectionConfig = ConnectionConfig(readTimeout = 50))
    }

    @Test
    fun testSimpleDeleteRequest() {
        testSimpleRequest("DELETE", delete("/"))
    }

    @Test
    fun testSimplePUTRequest() {
        testSimpleRequest("PUT", put("/"))
    }

    private fun testSimpleRequest(method: String, mappingBuilder: MappingBuilder) {
        stubFor(
            mappingBuilder
                .willReturn(
                    ok("hello")
                        .withStatus(201)
                        .withHeader("Content-Type", "text/plain")
                )
        )

        val response = httpClient.request(method, "http://localhost:8080/")
        assertEquals(201, response.statusCode)
        assertEquals("hello", response.body)
    }

    @Ignore
    @Test
    fun testSomeRealRequests() {
        val someUrls = listOf("http://www.google.com", "https://www.google.com", "www.google.com")
        for (i in 1..5) {
            someUrls.forEach {
                val response = httpClient.get(it)
                assertEquals(200, response.statusCode, "200 for $it")
            }
        }
    }
}
