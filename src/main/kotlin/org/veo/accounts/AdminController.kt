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
package org.veo.accounts

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.veo.accounts.keycloak.LicenseService

@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = SECURITY_SCHEME_OAUTH)
class AdminController(
    private val licenseService: LicenseService,
) {
    @Operation(description = "Store a license key")
    @PutMapping("license", consumes = ["application/pkcs7-mime"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun saveLicense(
        @RequestBody licenseString: String,
    ): ResponseEntity<String> {
        licenseService.saveLicense(licenseString)
        return ResponseEntity.noContent().build()
    }
}
