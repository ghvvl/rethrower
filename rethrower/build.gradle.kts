import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("kotlin")
    kotlin("kapt")
}

dependencies {

    compileOnly(Libraries.autocommon)

    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation(project(":rethrower-annotations"))
}