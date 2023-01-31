## Events
Veo-accounts receives external events using AMQP.

### client_change

#### CREATION

When the `client_change` message with the type `CREATION` is received, veo-accounts creates a Keycloak group
called `veo_client:{clientId}` and sets the group attributes `maxUsers` & `maxUnits` with the corresponding values from
the message content.

### DEACTIVATION

When receiving a client `DEACTIVATION`, the veo client group is tagged with the attribute `veo-accounts.deactivated` and
all its members are removed from the `veo-user` group, so they are no longer able to authenticate with
other veo REST APIs.

### ACTIVATION

`ACTIVATION` reverts the effects of `DEACTIVATION`, removing the `veo-accounts.deactivated` attribute from the veo client group and reassigning all its members to the `veo-user` group, so they can authenticate with other veo REST APIs again.

### DELETION
When receiving the client `DELETION`, veo-accounts deletes the veo client group and all its members from Keycloak.
