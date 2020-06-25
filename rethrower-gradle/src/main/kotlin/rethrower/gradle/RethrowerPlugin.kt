package rethrower.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class RethrowerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create<RethrowerExtension>(EXTENSION_NAME)
    }

    companion object {
        internal const val EXTENSION_NAME = "rethrower"
    }
}