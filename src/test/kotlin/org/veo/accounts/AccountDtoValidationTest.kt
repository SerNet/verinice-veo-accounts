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

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import javax.validation.ConstraintViolationException

class AccountDtoValidationTest {
    @Test
    fun `valid account is ok`() {
        shouldNotThrowAny { createAccountDto() }
        shouldNotThrowAny { updateAccountDto() }
    }

    @Test
    fun `empty strings are not ok`() {
        shouldThrow<ConstraintViolationException> { createAccountDto(firstName = "") }
        shouldThrow<ConstraintViolationException> { createAccountDto(lastName = "") }
        shouldThrow<ConstraintViolationException> { createAccountDto(username = "") }

        shouldThrow<ConstraintViolationException> { updateAccountDto(firstName = "") }
        shouldThrow<ConstraintViolationException> { updateAccountDto(lastName = "") }
    }

    @Test
    fun `overly long strings are not ok`() {
        val longText = "u".repeat(300)

        shouldThrow<ConstraintViolationException> { createAccountDto(firstName = longText) }
        shouldThrow<ConstraintViolationException> { createAccountDto(lastName = longText) }
        shouldThrow<ConstraintViolationException> { createAccountDto(username = longText) }

        shouldThrow<ConstraintViolationException> { updateAccountDto(firstName = longText) }
        shouldThrow<ConstraintViolationException> { updateAccountDto(lastName = longText) }
    }

    @Test
    fun `invalid email address is not ok`() {
        shouldThrow<ConstraintViolationException> { createAccountDto(emailAddress = "verinice test") }
        shouldThrow<ConstraintViolationException> { createAccountDto(emailAddress = "verinice.test") }

        shouldThrow<ConstraintViolationException> { updateAccountDto(emailAddress = "verinice test") }
        shouldThrow<ConstraintViolationException> { updateAccountDto(emailAddress = "verinice.test") }
    }
}
