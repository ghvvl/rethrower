package rethrower.compiler

import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

class RethrowerComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
       val rethrowFolders: List<String> = configuration.getList(RETHROW_FOLDERS_KEY)

        ClassBuilderInterceptorExtension.registerExtension(
            project,
            RethrowerExtension(rethrowFolders)
        )
    }

}