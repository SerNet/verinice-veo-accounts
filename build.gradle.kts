import com.diffplug.spotless.FormatterStep
import com.fasterxml.jackson.core.util.DefaultIndenter.SYSTEM_LINEFEED_INSTANCE
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.TextReportRenderer
import com.github.jk1.license.task.ReportTask
import org.cadixdev.gradle.licenser.header.HeaderFormatRegistry
import org.eclipse.jgit.api.Git
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Calendar
import kotlin.text.Regex
import kotlin.text.RegexOption

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"

    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"

    id("com.diffplug.spotless") version "6.25.0"
    id("org.cadixdev.licenser") version "0.6.1"
    jacoco
    id("com.gorylenko.gradle-git-properties") version "2.4.2"
    id("com.github.jk1.dependency-license-report") version "2.7"
}

group = "org.veo"
version = "0.25.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.keycloak:keycloak-admin-client:23.0.4")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("net.swiftzer.semver:semver:2.0.0")
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")

    val kotestVersion = "5.8.1"
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-property-jvm:$kotestVersion")

    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.testcontainers:rabbitmq:1.19.7")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("org.keycloak:keycloak-authz-client:23.0.4")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

val licenseFile3rdParty = "LICENSE-3RD-PARTY.txt"
licenseReport {
    renderers =
        arrayOf(
            TextReportRenderer(licenseFile3rdParty),
        )
    projects = arrayOf(project)
    filters =
        arrayOf(
            LicenseBundleNormalizer(),
        )
}

tasks.withType<ReportTask> {
    outputs.apply {
        // work around for license report not being updated when the project's version number changes
        // https://github.com/jk1/Gradle-License-Report/issues/223
        upToDateWhen { false }
        cacheIf { false }
    }
    doLast {
        val dateLinePattern = Regex("^This report was generated at.+$", RegexOption.MULTILINE)
        val newLicenseText = file("${config.outputDir}/$licenseFile3rdParty").readText()
        val licenseFile = file(licenseFile3rdParty)
        if (licenseFile.readText().replace(dateLinePattern, "") != newLicenseText.replace(dateLinePattern, "")) {
            licenseFile.writeText(newLicenseText)
        }
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
        addStep(
            object : FormatterStep {
                override fun getName() = "format json"

                override fun format(
                    rawUnix: String,
                    file: File,
                ): String {
                    val om = ObjectMapper()
                    return om.writer()
                        .with(DefaultPrettyPrinter().apply { indentArraysWith(SYSTEM_LINEFEED_INSTANCE) })
                        .writeValueAsString(om.readValue(rawUnix, Map::class.java))
                }
            },
        )
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
