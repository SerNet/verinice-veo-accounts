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
package org.veo.accounts

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.dtos.request.ClientRequestDto
import org.veo.accounts.dtos.response.ClientCreatedDto
import org.veo.accounts.keycloak.GroupService
import org.veo.accounts.messaging.MessageDispatcher
import org.veo.accounts.messaging.MessageDispatcher.ClientChange
import java.util.UUID

@RestController
@RequestMapping("/clients")
@SecurityRequirement(name = SECURITY_SCHEME_CLIENT_INIT_API_KEY)
class ClientController(
    private val groupService: GroupService,
    private val messageDispatcher: MessageDispatcher,
) {
    @Operation(
        description = "Create or update a veo client",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Client created",
            ),
            ApiResponse(
                responseCode = "204",
                description = "Client updated",
            ),
        ],
    )
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    fun createClient(
        @Valid
        @RequestBody
        dto: ClientRequestDto,
    ): ClientCreatedDto =
        dto
            .run {
                val id = VeoClientId(UUID.randomUUID())
                groupService.createClient(
                    id,
                    maxUnits,
                    maxUsers,
                )
                messageDispatcher.sendClientChangeMessage(
                    ClientChange(
                        ClientChange.Type.CREATION,
                        id,
                        name,
                        maxUnits,
                        maxUsers,
                        domainProducts,
                    ),
                )
                ClientCreatedDto(id)
            }

    @Operation(
        description = "Update a veo client",
        responses = [
            ApiResponse(
                responseCode = "204",
                description = "Client updated",
            ),
        ],
    )
    @PutMapping("{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateClient(
        @PathVariable clientId: VeoClientId,
        @Valid
        @RequestBody
        dto: ClientRequestDto,
    ) {
        dto
            .apply {
                groupService.updateClient(
                    clientId,
                    maxUnits,
                    maxUsers,
                )
                messageDispatcher.sendClientChangeMessage(
                    ClientChange(
                        ClientChange.Type.MODIFICATION,
                        clientId,
                        name,
                        maxUnits,
                        maxUsers,
                        domainProducts,
                    ),
                )
            }
    }

    @PostMapping("{clientId}/deactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivateClient(
        @PathVariable clientId: VeoClientId,
    ) {
        groupService.deactivateClient(clientId)
        messageDispatcher.sendClientChangeMessage(ClientChange(ClientChange.Type.DEACTIVATION, clientId))
    }

    @PostMapping("{clientId}/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun activateClient(
        @PathVariable clientId: VeoClientId,
    ) {
        groupService.activateClient(clientId)
        messageDispatcher.sendClientChangeMessage(ClientChange(ClientChange.Type.ACTIVATION, clientId))
    }

    @DeleteMapping("{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteClient(
        @PathVariable clientId: VeoClientId,
    ) {
        groupService.deleteClient(clientId)
        messageDispatcher.sendClientChangeMessage(ClientChange(ClientChange.Type.DELETION, clientId))
    }
}
