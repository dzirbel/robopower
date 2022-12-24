plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.detekt)
    jacoco
}

dependencies {
    api(libs.coroutines)
}
