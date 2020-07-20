buildscript {
    repositories {
        google()
        jcenter()
        maven { setUrl("https://dl.bintray.com/ghvvl/rethrower/") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Libraries.kotlinVersion}")
        classpath("com.github.ghvvl:rethrower-gradle:0.0.1")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://dl.bintray.com/ghvvl/rethrower/") }
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete(rootProject.buildDir)
    }
}