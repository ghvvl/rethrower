package rethrower

@Target(allowedTargets = [AnnotationTarget.FUNCTION, AnnotationTarget.CLASS])
@Retention(value = AnnotationRetention.SOURCE)
annotation class Hide