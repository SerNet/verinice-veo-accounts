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
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.UUID

@Schema(
    description = "Generated ID for an account. Not to be confused with username.",
    type = "text",
    example = "2505d684-5f19-4615-9c9b-a24eeea96ea0",
)
data class AccountId(
    @get:JsonValue
    val value: UUID,
) {
    constructor(str: String) : this(UUID.fromString(str))

    override fun toString() = value.toString()
}

@Component
private class AccountConverter : Converter<String, AccountId> {
    override fun convert(source: String): AccountId? = AccountId(source)
}
