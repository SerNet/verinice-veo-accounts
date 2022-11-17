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
package org.veo.accounts.keycloak

import org.keycloak.admin.client.resource.RealmResource
import javax.ws.rs.NotFoundException

fun RealmResource.tryDeleteGroup(id: String) {
    try {
        groups().group(id).apply {
            members().forEach { tryDeleteAccount(it.id) }
            remove()
        }
    } catch (_: NotFoundException) {
    }
}

fun RealmResource.tryDeleteAccount(id: String) {
    try {
        users().delete(id)
    } catch (_: NotFoundException) {
    }
}
