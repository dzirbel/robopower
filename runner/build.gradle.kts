plugins {
    kotlin("jvm") version libs.versions.kotlin
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
