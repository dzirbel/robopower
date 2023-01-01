plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":player-alex"))
    implementation(project(":player-dominic"))
    implementation(project(":player-matthew"))
}

tasks.register<JavaExec>("run") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.dzirbel.robopower.MainKt")
    workingDir = rootProject.projectDir
}
