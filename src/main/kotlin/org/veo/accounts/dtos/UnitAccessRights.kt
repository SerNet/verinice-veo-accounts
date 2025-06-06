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
import org.springframework.validation.annotation.Validated
import kotlin.collections.filterValues

@Schema(
    description =
        "Defines which units can be written and read. Keys are unit UUIDs, values are access modes.",
)
@Validated
data class UnitAccessRights(
    @get:JsonValue
    val value: Map<UnitId, Mode> = emptyMap(),
) {
    fun toAttributes(): Map<String, List<String>> =
        mapOf(
            "unitReadAccess" to value.keys.map(UnitId::toString),
            "unitWriteAccess" to value.filterValues { it == Mode.READ_WRITE }.keys.map(UnitId::toString),
        )

    enum class Mode {
        READ_ONLY,
        READ_WRITE,
    }

    companion object {
        fun byAttributes(attributes: Map<String, List<String>>): UnitAccessRights =
            UnitAccessRights(
                attributes["unitReadAccess"]
                    ?.associate {
                        UnitId(it) to
                            if (attributes["unitWriteAccess"]?.contains(it) == true) {
                                Mode.READ_WRITE
                            } else {
                                Mode.READ_ONLY
                            }
                    }
                    ?: emptyMap(),
            )
    }
}
