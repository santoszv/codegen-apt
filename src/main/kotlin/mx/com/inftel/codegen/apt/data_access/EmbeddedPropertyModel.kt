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
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class EmbeddedPropertyModel(private val processingEnv: ProcessingEnvironment, val getter: ExecutableElement, val setter: ExecutableElement) {

    val propertyName: String by lazy {
        getter.propertyName
    }

    val capitalizedName: String by lazy {
        getter.capitalizedName
    }

    val isColumn: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
        }
        (ann != null)
    }

    val isJoinColumn: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.JoinColumn")
        }
        (ann != null)
    }

    val isInsertable: Boolean by lazy {
        when {
            isColumn -> {
                val columnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(getter).first { annotationMirror ->
                    (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
                }
                val key = columnAnn.elementValues.keys.firstOrNull { it.simpleName.contentEquals("insertable") }
                if (key != null) {
                    columnAnn.elementValues[key]!!.value as Boolean
                } else {
                    true
                }
            }
            isJoinColumn -> {
                val joinColumnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(getter).first { annotationMirror ->
                    (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.JoinColumn")
                }
                val key = joinColumnAnn.elementValues.keys.firstOrNull { it.simpleName.contentEquals("insertable") }
                if (key != null) {
                    joinColumnAnn.elementValues[key]!!.value as Boolean
                } else {
                    true
                }
            }
            else -> throw RuntimeException()
        }
    }

    val isUpdatable: Boolean by lazy {
        when {
            isColumn -> {
                val columnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(getter).first { annotationMirror ->
                    (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
                }
                val key = columnAnn.elementValues.keys.firstOrNull { it.simpleName.contentEquals("updatable") }
                if (key != null) {
                    columnAnn.elementValues[key]!!.value as Boolean
                } else {
                    true
                }
            }
            isJoinColumn -> {
                val joinColumnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(getter).first { annotationMirror ->
                    (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.JoinColumn")
                }
                val key = joinColumnAnn.elementValues.keys.firstOrNull { it.simpleName.contentEquals("updatable") }
                if (key != null) {
                    joinColumnAnn.elementValues[key]!!.value as Boolean
                } else {
                    true
                }
            }
            else -> throw RuntimeException()
        }
    }

    val isNotNull: Boolean by lazy {
        val ann1 = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("org.jetbrains.annotations.NotNull")
        }
        val ann2 = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.validation.constraints.NotNull")
        }
        val ann3 = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.validation.constraints.NotBlank")
        }
        (ann1 != null || ann2 != null || ann3 != null)
    }

    val validations: List<AnnotationMirror> by lazy {
        processingEnv.elementUtils.getAllAnnotationMirrors(getter).filter { getterAnn ->
            getterAnn.annotationType.asElement().annotationMirrors.firstOrNull { constAnn ->
                (constAnn.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.validation.Constraint")
            } != null
        }
    }
}