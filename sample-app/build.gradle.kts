import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    kotlin("android")
    id("rethrower")
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")

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

}

rethrower {
    rethrowFolders = listOf("interactor", "repository")
}

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))

    implementation("com.github.ghvvl:rethrower:0.0.1")
    implementation("com.github.ghvvl:rethrower-annotations:0.0.1")
/*implementation(project(":rethrower-rx"))*/
}