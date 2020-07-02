package rethrower.compiler

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import rethrower.Hide

val HIDE_CLASS_FQNAME: FqName = FqName(Hide::class.java.canonicalName)

val ClassDescriptor.isHide: Boolean
    get() = this.annotations.hasAnnotation(HIDE_CLASS_FQNAME)
