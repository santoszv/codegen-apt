/*
 *    Copyright 2021 Santos Zatarain Vera
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package mx.com.inftel.codegen.apt.data_access

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

class EmbeddableModel(private val processingEnv: ProcessingEnvironment, val getter: ExecutableElement, val setter: ExecutableElement) {

    val properties: List<EmbeddedPropertyModel> by lazy {
        val embeddableElement = (getter.returnType as DeclaredType).asElement()
        val executableElements = processingEnv.elementUtils.getAllMembers(embeddableElement as TypeElement).filterIsInstance<ExecutableElement>()
        val getters = executableElements.filter { executableElement ->
            val columnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(executableElement).firstOrNull { annotationMirror ->
                (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
            }
            val joinColumnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(executableElement).firstOrNull { annotationMirror ->
                (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.JoinColumn")
            }
            (columnAnn != null || joinColumnAnn != null)
        }
        getters.mapNotNull { getter ->
            val setter = executableElements.firstOrNull { executableElement ->
                executableElement.simpleName.contentEquals("set${getter.capitalizedName}")
            }
            if (setter != null) {
                EmbeddedPropertyModel(processingEnv, getter, setter)
            } else {
                null
            }
        }
    }
}