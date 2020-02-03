package rethrower

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class RethrowCompiler : AbstractProcessor() {

    private lateinit var messager: Messager

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        messager = processingEnv.messager
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val foldersForWork = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val foldersList = foldersForWork?.split(" ")
        if (foldersForWork == null || foldersList == null) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Nothing to generate :(")
        }

        return true
    }

    override fun getSupportedOptions() = setOf(KAPT_KOTLIN_GENERATED_OPTION_NAME)

    override fun getSupportedAnnotationTypes() = setOf(Hide::class.java.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "generate_rethrow"
    }
}