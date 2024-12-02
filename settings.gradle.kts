import org.gradle.caching.http.HttpBuildCache

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "veo-accounts"

val isCiServer = System.getenv().containsKey("CI")

buildCache {
    local {
        isEnabled = !isCiServer
    }
    System.getenv("GRADLE_REMOTE_BUILD_CACHE_URL")?.let { url ->
        remote<HttpBuildCache> {
            this.url = uri(url)
            isPush = isCiServer
            isAllowUntrustedServer = isCiServer
        }
    }
}
