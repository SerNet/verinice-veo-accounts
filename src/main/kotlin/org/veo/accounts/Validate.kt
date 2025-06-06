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

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation

private val validator = Validation.buildDefaultValidatorFactory().validator

/** Performs JSR-380 [Validation] on target */
fun Any.validate() =
    validator
        .validate(this)
        .let {
            if (it.isNotEmpty()) {
                throw ValidationException(it)
            }
        }

class ValidationException(
    message: String,
) : Exception(message) {
    constructor(violations: Collection<ConstraintViolation<Any>>) : this(
        violations.joinToString("; ") {
            "Invalid ${it.rootBeanClass.simpleName}: ${it.message}"
        },
    )
}
