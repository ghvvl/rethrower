import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("kotlin")
    id("com.jfrog.bintray") version "1.8.5"
}

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    compileOnly(kotlin("compiler-embeddable", KotlinCompilerVersion.VERSION))

    implementation("com.github.ghvvl:rethrower:${Project.libraryVersion}")
    implementation("com.github.ghvvl:rethrower-annotations:${Project.libraryVersion}")
}

apply(from = "${project.rootDir}/publish.gradle.kts")