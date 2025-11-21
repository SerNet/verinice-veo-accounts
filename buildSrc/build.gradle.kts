plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies{
    implementation("com.diffplug.spotless:spotless-lib-extra:4.1.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.4.0.202509020913-r")
}