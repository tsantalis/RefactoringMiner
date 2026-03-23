package com.hivemaps.api.campus.api

import com.hivemaps.api.campus.domain.NoRouteFoundException
import com.hivemaps.api.campus.domain.NodeNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<Any> {
        return ResponseEntity.badRequest().body(mapOf("error" to e.message))
    }

    @ExceptionHandler(NoRouteFoundException::class)
    fun handleNoRouteFound(e: NoRouteFoundException): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapOf("error" to e.message))
    }

    @ExceptionHandler(NodeNotFoundException::class)
    fun handleNoNodeFound(e: NodeNotFoundException): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<Any> {
        return ResponseEntity.internalServerError().body(mapOf("error" to "Something went wrong"))
    }
}