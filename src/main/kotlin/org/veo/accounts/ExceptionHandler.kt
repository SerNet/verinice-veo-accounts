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
    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentNotValidException::class)
    fun handle(exception: Exception): ResponseEntity<String> = handle(exception, BAD_REQUEST)

    @ExceptionHandler(AbstractMappedException::class)
    fun handle(ex: AbstractMappedException) = handle(ex, ex.status)

    private fun handle(exception: Exception, status: HttpStatus): ResponseEntity<String> {
        return ResponseEntity<String>(exception.message, status)
    }
}
