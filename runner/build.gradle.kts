plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.detekt)
    jacoco
    application
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":player-dominic"))
    implementation(project(":player-matthew"))
}

application {
    mainClass.set("com.dzirbel.robopower.MainKt")
}
