/**
 * verinice.veo accounts
 * Copyright (C) 2025  Jonas Jordan
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
    description = "UUID for a unit",
    type = "text",
    example = "cf0c0e91-731c-46c8-9324-9ae3a8074492",
)
data class UnitId(
    @get:JsonValue
    val value: UUID,
) {
    constructor(str: String) : this(UUID.fromString(str))

    override fun toString() = value.toString()
}

@Component
private class UnitIdConverter : Converter<String, UnitId> {
    override fun convert(source: String): UnitId? = UnitId(source)
}
