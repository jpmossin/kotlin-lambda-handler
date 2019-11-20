package com.jpmossin.eventbot.lambda

import java.net.HttpURLConnection.HTTP_BAD_REQUEST

/**
 * Use this class when throwing an exception in
 * Lambda code that should trigger a 400 response.
 */
class InvalidClientRequestException(clientErrorDescription: String, cause: Throwable? = null) :
    ApiException(HTTP_BAD_REQUEST, clientErrorDescription, cause) {

    override fun toString(): String {
        return "InvalidClientRequestException() ${super.toString()}"
    }
}
