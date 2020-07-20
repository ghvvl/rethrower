package rethrower.compiler

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import rethrower.BaseExceptionCode

class RethrowerCodegenExtension(
    private val rethrowFolders: List<String>
) : ExpressionCodegenExtension {

    private val baseExceptionCodeClass = BaseExceptionCode::class

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        if (codegen.state.classBuilderMode == ClassBuilderMode.KAPT3) return

        val currentClass = codegen.descriptor
        val currentClassKT = codegen.myClass

        if (currentClassKT.isLocal) return //ignore local objects

        generateEnumErrorClass(codegen, currentClass, currentClassKT, rethrowFolders)
    }
}