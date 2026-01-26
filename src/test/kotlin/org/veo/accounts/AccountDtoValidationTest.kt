/*
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

class AccountDtoValidationTest {
    @Test
    fun `valid account is ok`() {
        shouldNotThrowAny { createAccountDto() }
        shouldNotThrowAny { updateAccountDto() }
    }

    @Test
    fun `empty strings are not ok`() {
        shouldThrow<ValidationException> { createAccountDto(firstName = "") }
        shouldThrow<ValidationException> { createAccountDto(lastName = "") }
        shouldThrow<ValidationException> { createAccountDto(username = "") }

        shouldThrow<ValidationException> { updateAccountDto(firstName = "") }
        shouldThrow<ValidationException> { updateAccountDto(lastName = "") }
    }

    @Test
    fun `overly long strings are not ok`() {
        val longText = "u".repeat(300)

        shouldThrow<ValidationException> { createAccountDto(firstName = longText) }
        shouldThrow<ValidationException> { createAccountDto(lastName = longText) }
        shouldThrow<ValidationException> { createAccountDto(username = longText) }

        shouldThrow<ValidationException> { updateAccountDto(firstName = longText) }
        shouldThrow<ValidationException> { updateAccountDto(lastName = longText) }
    }

    @Test
    fun `invalid email address is not ok`() {
        shouldThrow<ValidationException> { createAccountDto(emailAddress = "verinice test") }
        shouldThrow<ValidationException> { createAccountDto(emailAddress = "verinice.test") }

        shouldThrow<ValidationException> { updateAccountDto(emailAddress = "verinice test") }
        shouldThrow<ValidationException> { updateAccountDto(emailAddress = "verinice.test") }
    }

    @Test
    fun `invalid language is not ok`() {
        shouldThrow<ValidationException> { createAccountDto(language = "e") }
        shouldThrow<ValidationException> { createAccountDto(language = "eng") }
        shouldThrow<ValidationException> { createAccountDto(language = "xx") }

        shouldThrow<ValidationException> { updateAccountDto(language = "e") }
        shouldThrow<ValidationException> { updateAccountDto(language = "eng") }
        shouldThrow<ValidationException> { updateAccountDto(language = "xx") }
    }
}
