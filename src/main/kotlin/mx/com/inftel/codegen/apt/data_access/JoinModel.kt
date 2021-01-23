@file:Suppress("DuplicatedCode")

package mx.com.inftel.codegen.apt.data_access

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement

class JoinModel(private val processingEnv: ProcessingEnvironment, val getter: ExecutableElement, val setter: ExecutableElement) {

    val propertyName: String by lazy {
        getter.propertyName
    }

    val capitalizedName: String by lazy {
        getter.capitalizedName
    }
}