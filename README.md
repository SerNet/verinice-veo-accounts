# veo-accounts
Spring boot microservice for managing veo accounts.

## Introduction
In the veo application system, veo-accounts acts as a facade to the Keycloak REST API. It allows authorized end users to
manage accounts within their own veo client group.

For more information, see the [docs](doc/index.md)

## Build

### Build dependencies

* Java 17
    export JAVA_HOME=/path/to/jdk-17
    ./gradlew build [-x test]


For verification, we recommend `./gradlew build` as a `pre-commit` git hook.

## Runtime dependencies
* Java 17
* Keycloak server

## Config & Launch

### Configure keycloak
* setup keycloak values (`application.properties` > `veo.accounts.keycloak.*`).

### Run

The application can be started on the default port 8099, either with Gradle through the JDK. If Keycloak can only be
reached through a proxy, the proxy must be configured at startup.

Gradle

    ./gradlew bootRun

JDK with Proxy

    java -Dhttp.proxyHost=[PROXY_HOST] -Dhttp.proxyPort=[PORT] \
     -Dhttps.proxyHost=[PROXY_HOST] -Dhttps.proxyPort=[PORT] \
     -jar build/libs/veo-accounts-[VERSION].jar

## API docs
Launch and visit <http://localhost:8099/swagger-ui.html>

## Code format
Spotless is used for linting and license-gradle-plugin is used to apply license headers. The following task applies
spotless code format & adds missing license headers to new files:

    ./gradlew formatApply

The Kotlin lint configuration does not allow wildcard imports. Spotless cannot fix wildcard imports automatically, so
you should set up your IDE to avoid them.

## License

verinice.veo is released under [GNU AFFERO GENERAL PUBLIC LICENSE](https://www.gnu.org/licenses/agpl-3.0.en.html)
Version 3 (see [LICENSE.txt](./LICENSE.txt)) and uses third party libraries that are distributed under their own terms.
