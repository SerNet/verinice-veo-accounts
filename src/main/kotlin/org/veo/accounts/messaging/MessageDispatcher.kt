/*
 * verinice.veo accounts
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.accounts.messaging

import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.veo.accounts.dtos.DomainProducts
import org.veo.accounts.dtos.VeoClientId
import tools.jackson.module.kotlin.jacksonObjectMapper

private val om = jacksonObjectMapper()

@Component
class MessageDispatcher(
    @Value("\${veo.accounts.rabbitmq.exchanges.veo-subscriptions}")
    private val exchange: String,
    @Value("\${veo.accounts.rabbitmq.routing_key_prefix}")
    private val subscriptionRoutingKeyPrefix: String,
    connectionFactory: ConnectionFactory,
) {
    // TODO #5046 use JacksonJsonMessageConverter globally as MessageConverter @Bean, replace this handwired RabbitTemplate with the autowired one
    private val rabbitTemplate: RabbitTemplate =
        RabbitTemplate(connectionFactory).apply {
            messageConverter = JacksonJsonMessageConverter()
        }

    fun sendClientChangeMessage(content: ClientChange) {
        send(
            "${subscriptionRoutingKeyPrefix}client_change",
            om.writeValueAsString(content),
        )
    }

    private fun send(
        routingKey: String,
        content: String,
    ) {
        rabbitTemplate.convertAndSend(
            exchange,
            routingKey,
            mapOf("content" to content),
        )
    }

    data class ClientChange(
        val type: Type,
        val clientId: VeoClientId,
        val name: String? = null,
        val maxUnits: Int? = null,
        val maxUsers: Int? = null,
        val domainProducts: DomainProducts? = null,
    ) {
        val eventType = "client_change"

        @Deprecated("#5046")
        val source = "veo-accounts"

        enum class Type {
            CREATION,
            MODIFICATION,
            DEACTIVATION,
            ACTIVATION,
            DELETION,
        }
    }
}
