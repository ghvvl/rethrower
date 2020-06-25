package rethrower.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal val RETHROW_FOLDERS_KEY = CompilerConfigurationKey.create<List<String>>("rethrowfolders")