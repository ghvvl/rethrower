import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("kotlin")
    id("com.jfrog.bintray") version "1.8.5"
}

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
}

apply(from = "${project.rootDir}/publish.gradle.kts")