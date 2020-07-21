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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOriginFromPure
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

fun generateEnumErrorClass(
    codegen: ImplementationBodyCodegen,
    currentClass: ClassDescriptor,
    currentClassKt: KtPureClassOrObject,
    enumErrorClassName: String,
    enumEntries: List<String>
) {
    val containerAsmType = codegen.typeMapper.mapType(currentClass.defaultType)
    val enumErrorAsmType =
        Type.getObjectType(containerAsmType.internalName + "\$$enumErrorClassName")

    val enumErrorClass = ClassDescriptorImpl(
        currentClass,
        Name.identifier(enumErrorClassName),
        Modality.FINAL,
        ClassKind.ENUM_CLASS,
        emptyList(),
        currentClass.source,
        false,
        LockBasedStorageManager.NO_LOCKS
    )

    enumErrorClass.initialize(
        MemberScope.Empty,
        emptySet(),
        DescriptorFactory.createPrimaryConstructorForObject(enumErrorClass, enumErrorClass.source)
    )
    val classContextForEnumError = ClassContext(
        codegen.typeMapper,
        enumErrorClass,
        OwnerKind.IMPLEMENTATION,
        codegen.context.parentContext,
        null
    )

    /*ParcelableCodegenExtension create here copy of asm type idk for what */
    val classBuilderForEnumError = codegen.state.factory.newVisitor(
        JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, enumErrorClass),
        enumErrorAsmType,
        codegen.myClass.containingKtFile
    )

    val enumErrorClassCodegen = ImplementationBodyCodegen(
        currentClassKt,
        classContextForEnumError,
        classBuilderForEnumError,
        codegen.state,
        codegen.parentCodegen,
        false
    )
    classBuilderForEnumError.defineClass(
        null,
        Opcodes.V1_8,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER or Opcodes.ACC_ENUM,
        enumErrorAsmType.internalName,
        null,
        "${AsmTypes.ENUM_TYPE.internalName}<${enumErrorAsmType.internalName}>",
        arrayOf(baseExceptionCodeClassName)
    )
    codegen.v.visitInnerClass(
        enumErrorAsmType.internalName,
        containerAsmType.internalName,
        enumErrorClassName,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_ENUM
    )
    enumErrorClassCodegen.v.visitInnerClass(
        enumErrorAsmType.internalName,
        containerAsmType.internalName,
        enumErrorClassName,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_ENUM
    )

    writeSyntheticClassMetadata(classBuilderForEnumError, codegen.state)

    writeEnumEntries(
        enumErrorClassCodegen,
        enumEntries,
        enumErrorClass,
        enumErrorAsmType
    )
    writeValueField(enumErrorClassCodegen)
    writeStaticInitializer(
        enumErrorClassCodegen,
        enumEntries,
        enumErrorClass,
        enumErrorAsmType,
        currentClassKt
    )
    writeGetValueFunction(enumErrorClassCodegen, enumErrorClass, enumErrorAsmType, currentClassKt)
    writeEnumErrorClassConstructor(
        enumErrorClassCodegen,
        enumErrorClass,
        enumErrorAsmType,
        currentClassKt
    )
    writeDefaultEnumMethods(
        enumErrorClassCodegen,
        enumErrorClass,
        enumErrorAsmType,
        currentClassKt
    )
    classBuilderForEnumError.done()
}

private fun writeEnumErrorClassConstructor(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    currentClassKt: KtPureClassOrObject
) {
    val funcName = "<init>"
    val constructorDescriptor =
        DescriptorFactory
            .createPrimaryConstructorForObject(enumErrorClass, enumErrorClass.source)
            .apply {
                visibility = Visibilities.PRIVATE
            }

    val mv = codegen.v.newMethod(
        OtherOriginFromPure(currentClassKt, constructorDescriptor),
        Opcodes.ACC_PRIVATE,
        funcName,
        "($stringDescriptor${Type.INT_TYPE.descriptor})${Type.VOID_TYPE.descriptor}",
        null,
        null
    )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitVarInsn(Opcodes.ALOAD, 1)
    mv.visitVarInsn(Opcodes.ILOAD, 2)
    mv.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        AsmTypes.ENUM_TYPE.internalName,
        funcName,
        "($stringDescriptor${Type.INT_TYPE.descriptor})${Type.VOID_TYPE.descriptor}",
        false
    )
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        enumErrorAsmType.internalName,
        "ordinal",
        "()${Type.INT_TYPE.descriptor}",
        false
    )
    mv.visitFieldInsn(
        Opcodes.PUTFIELD,
        enumErrorAsmType.internalName,
        "value",
        Type.INT_TYPE.descriptor
    )
    mv.visitInsn(Opcodes.RETURN)
    FunctionCodegen.endVisit(mv, funcName, currentClassKt)
}

private fun writeDefaultEnumMethods(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    currentClassKt: KtPureClassOrObject
) {
    writeValuesFunction(codegen, enumErrorClass, enumErrorAsmType, currentClassKt)
    writeValueOfFunction(codegen, enumErrorClass, enumErrorAsmType, currentClassKt)
}

/**
 * Source code was copied from [ImplementationBodyCodegen.generateEnumValuesMethod]
 * */
private fun writeValuesFunction(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    currentClassKt: KtPureClassOrObject
) {

    val type = codegen
        .typeMapper
        .mapType(
            enumErrorClass
                .builtIns
                .getArrayType(Variance.INVARIANT, enumErrorClass.defaultType)
        )

    val mv = codegen.v.newMethod(
        OtherOriginFromPure(
            currentClassKt,
            DescriptorFactory.createEnumValuesMethod(enumErrorClass)
        ),
        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
        DescriptorUtils.ENUM_VALUES.asString(),
        "()${type.descriptor}",
        null,
        null
    )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.visitFieldInsn(
        Opcodes.GETSTATIC,
        enumErrorAsmType.internalName,
        "\$VALUES",
        type.descriptor
    )
    mv.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        type.internalName,
        "clone",
        "()$objectDescriptor",
        false
    )
    mv.visitTypeInsn(Opcodes.CHECKCAST, type.internalName)
    mv.visitInsn(Opcodes.ARETURN)
    FunctionCodegen.endVisit(mv, "values()", currentClassKt)
}

/**
 * Source code was copied from [ImplementationBodyCodegen.generateEnumValueOfMethod]
 * */
private fun writeValueOfFunction(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    currentClassKt: KtPureClassOrObject
) {
    val mv =
        codegen.v.newMethod(
            OtherOriginFromPure(
                currentClassKt,
                DescriptorFactory.createEnumValueOfMethod(enumErrorClass)
            ),
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            DescriptorUtils.ENUM_VALUE_OF.asString(),
            "($stringDescriptor)${enumErrorAsmType.descriptor}",
            null,
            null
        )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.visitLdcInsn(enumErrorAsmType)
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        AsmTypes.ENUM_TYPE.internalName,
        "valueOf",
        "($classDescriptor$stringDescriptor)$enumDescriptor",
        false
    )
    mv.visitTypeInsn(Opcodes.CHECKCAST, enumErrorAsmType.internalName)
    mv.visitInsn(Opcodes.ARETURN)
    FunctionCodegen.endVisit(mv, "valueOf()", currentClassKt)
}

private fun writeGetValueFunction(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    currentClassKt: KtPureClassOrObject
) {
    val funcName = "getValue"
    val funcDescriptor = SimpleFunctionDescriptorImpl.create(
        enumErrorClass,
        Annotations.EMPTY,
        Name.identifier(funcName),
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        enumErrorClass.source
    )

    funcDescriptor.initialize(
        null,
        null,
        emptyList(),
        emptyList(),
        enumErrorClass.builtIns.intType,
        Modality.OPEN,
        Visibilities.PUBLIC
    )
    val mv =
        codegen.v.newMethod(
            OtherOriginFromPure(currentClassKt, funcDescriptor),
            Opcodes.ACC_PUBLIC,
            funcName,
            "()${Type.INT_TYPE.descriptor}",
            null,
            null
        )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitFieldInsn(
        Opcodes.GETFIELD,
        enumErrorAsmType.internalName,
        "value",
        Type.INT_TYPE.descriptor
    )
    mv.visitInsn(Opcodes.IRETURN)
    FunctionCodegen.endVisit(mv, funcName, currentClassKt)
}

private fun writeValueField(codegen: ImplementationBodyCodegen) {
    codegen.v.newField(
        JvmDeclarationOrigin.NO_ORIGIN,
        Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL,
        "value",
        Type.INT_TYPE.descriptor,
        null,
        null
    )
}

private fun writeEnumEntries(
    codegen: ImplementationBodyCodegen,
    entries: List<String>,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type
) {
    fun createField(
        codegen: ImplementationBodyCodegen,
        fieldName: String,
        asmType: Type,
        isPublic: Boolean,
        isEnum: Boolean,
        isSynthetic: Boolean
    ) {
        codegen.v.newField(
            JvmDeclarationOrigin.NO_ORIGIN,
            (if (isPublic) Opcodes.ACC_PUBLIC else Opcodes.ACC_PRIVATE) or
                    when {
                        isEnum -> Opcodes.ACC_FINAL or Opcodes.ACC_STATIC or Opcodes.ACC_ENUM
                        isSynthetic -> Opcodes.ACC_FINAL or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC
                        else -> Opcodes.ACC_FINAL or Opcodes.ACC_STATIC
                    },
            fieldName,
            asmType.descriptor,
            null,
            null
        )
    }
    entries.forEach {
        createField(
            codegen,
            it,
            enumErrorAsmType,
            isPublic = true,
            isEnum = true,
            isSynthetic = false
        )
    }
    createField(
        codegen,
        "\$VALUES",
        codegen.typeMapper.mapType(
            enumErrorClass.builtIns.getArrayType(
                Variance.INVARIANT,
                enumErrorClass.defaultType
            )
        ),
        isPublic = false,
        isEnum = false,
        isSynthetic = true
    )
}

private fun writeStaticInitializer(
    codegen: ImplementationBodyCodegen,
    entries: List<String>,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    currentClassKt: KtPureClassOrObject
) {
    val funcName = "<clinit>"
    val funcDescriptor = SimpleFunctionDescriptorImpl.create(
        enumErrorClass,
        Annotations.EMPTY,
        Name.identifier(funcName),
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        enumErrorClass.source
    )

    funcDescriptor.initialize(
        null,
        null,
        emptyList(),
        emptyList(),
        enumErrorClass.builtIns.anyType,
        Modality.OPEN,
        Visibilities.PUBLIC
    )

    val mv =
        codegen.v.newMethod(
            OtherOriginFromPure(currentClassKt, funcDescriptor),
            Opcodes.ACC_STATIC,
            funcName,
            "()${Type.VOID_TYPE.descriptor}",
            null,
            null
        )
    if (!codegen.state.classBuilderMode.generateBodies) return

    mv.visitCode()
    mv.getVariableAccordingToIndex(entries.size)
    mv.visitTypeInsn(Opcodes.ANEWARRAY, enumErrorAsmType.internalName)
    mv.visitInsn(Opcodes.DUP)

    entries.forEachIndexed { index, entry ->
        mv.visitInsn(Opcodes.DUP)

        mv.getVariableAccordingToIndex(index)
        mv.visitTypeInsn(Opcodes.NEW, enumErrorAsmType.internalName)
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(entry)
        mv.getVariableAccordingToIndex(index)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            enumErrorAsmType.internalName,
            "<init>",
            "($stringDescriptor${Type.INT_TYPE.descriptor})${Type.VOID_TYPE.descriptor}",
            false
        )
        mv.visitInsn(Opcodes.DUP)
        mv.visitFieldInsn(
            Opcodes.PUTSTATIC,
            enumErrorAsmType.internalName,
            entry,
            enumErrorAsmType.descriptor
        )
        mv.visitInsn(Opcodes.AASTORE)
    }
    val fieldDescriptor =
        codegen.typeMapper.mapType(
            enumErrorClass.builtIns.getArrayType(Variance.INVARIANT, enumErrorClass.defaultType)
        ).descriptor

    mv.visitFieldInsn(
        Opcodes.PUTSTATIC,
        enumErrorAsmType.internalName,
        "\$VALUES",
        fieldDescriptor
    )
    mv.visitInsn(Opcodes.RETURN)
    FunctionCodegen.endVisit(mv, funcName, currentClassKt)
}

private fun MethodVisitor.getVariableAccordingToIndex(index: Int) =
    when (index) {
        0 -> {
            visitInsn(Opcodes.ICONST_0)
        }
        1 -> {
            visitInsn(Opcodes.ICONST_1)
        }
        2 -> {
            visitInsn(Opcodes.ICONST_2)
        }
        3 -> {
            visitInsn(Opcodes.ICONST_3)
        }
        4 -> {
            visitInsn(Opcodes.ICONST_4)
        }
        5 -> {
            visitInsn(Opcodes.ICONST_5)
        }
        else -> {
            visitIntInsn(Opcodes.BIPUSH, index)
        }
    }