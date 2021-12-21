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

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class CodegenProcessor : AbstractProcessor() {

    private val generatedClasses: MutableSet<String> = mutableSetOf()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        try {
            processingEnvironment = processingEnv
            val annotationElement = processingEnv.elementUtils.getTypeElement("javax.persistence.Entity")
            val entities = roundEnv.getElementsAnnotatedWith(annotationElement)
            for (entity in entities) {
                val classModel = ClassModel(entity as TypeElement)
                if (classModel.isTopLevel && classModel.isPublic && !classModel.isAbstract) {
                    if (classModel.hasDto) {
                        val dtoQualifiedName = classModel.dtoQualifiedName
                        if (!generatedClasses.contains(dtoQualifiedName)) {
                            generatedClasses.add(dtoQualifiedName)
                            processingEnv.filer.createSourceFile(dtoQualifiedName, entity).openWriter().buffered().use { writer ->
                                generateDto(writer, classModel)
                            }
                        }
                    }
                    if (classModel.hasCrud) {
                        val crudQualifiedName = classModel.crudQualifiedName
                        if (!generatedClasses.contains(crudQualifiedName)) {
                            generatedClasses.add(crudQualifiedName)
                            processingEnv.filer.createSourceFile(crudQualifiedName, entity).openWriter().buffered().use { writer ->
                                generateCrud(writer, classModel)
                            }
                        }
                    }
                }
            }
        } finally {
            processingEnvironment = null
        }
        return false
    }
}