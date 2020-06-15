package rethrower.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class RethrowerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create<RethrowerExtension>(EXTENSION_NAME)
        val plugins = project.plugins
        val extensions = project.extensions

        /*plugins.withId("org.jetbrains.kotlin.jvm") {
            val kotlin = extensions.getByType(KotlinJvmProjectExtension::class.java)
            kotlin.target.addRethrowDependency()
        }*/
    }

    /*private fun KotlinTarget.addRethrowDependency() {
        compilations.all { compilation ->
            compilation.dependencies {
                compileOnly(rethrowApi)
            }
        }
    }*/

    companion object {
        internal const val EXTENSION_NAME = "rethrower"
    }
}
//private const val rethrowApi = "com.jakewharton.confundus:confundus-api:$confundusVersion"