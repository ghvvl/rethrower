package rethrower.compiler

import jdk.internal.org.objectweb.asm.Type
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import rethrower.BaseException
import rethrower.BaseExceptionCode
import rethrower.Hide

val HIDE_CLASS_FQNAME: FqName = FqName(Hide::class.java.canonicalName)

val ClassDescriptor.isHide: Boolean
    get() = this.annotations.hasAnnotation(HIDE_CLASS_FQNAME)

private val baseExceptionClass = BaseException::class.java
private val baseExceptionCodeClass = BaseExceptionCode::class.java
private val baseExceptionClassAsm = Type.getObjectType(baseExceptionClass.name)
private val baseExceptionCodeClassAsm = Type.getObjectType(baseExceptionCodeClass.name)

internal val baseExceptionClassName = baseExceptionClassAsm.internalName
internal val baseExceptionCodeClassName = baseExceptionCodeClassAsm.internalName
internal val baseExceptionClassDescriptor = baseExceptionClassAsm.descriptor
internal val baseExceptionCodeClassDescriptor = baseExceptionCodeClassAsm.descriptor