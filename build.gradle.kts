import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// TODO jacoco and codecov

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.detekt)
    jacoco
}

allprojects {
    repositories {
        mavenCentral()
    }
}

val runtimeAssertions = findProperty("runtime.assertions")?.toString()?.toBoolean() == true

subprojects {
    configurations.all {
        resolutionStrategy {
            failOnNonReproducibleResolution()
        }
    }

    // fail compilation tasks for compiler warnings
    tasks.withType<KotlinCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }

    // enable assertions for execution tasks based on the project property
    if (runtimeAssertions) {
        tasks.withType<JavaExec> {
            enableAssertions = true
        }
    }

    // improve logging of test results
    tasks.withType<Test> {
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            csv.required.set(false)
        }
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

        jacoco {
            toolVersion = libs.versions.jacoco.get()
        }
    }
}
