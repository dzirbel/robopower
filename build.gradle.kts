import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// TODO assertions
// TODO detekt
// TODO jacoco and codecov
// TODO github actions

plugins {
    kotlin("jvm") version libs.versions.kotlin
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "16"
    }

    afterEvaluate {
        dependencies {
            testRuntimeOnly(libs.junit.engine)
            testImplementation(libs.junit.api)
            testImplementation(libs.junit.params)
        }

        tasks.test {
            useJUnitPlatform()
        }
    }
}
