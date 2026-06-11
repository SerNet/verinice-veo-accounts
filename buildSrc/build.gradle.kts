plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies{
    implementation("com.diffplug.spotless:spotless-lib-extra:4.6.2")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.7.0.202606012155-r")
}