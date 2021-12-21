/*
 * Copyright 2021 Santos Zatarain Vera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mx.com.inftel.codegen.apt.data_access

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class FieldModel(val variableElement: VariableElement) {

    val fieldName: String by lazy {
        variableElement.simpleName.toString()
    }

    val fieldType: Type by lazy {
        variableElement.asType().toType()
    }

    val isId: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Id")
        } != null
    }

    val isEmbeddedId: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.EmbeddedId")
        } != null
    }

    val isVersion: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Version")
        } != null
    }

    val isColumn: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
        } != null
    }

    val isJoinColumn: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.JoinColumn")
        } != null
    }

    val isEmbedded: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Embedded")
        } != null
    }

    val isGeneratedValue: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.GeneratedValue")
        } != null
    }

    val isNotNull: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        val ann1 = anns.firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("org.jetbrains.annotations.NotNull")
        }
        val ann2 = anns.firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.validation.constraints.NotNull")
        }
        val ann3 = anns.firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.validation.constraints.NotBlank")
        }
        (ann1 != null || ann2 != null || ann3 != null)
    }

    val isInsertTimestamp: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.InsertTimestamp")
        } != null
    }

    val isUpdateTimestamp: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.UpdateTimestamp")
        } != null
    }

    val isAlternativeId: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.AlternativeId")
        } != null
    }

    val isInsertable: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        when {
            isColumn -> {
                val ann = anns.first {
                    (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
                }
                val map = processingEnvironment!!.elementUtils.getElementValuesWithDefaults(ann)
                val key = map.keys.first { it.simpleName.contentEquals("insertable") }
                map[key]!!.value as Boolean
            }
            isJoinColumn -> {
                val ann = anns.first {
                    (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.JoinColumn")
                }
                val map = processingEnvironment!!.elementUtils.getElementValuesWithDefaults(ann)
                val key = map.keys.first { it.simpleName.contentEquals("insertable") }
                map[key]!!.value as Boolean
            }
            else -> false
        }
    }

    val isUpdatable: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        when {
            isColumn -> {
                val ann = anns.first {
                    (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
                }
                val map = processingEnvironment!!.elementUtils.getElementValuesWithDefaults(ann)
                val key = map.keys.first { it.simpleName.contentEquals("updatable") }
                map[key]!!.value as Boolean
            }
            isJoinColumn -> {
                val ann = anns.first {
                    (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.JoinColumn")
                }
                val map = processingEnvironment!!.elementUtils.getElementValuesWithDefaults(ann)
                val key = map.keys.first { it.simpleName.contentEquals("updatable") }
                map[key]!!.value as Boolean
            }
            else -> false
        }
    }

    val validations: List<AnnotationMirror> by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(variableElement)
        anns.filter { ann ->
            val mirrors = ann.annotationType.asElement().annotationMirrors
            mirrors.firstOrNull { mirror ->
                (mirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.validation.Constraint")
            } != null
        }
    }
}