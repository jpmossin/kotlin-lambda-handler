package com.jpmossin.eventbot.lambda

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Holds objects that can safely be shared globally as an optimization
 */
object LambdaCommon {

    val objectMapper = jacksonObjectMapper()
}
