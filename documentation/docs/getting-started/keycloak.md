The Keycloak service integration adds an authentication layer to UCE, providing login, user, and role management systems. This integration is still a work in progress—while not all features are fully implemented, the setup and core functionality already work reliably.

<hr/>

For this service, the setup process is the same for both users and developers, as it relies on Docker images.

## Setup

The following parameters in the [`.env` file](./webportal.md) must be set correctly:

```ini
KC_REALM_IMPORT_PATH=./auth/uce-realm.json
KC_ADMIN_USERNAME=admin
KC_ADMIN_PW=admin
```

where `KC_REALM_IMPORT_PATH` is the local path to your realm configuration (*essentially the configuration for your personal Keycloak instance which you can find the UCE source*), and `KC_ADMIN_USERNAME` and `KC_ADMIN_PW` define the default admin credentials required for login.

If you modify any parameters in the default `uce-realm.json`, you must also update the `common.conf` and `common-release.conf` files accordingly. However, it is not recommended to do so. Instead, use the default configuration and make adjustments through the web UI after startup.

Afterwards, simply start the `uce-keycloak-auth` container:

```
docker-compose up --build uce-keycloak-auth
```

Keycloak will start at `http://localhost:8080`, where you can access the admin web UI. To log in, use the `KC_ADMIN_USERNAME` and `KC_ADMIN_PW` credentials defined in the `.env` file. In the admin cockpit, you will find a realm named *uce*, which is used by UCE.

!!! warning "Changes"
    If you modify any of the default configurations-including the realm name, the client name (which is *uce-web*), or the client secret-you must update these parameters accordingly in the `common.conf` files within the UCE source.

**To enable authentication in UCE**, set the `authentication.isActivated` flag to `true` in the [UCE configuration](./configuration.md). Once enabled, a login button will appear in UCE, and certain features-such as the RAGBot-will only be accessible to authenticated users.

!!! important "Exposing the Port"
    The Keycloak service must be exposed outside the Docker Compose network, as UCE relies on public URL callbacks. Additionally, the port needs to be exposed to allow access to the admin UI.
