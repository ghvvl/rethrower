import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("kotlin")
    kotlin("kapt")
    `kotlin-dsl`
    id("java-gradle-plugin")
    id("com.jfrog.bintray") version "1.8.5"
}

gradlePlugin {
    plugins {
        register("rethrower") {
            id = "rethrower"
            displayName = "name"
            description = "description"
            implementationClass = "rethrower.gradle.RethrowerPlugin"
        }
    }
}

dependencies {
    compileOnly(kotlin("gradle-plugin-api", KotlinCompilerVersion.VERSION))
    compileOnly(kotlin("gradle-plugin", KotlinCompilerVersion.VERSION))
}

apply(from = "${project.rootDir}/publish.gradle.kts")