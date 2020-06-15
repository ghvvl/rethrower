package rethrower.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class RethrowerGradleSubPlugin : KotlinGradleSubplugin<AbstractCompile> {

    override fun isApplicable(
        project: Project,
        task: AbstractCompile
    ) = project.plugins.hasPlugin(RethrowerPlugin::class.java)

    override fun getCompilerPluginId() = "rethrower"

    override fun getPluginArtifact() = SubpluginArtifact(
        "rethrower",
        "rethrower-compiler",
        "0.0.1"
    )

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {

        val extension = project.extensions[RethrowerPlugin.EXTENSION_NAME] as RethrowerExtension
        return extension.rethrowFolders.map { SubpluginOption(key = "rethrowFolders", value = it) }
    }
}