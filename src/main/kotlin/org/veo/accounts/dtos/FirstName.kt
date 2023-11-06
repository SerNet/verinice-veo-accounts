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
import org.veo.accounts.validate

private const val MIN_LENGTH = 1
private const val MAX_LENGTH = 256

@Schema(
    description = "User's given name",
    type = "string",
    minLength = MIN_LENGTH,
    maxLength = MAX_LENGTH,
    example = "Katie",
)
data class FirstName(
    @field:Size(min = MIN_LENGTH, max = MAX_LENGTH)
    @get:JsonValue
    val value: String,
) {
    init {
        validate()
    }
}
