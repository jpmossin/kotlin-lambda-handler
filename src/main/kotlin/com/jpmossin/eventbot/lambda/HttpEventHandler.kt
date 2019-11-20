package com.jpmossin.eventbot.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import kotlin.math.log

/**
 * Handles event deserialization and response serialization, making the
 * common task of processing an http event to an api gateway response simpler.
 */
abstract class HttpEventHandler : RequestStreamHandler {

    private val mapper = LambdaCommon.objectMapper
    private val logger = LogManager.getLogger(javaClass)

    final override fun handleRequest(input: InputStream, output: OutputStream, context: Context?) {
        val response = try {
            val event = mapper.readValue<HttpEvent>(input)
            handleHttpEvent(event)
        } catch (ex: ApiException) {
            logger.warn("Error handling http event: $ex", ex)
            ApiGatewayResponse.error(ex)
        } catch (ex: Exception) {
            logger.error("Unhandled exception", ex)
            throw ex
        }

        logger.info("response status: ${response.statusCode}")
        mapper.writeValue(output, response)
    }

    abstract fun handleHttpEvent(event: HttpEvent): ApiGatewayResponse

}
