plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":runner"))
    implementation(project(":player-alex"))
    implementation(project(":player-dominic"))

    implementation(libs.kotlindl.api)
    implementation(libs.kotlindl.tensorflow)
}
