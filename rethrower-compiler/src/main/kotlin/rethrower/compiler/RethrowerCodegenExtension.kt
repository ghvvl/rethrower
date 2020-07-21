package rethrower.compiler

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtNamedFunction
import rethrower.BaseException
import rethrower.BaseExceptionCode

class RethrowerCodegenExtension(
    private val rethrowFolders: List<String>
) : ExpressionCodegenExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        if (codegen.state.classBuilderMode == ClassBuilderMode.KAPT3) return

        val currentClass = codegen.descriptor
        val currentClassKT = codegen.myClass

        if (currentClassKT.isLocal) return //ignore local objects

        val currentClassKTName = currentClassKT.name!!
        val classPackage = codegen.className.substringBefore(currentClassKTName)
        val classInRethrowFolder = rethrowFolders.indexOfFirst { classPackage.contains(it) }

        if (classInRethrowFolder != -1 && currentClass.kind == ClassKind.CLASS && !currentClass.isHide) {
            val errorClassShortName = currentClassKTName.filter { it.isUpperCase() }
            val errorClassName = errorClassShortName + "E"
            val enumEntries = getEnumEntries(currentClassKT.body!!.functions)
            val enumErrorCodeClassName = errorClassName + "C"

            generateErrorClass(
                codegen,
                currentClass,
                currentClassKT,
                errorClassName,
                enumErrorCodeClassName,
                errorClassShortName,
                currentClassKTName
            )
            generateEnumErrorClass(
                codegen,
                currentClass,
                currentClassKT,
                enumErrorCodeClassName,
                enumEntries
            )
        }
    }
}

private fun getEnumEntries(
    functionList: List<KtNamedFunction>
): List<String> = functionList.map {
    val functionName = it.name!!
    val builder = StringBuilder(functionName.length)

    functionName.forEach { char ->
        if (char.isUpperCase()) {
            builder.append('_')
            builder.append(char)
        } else {
            builder.append(char.toUpperCase())
        }
    }

    builder.toString()
}