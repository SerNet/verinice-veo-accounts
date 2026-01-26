plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies{
    implementation("com.diffplug.spotless:spotless-lib-extra:4.2.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.5.0.202512021534-r")
}