package rethrower.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

class RethrowerCLIProcessor : CommandLineProcessor {

    override val pluginId = "rethrower"

    override val pluginOptions = listOf(RETHROW_FOLDERS_OPTION)

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option) {
        RETHROW_FOLDERS_OPTION -> configuration.appendList(RETHROW_FOLDERS_KEY, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }

    companion object {
        private val RETHROW_FOLDERS_OPTION = CliOption(
            "rethrowFolders",
            "folders to rethrow",
            "folders to rethrow",
            required = true,
            allowMultipleOccurrences = true
        )
    }
}