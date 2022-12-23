import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// TODO assertions
// TODO jacoco and codecov
// TODO github actions

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.detekt)
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
            detektPlugins(libs.detekt.formatting)

            testRuntimeOnly(libs.junit.engine)
            testImplementation(libs.junit.api)
            testImplementation(libs.junit.params)
        }

        tasks.detekt.configure {
            // run detekt with type resolution
            dependsOn(tasks.detektMain)
        }

        tasks.test {
            useJUnitPlatform()
        }
    }
}
