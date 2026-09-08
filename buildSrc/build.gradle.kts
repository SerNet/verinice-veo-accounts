plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies{
    implementation("com.diffplug.spotless:spotless-lib-extra:4.10.2")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.8.0.202609011348-r")
}