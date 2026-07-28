plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies{
    implementation("com.diffplug.spotless:spotless-lib-extra:4.9.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.7.1.202607240634-r")
}