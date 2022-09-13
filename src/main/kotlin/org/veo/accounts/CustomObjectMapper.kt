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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule

class CustomObjectMapper : ObjectMapper() {
    init {
        findAndRegisterModules()
        addStringConverter(AccountId::class.java) { AccountId(it) }
        addStringConverter(EmailAddress::class.java) { EmailAddress(it) }
        addStringConverter(Username::class.java) { Username(it) }
    }

    private fun <T> addStringConverter(clazz: Class<T>, parse: (String) -> T) = registerModule(
        SimpleModule(clazz.name).apply {
            addSerializer(
                clazz,
                object : JsonSerializer<T>() {
                    override fun serialize(value: T, gen: JsonGenerator?, serializers: SerializerProvider?) {
                        gen?.writeString(value?.toString())
                    }
                }
            )
            addDeserializer(
                clazz,
                object : JsonDeserializer<T>() {
                    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): T? = p?.text?.let(parse)
                }
            )
        }
    )
}
