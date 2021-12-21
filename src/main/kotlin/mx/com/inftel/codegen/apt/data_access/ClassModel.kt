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

import java.beans.Introspector
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType

class ClassModel(private val typeElement: TypeElement) {

    val packageName: String by lazy {
        var posiblePackageElement: Element? = typeElement.enclosingElement
        while (posiblePackageElement != null && posiblePackageElement !is PackageElement) {
            posiblePackageElement = posiblePackageElement.enclosingElement
        }
        if (posiblePackageElement is PackageElement) {
            posiblePackageElement.qualifiedName.toString()
        } else {
            ""
        }
    }

    val simpleName: String by lazy {
        typeElement.simpleName.toString()
    }

    val qualifiedName: String by lazy {
        typeElement.qualifiedName.toString()
    }

    val simpleNameWithoutEntitySuffix: String by lazy {
        val simpleName = typeElement.simpleName.toString()
        if (simpleName.endsWith("Entity")) {
            simpleName.substring(simpleName.length - 6)
        } else {
            simpleName
        }
    }

    val isTopLevel: Boolean by lazy {
        typeElement.nestingKind == NestingKind.TOP_LEVEL
    }

    val isPublic: Boolean by lazy {
        typeElement.modifiers.contains(Modifier.PUBLIC)
    }

    val isAbstract: Boolean by lazy {
        typeElement.modifiers.contains(Modifier.ABSTRACT)
    }

    val isEntity: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(typeElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Entity")
        } != null
    }

    val isEmbeddable: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(typeElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Embeddable")
        } != null
    }

    val isMappedSuperclass: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(typeElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.MappedSuperclass")
        } != null
    }

    val hasDto: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(typeElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Dto")
        } != null
    }

    val hasCrud: Boolean by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(typeElement)
        anns.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Crud")
        } != null
    }

    val crudQualifiedName: String by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(typeElement)
        val ann = anns.first { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Crud")
        }
        ann.elementValues.values.first().value as String
    }

    val crudSimpleName: String by lazy {
        crudQualifiedName.substringAfterLast('.')
    }

    val crudPackageName: String by lazy {
        crudQualifiedName.substringBeforeLast('.', "")
    }

    val dtoQualifiedName: String by lazy {
        val anns = processingEnvironment!!.elementUtils.getAllAnnotationMirrors(typeElement)
        val ann = anns.first { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Dto")
        }
        ann.elementValues.values.first().value as String
    }

    val dtoSimpleName: String by lazy {
        dtoQualifiedName.substringAfterLast('.')
    }

    val dtoPackageName: String by lazy {
        dtoQualifiedName.substringBeforeLast('.', "")
    }

    private val superClass: ClassModel? by lazy {
        val superClass = typeElement.superclass?.let {
            if (it is DeclaredType) {
                ClassModel(it.asElement() as TypeElement)
            } else {
                null
            }
        }
        if (superClass != null && (superClass.isEntity || superClass.isMappedSuperclass)) {
            superClass
        } else {
            null
        }
    }

    private val keys: Set<String> by lazy {
        processingEnvironment!!.elementUtils.getAllMembers(typeElement).asSequence().filterIsInstance<ExecutableElement>().map {
            MethodModel(it)
        }.filter {
            it.isColumn || it.isJoinColumn || it.isEmbeddedId || it.isEmbedded
        }.mapNotNull {
            if (it.methodName.length > 2 && it.methodName.startsWith("is")) {
                Introspector.decapitalize(it.methodName.substring(2))
            } else if (it.methodName.length > 3 && it.methodName.startsWith("get")) {
                Introspector.decapitalize(it.methodName.substring(3))
            } else {
                null
            }
        }.toSet()
    }

    private val declaredFields: List<FieldModel> by lazy {
        typeElement.enclosedElements.filterIsInstance<VariableElement>().filter {
            !it.modifiers.contains(Modifier.STATIC)
        }.map {
            FieldModel(it)
        }
    }

    private val declaredIsMethods: List<MethodModel> by lazy {
        typeElement.enclosedElements.filterIsInstance<ExecutableElement>().filter {
            !it.modifiers.contains(Modifier.STATIC)
        }.filter {
            it.simpleName.length > 2 && it.simpleName.startsWith("is")
        }.map {
            MethodModel(it)
        }
    }

    private val declaredGetMethods: List<MethodModel> by lazy {
        typeElement.enclosedElements.filterIsInstance<ExecutableElement>().filter {
            !it.modifiers.contains(Modifier.STATIC)
        }.filter {
            it.simpleName.length > 3 && it.simpleName.startsWith("get")
        }.map {
            MethodModel(it)
        }
    }

    private val declaredSetMethods: List<MethodModel> by lazy {
        typeElement.enclosedElements.filterIsInstance<ExecutableElement>().filter {
            !it.modifiers.contains(Modifier.STATIC)
        }.filter {
            it.simpleName.length > 3 && it.simpleName.startsWith("set")
        }.map {
            MethodModel(it)
        }
    }

    val properties: List<PropertyModel> by lazy {
        val declaredFields = declaredFields.associateBy { it.fieldName }
        val declaredIsMethods = declaredIsMethods.associateBy { Introspector.decapitalize(it.methodName.substring(2)) }
        val declaredGetMethods = declaredGetMethods.associateBy { Introspector.decapitalize(it.methodName.substring(3)) }
        val declaredSetMethods = declaredSetMethods.associateBy { Introspector.decapitalize(it.methodName.substring(3)) }
        val superProperties = superClass?.properties?.associateBy { it.propertyName } ?: emptyMap()
        val propertiesNames = keys + declaredFields.keys + superProperties.keys
        propertiesNames.map { PropertyModel(it, declaredFields[it], declaredGetMethods[it] ?: declaredIsMethods[it], declaredSetMethods[it], superProperties[it]) }
    }

    val idProperty: PropertyModel? by lazy {
        properties.firstOrNull { it.isId }
    }

    val embeddedIdProperty: PropertyModel? by lazy {
        properties.firstOrNull { it.isEmbeddedId }
    }
}

