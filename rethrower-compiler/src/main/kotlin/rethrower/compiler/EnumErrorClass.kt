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
import org.jetbrains.kotlin.psi.KtNamedFunction
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
    currentClassKT: KtPureClassOrObject,
    rethrowFolders: List<String>
) {
    val currentClassKTName = currentClassKT.name!!

    val classPackage = codegen.className.substringBefore(currentClassKTName)

    val classInRethrowFolder = rethrowFolders.indexOfFirst { classPackage.contains(it) }

    if (classInRethrowFolder != -1 && currentClass.kind == ClassKind.CLASS && !currentClass.isHide) {
        val enumEntries = getEnumEntries(currentClassKT.body!!.functions)
        val errorClassName = currentClassKTName.filter { it.isUpperCase() } + "E"
        val enumErrorCodesClassName = errorClassName + "C"

        generateEnumErrorClassBody(
            codegen,
            currentClass,
            currentClassKT,
            enumEntries,
            enumErrorCodesClassName
        )
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

private fun generateEnumErrorClassBody(
    codegen: ImplementationBodyCodegen,
    currentClass: ClassDescriptor,
    enumErrorKt: KtPureClassOrObject,
    enumEntries: List<String>,
    className: String
) {
    val containerAsmType = codegen.typeMapper.mapType(currentClass.defaultType)
    val enumErrorAsmType = Type.getObjectType(containerAsmType.internalName + "\$$className")

    val enumErrorClass = ClassDescriptorImpl(
        currentClass,
        Name.identifier(className),
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
        DescriptorFactory.createPrimaryConstructorForObject(
            enumErrorClass,
            enumErrorClass.source
        )
    )
    val classContextForEnumError = ClassContext(
        codegen.typeMapper,
        enumErrorClass,
        OwnerKind.IMPLEMENTATION,
        codegen.context.parentContext,
        null
    )

    val classBuilderForEnumError = codegen.state.factory.newVisitor(
        JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, enumErrorClass),
        Type.getObjectType(enumErrorAsmType.internalName),
        codegen.myClass.containingKtFile
    )

    val enumErrorClassCodegen = ImplementationBodyCodegen(
        enumErrorKt,
        classContextForEnumError,
        classBuilderForEnumError,
        codegen.state,
        codegen.parentCodegen,
        false
    )
    classBuilderForEnumError.defineClass(
        null,
        Opcodes.API_VERSION,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER or Opcodes.ACC_ENUM,
        enumErrorAsmType.internalName,
        null,
        "${AsmTypes.ENUM_TYPE.internalName}<${enumErrorAsmType.internalName}>",
        arrayOf("rethrower/BaseExceptionCode")
    )
    codegen.v.visitInnerClass(
        enumErrorAsmType.internalName,
        containerAsmType.internalName,
        className,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_ENUM
    )
    enumErrorClassCodegen.v.visitInnerClass(
        enumErrorAsmType.internalName,
        containerAsmType.internalName,
        className,
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
        enumErrorKt
    )
    writeGetValueFunction(enumErrorClassCodegen, enumErrorClass, enumErrorAsmType, enumErrorKt)
    writeEnumErrorClassConstructor(
        enumErrorClassCodegen,
        enumErrorClass,
        enumErrorAsmType,
        enumErrorKt
    )
    writeDefaultEnumMethods(
        enumErrorClassCodegen,
        enumErrorClass,
        enumErrorAsmType,
        enumErrorKt
    )

    classBuilderForEnumError.done()
}

private fun writeEnumErrorClassConstructor(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    enumErrorKt: KtPureClassOrObject
) {
    val constructorDescriptor =
        DescriptorFactory
            .createPrimaryConstructorForObject(enumErrorClass, enumErrorClass.source)
            .apply {
                visibility = Visibilities.PRIVATE
                returnType = enumErrorClass.defaultType
            }

    val mv = codegen.v.newMethod(
        OtherOriginFromPure(
            enumErrorKt,
            constructorDescriptor
        ),
        Opcodes.ACC_PRIVATE,
        "<init>",
        "(Ljava/lang/String;${Type.INT_TYPE.descriptor})${Type.VOID_TYPE.descriptor}",
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
        "<init>",
        "(Ljava/lang/String;${Type.INT_TYPE.descriptor})${Type.VOID_TYPE.descriptor}",
        false
    )
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
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
    FunctionCodegen.endVisit(mv, "<init>", enumErrorKt)
}

private fun writeDefaultEnumMethods(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    enumErrorKt: KtPureClassOrObject
) {
    writeValuesFunction(codegen, enumErrorClass, enumErrorAsmType, enumErrorKt)
    writeValueOfFunction(codegen, enumErrorClass, enumErrorAsmType, enumErrorKt)
}

/**
 * Source code was copied from [ImplementationBodyCodegen.generateEnumValuesMethod]
 * */
private fun writeValuesFunction(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    enumErrorKt: KtPureClassOrObject
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
            enumErrorKt,
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
        "()Ljava/lang/Object;",
        false
    )
    mv.visitTypeInsn(Opcodes.CHECKCAST, type.internalName)
    mv.visitInsn(Opcodes.ARETURN)
    FunctionCodegen.endVisit(mv, "values()", enumErrorKt)
}

/**
 * Source code was copied from [ImplementationBodyCodegen.generateEnumValueOfMethod]
 * */
private fun writeValueOfFunction(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    enumErrorKt: KtPureClassOrObject
) {
    val mv =
        codegen.v.newMethod(
            OtherOriginFromPure(
                enumErrorKt,
                DescriptorFactory.createEnumValueOfMethod(enumErrorClass)
            ),
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            DescriptorUtils.ENUM_VALUE_OF.asString(),
            "(Ljava/lang/String;)${enumErrorAsmType.descriptor}",
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
        "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;",
        false
    )
    mv.visitTypeInsn(Opcodes.CHECKCAST, enumErrorAsmType.internalName)
    mv.visitInsn(Opcodes.ARETURN)
    FunctionCodegen.endVisit(mv, "valueOf()", enumErrorKt)
}

private fun writeGetValueFunction(
    codegen: ImplementationBodyCodegen,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    enumErrorKt: KtPureClassOrObject
) {
    val funcDescriptor = SimpleFunctionDescriptorImpl.create(
        enumErrorClass,
        Annotations.EMPTY,
        Name.identifier("getValue"),
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
            OtherOriginFromPure(enumErrorKt, funcDescriptor),
            Opcodes.ACC_PUBLIC,
            "getValue",
            "()${Type.INT_TYPE.internalName}",
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
    FunctionCodegen.endVisit(mv, "getValue()", enumErrorKt)
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
        isPublic: Boolean
    ) {
        codegen.v.newField(
            JvmDeclarationOrigin.NO_ORIGIN,
            (if (isPublic) Opcodes.ACC_PUBLIC else Opcodes.ACC_PRIVATE) or Opcodes.ACC_FINAL or Opcodes.ACC_STATIC,
            fieldName,
            asmType.descriptor,
            null,
            null
        )
    }
    entries.forEach { createField(codegen, it, enumErrorAsmType, true) }
    createField(
        codegen,
        "\$VALUES",
        codegen.typeMapper.mapType(
            enumErrorClass.builtIns.getArrayType(
                Variance.INVARIANT,
                enumErrorClass.defaultType
            )
        ),
        false
    )
}

private fun writeStaticInitializer(
    codegen: ImplementationBodyCodegen,
    entries: List<String>,
    enumErrorClass: ClassDescriptor,
    enumErrorAsmType: Type,
    enumErrorKt: KtPureClassOrObject
) {
    val funcDescriptor = SimpleFunctionDescriptorImpl.create(
        enumErrorClass,
        Annotations.EMPTY,
        Name.identifier("<clinit>"),
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
            OtherOriginFromPure(enumErrorKt, funcDescriptor),
            Opcodes.ACC_STATIC,
            "<clinit>",
            "()${Type.VOID_TYPE.internalName}",
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
            "(Ljava/lang/String;${Type.INT_TYPE.internalName})${Type.VOID_TYPE.internalName}",
            false
        )
        mv.visitInsn(Opcodes.DUP)
        mv.visitFieldInsn(
            Opcodes.PUTSTATIC,
            enumErrorAsmType.internalName,
            entry,
            "L${enumErrorAsmType.internalName}"
        )
        mv.visitInsn(Opcodes.AASTORE)
    }
    mv.visitFieldInsn(
        Opcodes.PUTSTATIC,
        enumErrorAsmType.internalName,
        "\$VALUES",
        "[L${enumErrorAsmType.internalName}"
    )
    mv.visitInsn(Opcodes.RETURN)
    FunctionCodegen.endVisit(mv, "<clinit>", enumErrorKt)
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