/*
 * verinice.veo accounts
 * Copyright (C) 2022  Anton Jacobsson
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
import java.net.URI

private const val MIN_LENGTH = 1
private const val MAX_LENGTH = 2000

@Schema(
    description = "A resource's URI",
    type = "string",
    minLength = MIN_LENGTH,
    maxLength = MAX_LENGTH,
    example = "https://<api.example.org>/veo/accounts/6d071be3-ca56-4c2a-8fba-1a80596e3403",
)
data class Link(
    @get:JsonValue
    val value: URI,
) {
    init {
        require(value.toString().length in MIN_LENGTH until MAX_LENGTH)
    }
}
