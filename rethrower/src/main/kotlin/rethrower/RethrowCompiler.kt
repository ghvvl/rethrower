package rethrower

import com.google.auto.service.AutoService
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.AGGREGATING)
class RethrowCompiler : AbstractProcessor() {

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        processingEnv.messager.printMessage(Diagnostic.Kind.OTHER, "HELLO DUDE")
        processingEnv.messager.printMessage(Diagnostic.Kind.OTHER, annotations.toString())
        println(annotations)
        println(roundEnv)
        return false
    }

    override fun getSupportedOptions(): Set<String> = setOf(OPTION_GENERATE_RETHROW)

    override fun getSupportedAnnotationTypes() = setOf(Hide::class.java.simpleName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    companion object {
        private const val OPTION_GENERATE_RETHROW = "generate_rethrow"

    }
}