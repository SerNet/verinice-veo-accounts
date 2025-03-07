import org.cadixdev.gradle.licenser.header.HeaderFormatRegistry
import org.eclipse.jgit.api.Git
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Calendar

plugins {
    id("org.springframework.boot") version "3.4.3"

    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"

    id("com.diffplug.spotless") version "7.0.2"
    id("org.cadixdev.licenser") version "0.6.1"
    jacoco
    id("com.gorylenko.gradle-git-properties") version "2.5.0"
}

group = "org.veo"
version = "0.35.0-SNAPSHOT"

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
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("net.swiftzer.semver:semver:2.0.0")
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    runtimeOnly("ch.qos.logback.contrib:logback-jackson:0.1.5")

    val kotestVersion = "5.9.1"
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-property-jvm:$kotestVersion")

    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.testcontainers:rabbitmq:1.20.6")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("org.keycloak:keycloak-authz-client:26.0.4")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    if (name == "test") {
        filter {
            excludeTestsMatching("org.veo.accounts.rest.*")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        allWarningsAsErrors = true
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.register("formatApply") {
    dependsOn("spotlessApply")
    dependsOn("licenseFormat")
}

tasks.register("restTest", Test::class.java) {
    description = "Runs REST API integration tests"
    group = "verification"

    shouldRunAfter("test")

    filter {
        includeTestsMatching("org.veo.accounts.rest.*")
    }

    inputs
        .property("veoAccountsBaseUrl") {
            System.getenv("VEO_ACCOUNTS_RESTTEST_BASEURL")
        }.optional(true)

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
}

spotless {
    format("misc") {
        target("**/*.md", "**/*.gitignore", "**/*.properties")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
        replaceRegex("Excessive line breaks", "\n{3,}", "\n\n")
    }
    kotlin {
        ktlint()
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
        indentWithSpaces()
        endWithNewline()
    }
}

license {
    header.set(resources.text.fromFile("templates/licenseHeader.txt"))
    newLine.set(false)
    skipExistingHeaders.set(true)
    exclude("**/*.properties", "**/*.xml")
    style(
        closureOf<HeaderFormatRegistry> {
            put("kt", "JAVADOC")
        },
    )

    ext["author"] =
        Git.open(project.rootDir).use {
            it.getRepository().getConfig().getString("user", null, "name") ?: "<name>"
        }
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
}

springBoot {
    buildInfo {
        properties {
            if (getRootProject().hasProperty("ciBuildNumber")) {
                additional.set(
                    mapOf(
                        "ci.buildnumber" to rootProject.properties["ciBuildNumber"],
                        "ci.jobname" to rootProject.properties["ciJobName"],
                    ),
                )
            }
        }
    }
}
