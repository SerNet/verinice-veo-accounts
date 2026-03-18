plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies{
    implementation("com.diffplug.spotless:spotless-lib-extra:4.5.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.6.0.202603022253-r")
}