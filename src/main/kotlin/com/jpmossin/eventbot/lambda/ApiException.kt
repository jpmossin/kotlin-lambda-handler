package com.jpmossin.eventbot.lambda

import java.lang.RuntimeException

/**
 * Use this class (or a subclass) whenever throwing an exception in Lambda
 * code that should be propagated as an error to the client.
 *
 * @param clientFriendlyDescription A description of the error that will be returned in the response.
 */
open class ApiException(val httpStatusCode: Int, val clientFriendlyDescription: String, cause: Throwable? = null) :
    RuntimeException(clientFriendlyDescription, cause) {

    override fun toString(): String {
        return "ApiException(httpStatusCode=$httpStatusCode, clientErrorDescription='$clientFriendlyDescription')"
    }
}
