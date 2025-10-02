/**
 * verinice.veo accounts
 * Copyright (C) 2025  Jochen Kemnade
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
package org.veo.accounts.systemmessages

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class SystemMessageService(
    @Value("\${veo.accounts.veo.apiurl}")
    private val veoApiUrl: String,
    @Value("\${veo.accounts.veo.systemMessagesApiKey}")
    private val apiKey: String,
    private val restTemplate: RestTemplate,
) {
    fun setLicenseMessages(messages: Set<LicenseMessage>) {
        getMessages(setOf(LICENSE_TAG)).let { existingMessages ->
            existingMessages.filter { msg -> !messages.any { it.matches(msg) } }.forEach {
                deleteMessage(it.id as String)
            }
            messages.filter { msg -> !existingMessages.any { msg.matches(it) } }.forEach {
                addMessage(it.toSystemMessage())
            }
        }
    }

    private fun getMessages(tags: Set<String>): List<SystemMessage> =
        restTemplate
            .exchange(
                "$veoApiUrl/messages",
                HttpMethod.GET,
                HttpEntity<Unit>(createHeaders()),
                Array<SystemMessage>::class.java,
            ).let {
                it.body!!.asList().filter { msg -> msg.tags.any { t -> tags.contains(t) } }
            }

    private fun addMessage(message: SystemMessage) {
        restTemplate.exchange(
            "$veoApiUrl/admin/messages",
            HttpMethod.POST,
            HttpEntity(message, createHeaders()),
            Void::class.java,
        )
    }

    private fun deleteMessage(id: String) {
        restTemplate.exchange(
            "$veoApiUrl/admin/messages/$id",
            HttpMethod.DELETE,
            HttpEntity<Unit>(createHeaders()),
            Void::class.java,
        )
    }

    fun createHeaders(): HttpHeaders =
        object : HttpHeaders() {
            init {
                set("x-api-key", apiKey)
            }
        }
}
