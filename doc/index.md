# veo-accounts

## Documentation
Organizations using VEO are supposed to have one main user that is created by the shop or by an administrator. This
user may create additional users but only with certain restrictions. Keycloak accounts for end users are not
authorized to read or write other accounts directly on Keycloak. They can only manage other accounts within their own
veo client group by using the veo-accounts REST API.

Veo-accounts provides endpoints for reading, creating, updating and deleting Keycloak accounts that belong to the same
veo client group as the authenticated user. Accounts are assigned to veo clients using a Keycloak group mapping with the
group name schema "veo_client:{uuid}". Veo clients are not to be confused with
[Keycloak clients](https://www.keycloak.org/docs/latest/server_admin/#core-concepts-and-terms).

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

These roles are currently only granted to a veo client's main account that is created along with the veo client itself.

### Restrictions
The following rules are imposed on account management by veo-accounts:
* accounts cannot be assigned to any veo client group other than the authenticated user's veo client group
* accounts cannot be granted account management roles (`account:*)
* once an account has been created, the username cannot be changed
* accounts can only be created or updated with an email address
* no self-destruction: the authenticated user cannot delete their own account
