package rethrower

@Target(allowedTargets = [AnnotationTarget.FUNCTION, AnnotationTarget.CLASS])
@Retention(value = AnnotationRetention.RUNTIME)
annotation class Hide