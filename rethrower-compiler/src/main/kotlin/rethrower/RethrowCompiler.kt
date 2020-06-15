package rethrower

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@Deprecated(message = "Use RethrowExtension instead", replaceWith = ReplaceWith("RethrowExtension"))
class RethrowCompiler : AbstractProcessor() {

    private lateinit var messager: Messager

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        messager = processingEnv.messager
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        generation(annotations, roundEnv)
        return true
    }

    private fun generation(annotations: Set<TypeElement>, roundEnv: RoundEnvironment) {
        val elements = roundEnv.rootElements
        if (elements.isEmpty()) return //Skip generation without elements

        val foldersForWork = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val foldersList = foldersForWork?.split(" ")
        if (foldersForWork == null || foldersList == null) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Nothing to generate :(")
        }

        elements
            .filter { it.kind == ElementKind.CLASS }
            .filter { element ->
                var filter = false
                for (folder in foldersList!!) {
                    if (element.enclosingElement.toString().contains(folder)) {
                        filter = true
                        break
                    }
                }

                filter
            }
            .filter { it.getAnnotation(Hide::class.java) == null }
            .generateRethrowIfNeeded()
    }

    private fun List<Element>.generateRethrowIfNeeded() = forEach { element ->
        val functions = element
            .enclosedElements
            .filterIsInstance<ExecutableElement>()
            .filter { it.kind == ElementKind.METHOD }
            .filter { it.getAnnotation(Hide::class.java) == null }



        functions.forEach {
            println("$element $it")
            val returnType = it.modifiers
        }

    }

    private fun println(text: Any?) =
        messager.printMessage(
            Diagnostic.Kind.WARNING,
            text?.toString() ?: "Please provide correct info"
        )

    override fun getSupportedOptions() = setOf(KAPT_KOTLIN_GENERATED_OPTION_NAME)

    override fun getSupportedAnnotationTypes() = setOf("*")

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "generate_rethrow"
    }
}