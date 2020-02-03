import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("kotlin")
    kotlin("kapt")
}

dependencies {

    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation(project(":rethrower-annotations"))
    implementation(project(":rethrower"))
}