plugins {
    id("org.springframework.boot") version "3.5.7"

    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"

    id("com.diffplug.spotless") version "8.0.0"
    jacoco
    id("com.gorylenko.gradle-git-properties") version "2.5.3"
}

group = "org.veo"
version = "0.47.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.keycloak:keycloak-admin-client:26.0.4")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.82")

    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    runtimeOnly("ch.qos.logback.contrib:logback-jackson:0.1.5")
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        configureEach {
            dependencies {
                implementation("io.kotest:kotest-assertions-core-jvm:6.0.4")
                implementation("io.kotest:kotest-runner-junit5-jvm:6.0.4")
                implementation("io.kotest:kotest-assertions-core-jvm:6.0.4")
                implementation("io.kotest:kotest-property-jvm:6.0.4")
            }
        }
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation("io.mockk:mockk:1.14.6")
            }
        }
        register<JvmTestSuite>("restTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation("org.testcontainers:rabbitmq:1.21.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                implementation("org.keycloak:keycloak-authz-client:26.0.4")
                implementation("org.springframework.boot:spring-boot-starter-test") {
                    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
                }
            }
            configurations {
                named(sources.implementationConfigurationName) {
                    extendsFrom(getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME))
                }
            }
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                        description = "Runs REST API integration tests"
                        group = "verification"
                        inputs.property("veoAccountsBaseUrl") { System.getenv("VEO_ACCOUNTS_RESTTEST_BASEURL") }.optional(true)
                        systemProperties(
                            System.getProperties().mapKeys { it.key as String }.filterKeys {
                                listOf(
                                    "http.proxyHost",
                                    "http.proxyPort",
                                    "http.nonProxyHosts",
                                    "https.proxyHost",
                                    "https.proxyPort",
                                    "http.nonProxyHosts",
                                ).contains(it)
                            },
                        )
                        // Enable Origin header for CORS tests
                        systemProperty("sun.net.http.allowRestrictedHeaders", "true")
                        systemProperty("user.language", "en")
                    }
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs = listOf("-Xjsr305=strict ", "-Xannotation-default-target=param-property")
    }
}

spotless {
    format("misc") {
        target("**/*.md", "**/*.gitignore", "**/*.properties")
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
        replaceRegex("Excessive line breaks", "\n{3,}", "\n\n")
    }
    kotlin {
        ktlint()
        addStep(
            org.veo.accounts.LicenseHeaderStep
                .create(project.rootDir),
        )
    }
    kotlinGradle {
        ktlint()
    }
    json {
        target("**/*.json")
        gson().indentWithSpaces(2)
        endWithNewline()
    }
    yaml {
        target(".gitlab-ci.yml")
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
}

springBoot {
    buildInfo {
        properties {
            if (getRootProject().hasProperty("ciBuildNumber")) {
                additional.set(
                    mapOf(
                        "ci.buildnumber" to rootProject.properties["ciBuildNumber"] as String,
                        "ci.jobname" to rootProject.properties["ciJobName"] as String,
                    ),
                )
            }
        }
    }
}
