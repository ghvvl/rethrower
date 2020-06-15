import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("kotlin")
}

version = "0.0.1"
group

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    compileOnly(kotlin("compiler-embeddable", KotlinCompilerVersion.VERSION))
    implementation(project(":rethrower-annotations"))
    implementation(project(":rethrower"))
}