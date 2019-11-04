package rethrower

abstract class BaseException(
    private val exceptionCode: BaseExceptionCode,
    override val cause: Throwable?,
    override val message: String?
) : Throwable(message, cause) {

    fun errorCode() = exceptionCode.value

    fun errorCodeName() = exceptionCode

    abstract fun domainShortName(): String

    abstract fun domainLongName(): String

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this::class == other::class && exceptionCode == (other as BaseException).exceptionCode)
            return other.cause == this.cause

        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}