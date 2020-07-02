package rethrower.compiler

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File

internal class RethrowerClassBuilderInterceptorExtension(
    private val rethrowFolders: List<String>
) : ClassBuilderInterceptorExtension {

    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink
    ): ClassBuilderFactory = object : ClassBuilderFactory by interceptedFactory {

        override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
            val classBuilderDelegate = interceptedFactory.newClassBuilder(origin)

            return object : DelegatingClassBuilder() {

                override fun defineClass(
                    origin: PsiElement?,
                    version: Int,
                    access: Int,
                    name: String,
                    signature: String?,
                    superName: String,
                    interfaces: Array<out String>
                ) {

                    super.defineClass(
                        origin,
                        version,
                        access,
                        name,
                        signature,
                        superName,
                        interfaces
                    )
                }

                override fun getDelegate() = classBuilderDelegate

                override fun newMethod(
                    origin: JvmDeclarationOrigin,
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?
                ): MethodVisitor {
                    val methodVisitorDelegate =
                        classBuilderDelegate.newMethod(
                            origin,
                            access,
                            name,
                            descriptor,
                            signature,
                            exceptions
                        )

                    return object : MethodVisitor(Opcodes.ASM7, methodVisitorDelegate) {

                            override fun visitMethodInsn(
                                opcode: Int,
                                owner: String,
                                name: String,
                                descriptor: String,
                                isInterface: Boolean
                            ) {
                                if (name != "unsafeCast" || owner != "com/jakewharton/confundus/Api") {
                                    super.visitMethodInsn(
                                        opcode,
                                        owner,
                                        name,
                                        descriptor,
                                        isInterface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}