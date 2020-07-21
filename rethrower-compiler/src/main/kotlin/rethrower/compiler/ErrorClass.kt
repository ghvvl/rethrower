package rethrower.compiler

import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.writeSyntheticClassMetadata
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOriginFromPure
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

fun generateErrorClass(
    codegen: ImplementationBodyCodegen,
    currentClass: ClassDescriptor,
    currentClassKt: KtPureClassOrObject,
    errorClassName: String,
    enumErrorClassName: String,
    errorClassShortName: String,
    errorCLassLongName: String
) {
    val containerAsmType = codegen.typeMapper.mapType(currentClass.defaultType)
    val errorAsmType = Type.getObjectType(containerAsmType.internalName + "\$$errorClassName")
    val enumErrorAsmType =
        Type.getObjectType(containerAsmType.internalName + "\$$enumErrorClassName")

    val errorClass = ClassDescriptorImpl(
        currentClass,
        Name.identifier(errorClassName),
        Modality.FINAL,
        ClassKind.CLASS,
        emptyList(),
        currentClass.source,
        false,
        LockBasedStorageManager.NO_LOCKS
    )

    errorClass.initialize(
        MemberScope.Empty,
        emptySet(),
        DescriptorFactory.createPrimaryConstructorForObject(errorClass, errorClass.source)
    )
    val classContextForError = ClassContext(
        codegen.typeMapper,
        errorClass,
        OwnerKind.IMPLEMENTATION,
        codegen.context.parentContext,
        null
    )

    /*ParcelableCodegenExtension create here copy of asm type idk for what */
    val classBuilderForError = codegen.state.factory.newVisitor(
        JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, errorClass),
        errorAsmType,
        codegen.myClass.containingKtFile
    )

    val errorClassCodegen = ImplementationBodyCodegen(
        currentClassKt,
        classContextForError,
        classBuilderForError,
        codegen.state,
        codegen.parentCodegen,
        false
    )
    classBuilderForError.defineClass(
        null,
        Opcodes.V1_8,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER,
        errorAsmType.internalName,
        null,
        baseExceptionClassName,
        emptyArray()
    )
    codegen.v.visitInnerClass(
        errorAsmType.internalName,
        containerAsmType.internalName,
        errorClassName,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
    )
    errorClassCodegen.v.visitInnerClass(
        errorAsmType.internalName,
        containerAsmType.internalName,
        errorClassName,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
    )

    writeSyntheticClassMetadata(classBuilderForError, codegen.state)

    writeOverridedMembers(
        errorClassCodegen,
        errorClass,
        currentClassKt,
        errorAsmType,
        errorClassShortName,
        errorCLassLongName
    )
    writeErrorClassConstructor(
        errorClassCodegen,
        errorClass,
        errorAsmType,
        enumErrorAsmType,
        currentClassKt
    )

    classBuilderForError.done()
}

private fun writeOverridedMembers(
    codegen: ImplementationBodyCodegen,
    errorClass: ClassDescriptor,
    currentClassKt: KtPureClassOrObject,
    errorAsmType: Type,
    errorClassShortName: String,
    errorCLassLongName: String
) {
    writeDomainShortNameFunction(codegen, errorClass, currentClassKt, errorClassShortName)
    writeDomainLongNameFunction(codegen, errorClass, currentClassKt, errorCLassLongName)
    writeCauseField(codegen)
    writeGetCauseFunction(codegen, errorClass, currentClassKt, errorAsmType)
}

private fun writeDomainShortNameFunction(
    codegen: ImplementationBodyCodegen,
    errorClass: ClassDescriptor,
    currentClassKt: KtPureClassOrObject,
    errorClassShortName: String
) {
    val funcName = "domainShortName"
    val funcDescriptor = SimpleFunctionDescriptorImpl.create(
        errorClass,
        Annotations.EMPTY,
        Name.identifier(funcName),
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        errorClass.source
    )

    funcDescriptor.initialize(
        null,
        null,
        emptyList(),
        emptyList(),
        errorClass.builtIns.stringType,
        Modality.OPEN,
        Visibilities.PUBLIC
    )
    val mv = codegen.v.newMethod(
        OtherOriginFromPure(currentClassKt, funcDescriptor),
        Opcodes.ACC_PUBLIC,
        funcName,
        "()$stringDescriptor",
        null,
        null
    )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.visitLdcInsn(errorClassShortName)
    mv.visitInsn(Opcodes.ARETURN)
    FunctionCodegen.endVisit(mv, funcName, currentClassKt)
}

private fun writeDomainLongNameFunction(
    codegen: ImplementationBodyCodegen,
    errorClass: ClassDescriptor,
    currentClassKt: KtPureClassOrObject,
    errorClassLongName: String
) {
    val funcName = "domainLongName"
    val funcDescriptor = SimpleFunctionDescriptorImpl.create(
        errorClass,
        Annotations.EMPTY,
        Name.identifier(funcName),
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        errorClass.source
    )

    funcDescriptor.initialize(
        null,
        null,
        emptyList(),
        emptyList(),
        errorClass.builtIns.stringType,
        Modality.OPEN,
        Visibilities.PUBLIC
    )

    val mv = codegen.v.newMethod(
        OtherOriginFromPure(currentClassKt, funcDescriptor),
        Opcodes.ACC_PUBLIC,
        funcName,
        "()$stringDescriptor",
        null,
        null
    )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.visitLdcInsn(errorClassLongName)
    mv.visitInsn(Opcodes.ARETURN)
    FunctionCodegen.endVisit(mv, funcName, currentClassKt)
}

private fun writeCauseField(codegen: ImplementationBodyCodegen) {
    codegen.v.newField(
        JvmDeclarationOrigin.NO_ORIGIN,
        Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL,
        "cause",
        throwableDescriptor,
        null,
        null
    )
}

private fun writeGetCauseFunction(
    codegen: ImplementationBodyCodegen,
    errorClass: ClassDescriptor,
    currentClassKt: KtPureClassOrObject,
    errorAsmType: Type
) {
    val funcName = "getCause"
    val funcDescriptor = SimpleFunctionDescriptorImpl.create(
        errorClass,
        Annotations.EMPTY,
        Name.identifier(funcName),
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        errorClass.source
    )

    funcDescriptor.initialize(
        null,
        null,
        emptyList(),
        emptyList(),
        errorClass.builtIns.throwable.defaultType,
        Modality.OPEN,
        Visibilities.PUBLIC
    )
    val mv =
        codegen.v.newMethod(
            OtherOriginFromPure(currentClassKt, funcDescriptor),
            Opcodes.ACC_PUBLIC,
            funcName,
            "()$throwableDescriptor",
            null,
            null
        )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitFieldInsn(
        Opcodes.GETFIELD,
        errorAsmType.internalName,
        "cause",
        throwableDescriptor
    )
    mv.visitInsn(Opcodes.ARETURN)
    FunctionCodegen.endVisit(mv, funcName, currentClassKt)
}

private fun writeErrorClassConstructor(
    codegen: ImplementationBodyCodegen,
    errorClass: ClassDescriptor,
    errorAsmType: Type,
    enumErrorAsmType: Type,
    currentClassKt: KtPureClassOrObject
) {
    val funcName = "<init>"
    val constructorDescriptor =
        DescriptorFactory.createPrimaryConstructorForObject(errorClass, errorClass.source)
    val mv = codegen.v.newMethod(
        OtherOriginFromPure(currentClassKt, constructorDescriptor),
        Opcodes.ACC_PUBLIC,
        funcName,
        "(${enumErrorAsmType.descriptor}${throwableDescriptor})${Type.VOID_TYPE.descriptor}",
        null,
        null
    )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitVarInsn(Opcodes.ALOAD, 1)
    mv.visitTypeInsn(Opcodes.CHECKCAST, baseExceptionCodeClassName)
    mv.visitVarInsn(Opcodes.ALOAD, 2)
    mv.visitInsn(Opcodes.ACONST_NULL)
    mv.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        baseExceptionClassName,
        funcName,
        "(${baseExceptionCodeClassDescriptor}${throwableDescriptor}${stringDescriptor})${Type.VOID_TYPE.descriptor}",
        false
    )
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitVarInsn(Opcodes.ALOAD, 2)
    mv.visitFieldInsn(
        Opcodes.PUTFIELD,
        errorAsmType.internalName,
        "cause",
        throwableDescriptor
    )

    mv.visitInsn(Opcodes.RETURN)
    FunctionCodegen.endVisit(mv, funcName, currentClassKt)
}