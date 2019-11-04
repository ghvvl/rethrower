import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("kotlin")
    kotlin("kapt")
}

dependencies {

    compileOnly(Libraries.autocommon)
    compileOnly(Libraries.autoservice)
    compileOnly(Libraries.gradleIncapHelperAnnotations)

    kapt(Libraries.gradleIncapHelperProcessor)
    kapt(Libraries.autoservice)

    implementation(kotlin("stdlib-jdk7", KotlinCompilerVersion.VERSION))
}