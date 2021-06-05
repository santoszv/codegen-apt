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

@file:Suppress("DuplicatedCode")

package mx.com.inftel.codegen.apt.data_access

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class DataAccessCodegen : AbstractProcessor() {

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        try {
            val entities = roundEnv.getElementsAnnotatedWithAny(*annotations.toTypedArray()).filter { element ->
                element.kind == ElementKind.CLASS
                        && element.modifiers.contains(Modifier.PUBLIC)
                        && !element.modifiers.contains(Modifier.STATIC)
                        && !element.modifiers.contains(Modifier.ABSTRACT)
            }.filter { element ->
                val annotationMirrors = processingEnv.elementUtils.getAllAnnotationMirrors(element)
                val crudAnn = annotationMirrors.find { (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Crud") }
                val dtoAnn = annotationMirrors.find { (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Dto") }
                (crudAnn != null && dtoAnn != null)
            }.map { element ->
                EntityModel(processingEnv, element)
            }
            for (entity in entities) {
                entity.generateCode()
            }
        } catch (ex: Exception) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, ex.stackTraceToString())
        }
        return false
    }
}