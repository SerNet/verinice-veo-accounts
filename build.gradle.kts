import com.diffplug.spotless.FormatterStep
import com.fasterxml.jackson.core.util.DefaultIndenter.SYSTEM_LINEFEED_INSTANCE
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import org.cadixdev.gradle.licenser.header.HeaderFormatRegistry
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Calendar

plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"

    kotlin("jvm") version "1.7.20"
    kotlin("plugin.spring") version "1.7.20"

    id("com.diffplug.spotless") version "6.10.0"
    id("org.cadixdev.licenser") version "0.6.1"
    jacoco
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
}

group = "org.veo"
version = "0.16-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.keycloak:keycloak-admin-client:19.0.2")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.security:spring-security-test")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.11")
    implementation("io.mockk:mockk:1.13.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
    implementation("net.swiftzer.semver:semver:1.2.0")

    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")

    val kotestVersion = "5.4.2"
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-property-jvm:$kotestVersion")

    testImplementation("org.keycloak:keycloak-authz-client:19.0.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

extra["kotlin-coroutines.version"] = "1.6.0"

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

    inputs.property("veoAccountsBaseUrl") {
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
                "http.nonProxyHosts"
            ).contains(it)
        }
    )
}

spotless {
    format("misc") {
        target("**/*.md", "**/*.gitignore")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
    json {
        target("**/*.json")
        addStep(object : FormatterStep {
            override fun getName() = "format json"
            override fun format(rawUnix: String, file: File): String {
                val om = ObjectMapper()
                return om.writer()
                    .with(DefaultPrettyPrinter().apply { indentArraysWith(SYSTEM_LINEFEED_INSTANCE) })
                    .writeValueAsString(om.readValue(rawUnix, Map::class.java))
            }
        })
    }
}

license {
    header.set(resources.text.fromFile("templates/licenseHeader.txt"))
    newLine.set(false)
    skipExistingHeaders.set(true)
    exclude("**/*.properties")
    style(
        closureOf<HeaderFormatRegistry> {
            put("kt", "JAVADOC")
        }
    )
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
    ext["author"] = ProcessBuilder("git", "config", "user.name").start()
        .inputStream.bufferedReader().readText().trim()
}

springBoot {
    buildInfo {
        properties {
            if (getRootProject().hasProperty("ciBuildNumber")) {
                additional = mapOf(
                    "ci.buildnumber" to rootProject.properties["ciBuildNumber"],
                    "ci.jobname" to rootProject.properties["ciJobName"]
                )
            }
        }
    }
}

if (rootProject.hasProperty("ci")) {
    tasks.withType<Test> {
        // Don't let failing tests fail the build, let the junit step in the Jenkins pipeline decide what to do
        ignoreFailures = true
    }
}
