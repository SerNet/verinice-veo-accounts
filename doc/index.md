# veo-accounts

## Documentation

Veo-accounts is a REST API that can be used by account managers to retrieve, create, modify, and delete veo user accounts within their own veo client. In the veo application ecosystem, it acts as a facade to the Keycloak admin API and is responsible for managing users and groups in Keycloak.

For exhaustive API documentation, launch veo-accounts and visit `/swagger-ui.html`.

### Clients
Each veo client is represented as a Keycloak group with the group name schema "veo_client:{uuid}". Veo clients are not to be confused with [Keycloak clients](https://www.keycloak.org/docs/latest/server_admin/#core-concepts-and-terms). Veo-accounts creates, updates, and deletes veo client groups in Keycloak when the corresponding [external events](./events.md) are received.

### Client initialization

To enable account management in a [newly created](./events.md#creation) veo client, an initial user must be created (otherwise there would be no users in the group who could authenticate with the veo-accounts API). For this purpose, the API features a dedicated endpoint `POST /initial`, which requires a secret API key (as opposed to the JWT that is required for other endpoints). The initial user is created with [account management privileges](#authorization) and will receive an email with a password reset link.

### Account management
After defining a password, the initial user can authenticate with veo-accounts using a JWT and create additional accounts in their own veo client group. `GET`, `POST`, `PUT` & `DELETE` endpoints are available for managing accounts. For usage details, see the API documentation and [Restrictions](#restrictions).

### Authentication
Users authenticate with veo-accounts using a JWT for the public Keycloak authentication client (e.g. "veo-prod" or
"veo-development-client").

To perform the actual account read/write operations on the Keycloak REST API, veo-accounts itself authenticates using
the confidential Keycloak client "veo-accounts" and the respective client secret.

### Authorization
Operations on veo-accounts are governed by the following Keycloak roles:
* `account:read`
* `account:create`
* `account:delete`
* `account:update`

These roles are currently only granted to a veo client's initial account that is created in a brand-new veo client.

### Client lifecycle
Veo-accounts is responsible for managing veo client groups in Keycloak and listens to client lifecycle events via AMQP. See [Events](./events.md).

### Restrictions
The following rules are imposed on account management by veo-accounts:
* accounts cannot be assigned to any veo client group other than the authenticated user's veo client group
* accounts cannot be granted account management roles (`account:*`)
* once an account has been created, the username cannot be changed
* accounts can only be created or updated with an email address, first name & last name
* no self-management: the authenticated user cannot view, update or delete their own account

### Mailing
Veo-accounts makes keycloak send an email with a link for required actions (email verification and/or password update)
when:
* An account is created
* An account is updated with a new email address
