/*
 * verinice.veo accounts
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.accounts.dtos

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.veo.accounts.ValidationException
import org.veo.accounts.validate
import java.util.Locale

private const val MIN_LENGTH = 2
private const val MAX_LENGTH = 2

@Schema(
    description = "User's preferred language (ISO 639-1 code)",
    type = "string",
    minLength = MIN_LENGTH,
    maxLength = MAX_LENGTH,
    example = "de",
)
class Language(
    @field:Size(min = MIN_LENGTH, max = MAX_LENGTH)
    @get:JsonValue
    val value: String,
) {
    init {
        validate()
        if (!Locale.getISOLanguages().contains(value)) {
            throw ValidationException("'$value' is not a valid ISO 639-1 language code")
        }
    }
}
