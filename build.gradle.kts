// TODO jacoco and codecov

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.detekt)
}

allprojects {
    repositories {
        mavenCentral()
    }
}

val runtimeAssertions = findProperty("runtime.assertions")?.toString()?.toBoolean() == true

subprojects {
    if (runtimeAssertions) {
        tasks.withType<JavaExec>().configureEach {
            enableAssertions = true
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
    }
}
