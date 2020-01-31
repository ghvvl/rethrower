import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("kotlin")
}

dependencies {

    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
}