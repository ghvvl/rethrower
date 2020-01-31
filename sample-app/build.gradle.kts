import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.2")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        applicationId = "ru.vvl.rethrower.test"
        minSdkVersion(22)
        targetSdkVersion(29)
        versionCode = 1
        versionName = ("0.0.1")
    }

    sourceSets {
        getByName("main") { java.srcDirs("src/main/kotlin") }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    kapt {
        arguments {
            arg("generate_rethrow", "interactor", "repository")
        }
    }

}

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))

    kapt(project(":rethrower"))
    implementation(project(":rethrower-annotations"))
/*implementation(project(":rethrower-rx"))*/
}