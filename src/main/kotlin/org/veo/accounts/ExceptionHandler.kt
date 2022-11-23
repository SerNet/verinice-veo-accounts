/**
 * verinice.veo accounts
 * Copyright (C) 2022  Jonas Jordan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.veo.accounts

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.veo.accounts.exceptions.AbstractMappedException

@ControllerAdvice
class ExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(exception: HttpMessageNotReadableException): ResponseEntity<String> =
        handle(getParsingErrorMessage(exception), BAD_REQUEST)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(exception: MethodArgumentNotValidException): ResponseEntity<String> =
        handle(exception.message, BAD_REQUEST)

    @ExceptionHandler(AbstractMappedException::class)
    fun handle(ex: AbstractMappedException) = handle(ex.message, ex.status)

    private fun handle(message: String?, status: HttpStatus): ResponseEntity<String> {
        return ResponseEntity<String>(message, status)
    }

    private fun getParsingErrorMessage(ex: HttpMessageNotReadableException): String? = ex.cause
        .let { cause ->
            when (cause) {
                is MissingKotlinParameterException -> "${cause.parameter.name} must not be null"
                is ValueInstantiationException -> cause.cause?.message
                is MismatchedInputException ->
                    cause
                        .run { originalMessage?.replace(targetType.name, targetType.simpleName) }
                is JsonMappingException ->
                    cause.originalMessage
                is JsonParseException ->
                    cause
                        .location
                        .run { "Invalid request body: JSON syntax error at line $lineNr, column $columnNr" }
                else -> cause?.message
            }
        }
        ?: ex.message
}
