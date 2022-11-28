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
package org.veo.accounts.dtos

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.veo.accounts.validate

private const val minLength = 1
private const val maxLength = 256

@Schema(
    description = "Unique user account identifier, but not be confused with the generated account ID. Once chosen, usernames cannot be changed.",
    type = "string",
    minLength = minLength,
    maxLength = maxLength,
    example = "ksmith"
)
@Validated
data class Username(
    @field:Size(min = minLength, max = maxLength)
    @get:JsonValue
    val value: String
) {
    init {
        validate()
    }
}
