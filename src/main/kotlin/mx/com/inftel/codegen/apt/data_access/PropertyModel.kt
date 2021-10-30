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

import java.io.BufferedWriter
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.*

class PropertyModel(private val processingEnv: ProcessingEnvironment, val getter: ExecutableElement, val setter: ExecutableElement) {

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

    val isEmbeddedId: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.EmbeddedId")
        }
        (ann != null)
    }

    val isEmbedded: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Embedded")
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

    val isId: Boolean by lazy {
        val annId = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Id")
        }
        val annEmbedId = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.EmbeddedId")
        }
        (annId != null || annEmbedId != null)
    }

    val isGeneratedValue: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.GeneratedValue")
        }
        (ann != null)
    }

    val isVersion: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Version")
        }
        (ann != null)
    }

    val isInsertTimestamp: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.InsertTimestamp")
        }
        (ann != null)
    }

    val isUpdateTimestamp: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.UpdateTimestamp")
        }
        (ann != null)
    }

    val isAltId: Boolean by lazy {
        val ann = processingEnv.elementUtils.getAllAnnotationMirrors(getter).firstOrNull { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.AlternativeId")
        }
        (ann != null)
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

    val isTimestamp: Boolean by lazy {
        isInsertTimestamp || isUpdateTimestamp
    }

    val isManaged: Boolean by lazy {
        isId || isVersion || isTimestamp
    }

    val validations: List<AnnotationMirror> by lazy {
        processingEnv.elementUtils.getAllAnnotationMirrors(getter).filter { getterAnn ->
            getterAnn.annotationType.asElement().annotationMirrors.firstOrNull { constAnn ->
                (constAnn.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.validation.Constraint")
            } != null
        }
    }

    val joinModel: JoinModel? by lazy {
        val returnType = processingEnv.typeUtils.asElement(getter.returnType)
        if (returnType != null
            && returnType.kind == ElementKind.CLASS
            && returnType.modifiers.contains(Modifier.PUBLIC)
            && !returnType.modifiers.contains(Modifier.STATIC)
            && !returnType.modifiers.contains(Modifier.ABSTRACT)
        ) {
            val entityAnn = processingEnv.elementUtils.getAllAnnotationMirrors(returnType).firstOrNull { annotationMirror ->
                (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Entity")
            }
            if (entityAnn != null) {
                val executableElements = processingEnv.elementUtils.getAllMembers(returnType as TypeElement).filterIsInstance<ExecutableElement>()
                val getter = executableElements.firstOrNull { executableElement ->
                    val columnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(executableElement).firstOrNull { annotationMirror ->
                        (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
                    }
                    val idAnn = processingEnv.elementUtils.getAllAnnotationMirrors(executableElement).firstOrNull { annotationMirror ->
                        (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Id")
                    }
                    (columnAnn != null || idAnn != null)
                }
                if (getter != null) {
                    val setter = executableElements.firstOrNull { executableElement ->
                        executableElement.simpleName.contentEquals("set${getter.capitalizedName}")
                    }
                    if (setter != null) {
                        JoinModel(processingEnv, getter, setter)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    val embeddableModel: EmbeddableModel? by lazy {
        val returnType = processingEnv.typeUtils.asElement(getter.returnType)
        if (returnType != null
            && returnType.kind == ElementKind.CLASS
            && returnType.modifiers.contains(Modifier.PUBLIC)
            && !returnType.modifiers.contains(Modifier.STATIC)
            && !returnType.modifiers.contains(Modifier.ABSTRACT)
        ) {
            val embeddedAnn = processingEnv.elementUtils.getAllAnnotationMirrors(returnType).firstOrNull { annotationMirror ->
                (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Embeddable")
            }
            if (embeddedAnn != null) {
                EmbeddableModel(processingEnv, getter, setter)
            } else {
                null
            }
        } else {
            null
        }
    }

    @Deprecated("DO NOT USE")
    fun generateCreate1(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} create${entityModel.simpleName}(${entityModel.dtoFullyQualifiedName} data) {")
        writer.newLine()
        writer.write("        ${entityModel.fullyQualifiedName} entity = new ${entityModel.fullyQualifiedName}();")
        val timestamps = entityModel.properties.filter { it.isColumn && it.isInsertable && it.isTimestamp }
        if (timestamps.isNotEmpty()) {
            writer.newLine()
            writer.write("        long now = System.currentTimeMillis();")
            for (timestamp in timestamps) {
                writer.newLine()
                writer.write("        entity.${timestamp.setter.simpleName}(now);")
            }
        }
        for ((index, property) in entityModel.properties.withIndex()) {
            if (property.isColumn && property.isInsertable && !property.isManaged) {
                writer.newLine()
                writer.write("        entity.${property.setter.simpleName}(data.${property.getter.simpleName}());")
            }
            if (property.isJoinColumn && property.isInsertable && !property.isManaged) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        ${property.getter.returnType.asString} relation${index} = entityManager.find(${property.getter.returnType.asString}.class, data.${property.getter.simpleName}${joinModel.capitalizedName}());")
                    if (property.isNotNull) {
                        writer.newLine()
                        writer.write("        if (relation${index} == null) {")
                        writer.newLine()
                        writer.write("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                        writer.newLine()
                        writer.write("        }")
                    }
                    writer.newLine()
                    writer.write("        entity.${property.setter.simpleName}(relation${index});")
                }
            }
        }
        writer.newLine()
        writer.write("        entityManager.persist(entity);")
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("        ${entityModel.dtoFullyQualifiedName} result = new ${entityModel.dtoFullyQualifiedName}();")
        for (property in entityModel.properties) {
            if (property.isColumn) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        if (entity.${property.getter.simpleName}() != null) {")
                    writer.newLine()
                    writer.write("            result.${property.setter.simpleName}${joinModel.capitalizedName}(entity.${property.getter.simpleName}().${joinModel.getter.simpleName}());")
                    writer.newLine()
                    writer.write("        }")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    @Deprecated("DO NOT USE")
    fun generateCreate2(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} create${entityModel.simpleName}(${getter.returnType.asString} id, ${entityModel.dtoFullyQualifiedName} data) {")
        writer.newLine()
        writer.write("        ${entityModel.fullyQualifiedName} entity = new ${entityModel.fullyQualifiedName}();")
        writer.newLine()
        writer.write("        entity.${setter.simpleName}(id);")
        val timestamps = entityModel.properties.filter { it.isColumn && it.isInsertable && it.isTimestamp }
        if (timestamps.isNotEmpty()) {
            writer.newLine()
            writer.write("        long now = System.currentTimeMillis();")
            for (timestamp in timestamps) {
                writer.newLine()
                writer.write("        entity.${timestamp.setter.simpleName}(now);")
            }
        }
        for ((index, property) in entityModel.properties.withIndex()) {
            if (property.isColumn && property.isInsertable && !property.isManaged) {
                writer.newLine()
                writer.write("        entity.${property.setter.simpleName}(data.${property.getter.simpleName}());")
            }
            if (property.isJoinColumn && property.isInsertable && !property.isManaged) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        ${property.getter.returnType.asString} relation${index} = entityManager.find(${property.getter.returnType.asString}.class, data.${property.getter.simpleName}${joinModel.capitalizedName}());")
                    if (property.isNotNull) {
                        writer.newLine()
                        writer.write("        if (relation${index} == null) {")
                        writer.newLine()
                        writer.write("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                        writer.newLine()
                        writer.write("        }")
                    }
                    writer.newLine()
                    writer.write("        entity.${property.setter.simpleName}(relation${index});")
                }
            }
        }
        writer.newLine()
        writer.write("        entityManager.persist(entity);")
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("        ${entityModel.dtoFullyQualifiedName} result = new ${entityModel.dtoFullyQualifiedName}();")
        for (property in entityModel.properties) {
            if (property.isColumn) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        if (entity.${property.getter.simpleName}() != null) {")
                    writer.newLine()
                    writer.write("            result.${property.setter.simpleName}${joinModel.capitalizedName}(entity.${property.getter.simpleName}().${joinModel.getter.simpleName}());")
                    writer.newLine()
                    writer.write("        }")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    @Deprecated("DO NOT USE")
    fun generateFindById(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} find${entityModel.simpleName}ById(${getter.returnType.asString} id) {")
        writer.newLine()
        writer.write("        return find${entityModel.simpleName}By${capitalizedName}(id, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} find${entityModel.simpleName}ById(${getter.returnType.asString} id, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        ${entityModel.fullyQualifiedName} entity;")
        writer.newLine()
        writer.write("        if (lockMode == null) {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.fullyQualifiedName}.class, id);")
        writer.newLine()
        writer.write("        } else {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.fullyQualifiedName}.class, id, lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (entity == null) {")
        writer.newLine()
        writer.write("            return null;")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        ${entityModel.dtoFullyQualifiedName} result = new ${entityModel.dtoFullyQualifiedName}();")
        for (property in entityModel.properties) {
            if (property.isColumn) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        if (entity.${property.getter.simpleName}() != null) {")
                    writer.newLine()
                    writer.write("            result.${property.setter.simpleName}${joinModel.capitalizedName}(entity.${property.getter.simpleName}().${joinModel.getter.simpleName}());")
                    writer.newLine()
                    writer.write("        }")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    @Deprecated("DO NOT USE")
    fun generateUpdateById(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} update${entityModel.simpleName}ById(${getter.returnType.asString} id, ${entityModel.dtoFullyQualifiedName} data) {")
        writer.newLine()
        writer.write("        return update${entityModel.simpleName}By${capitalizedName}(id, data, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} update${entityModel.simpleName}By${capitalizedName}(${getter.returnType.asString} id, ${entityModel.dtoFullyQualifiedName} data, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        ${entityModel.fullyQualifiedName} entity;")
        writer.newLine()
        writer.write("        if (lockMode == null) {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.fullyQualifiedName}.class, id);")
        writer.newLine()
        writer.write("        } else {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.fullyQualifiedName}.class, id, lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (entity == null) {")
        writer.newLine()
        writer.write("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException(\"Entity Not Found\");")
        writer.newLine()
        writer.write("        }")
        val timestamps = entityModel.properties.filter { it.isColumn && it.isUpdatable && it.isUpdateTimestamp }
        if (timestamps.isNotEmpty()) {
            writer.newLine()
            writer.write("        long now = System.currentTimeMillis();")
            for (timestamp in timestamps) {
                writer.newLine()
                writer.write("        entity.${timestamp.setter.simpleName}(now);")
            }
        }
        for ((index, property) in entityModel.properties.withIndex()) {
            if (property.isColumn && property.isUpdatable && !property.isManaged) {
                writer.newLine()
                writer.write("        entity.${property.setter.simpleName}(data.${property.getter.simpleName}());")
            }
            if (property.isJoinColumn && property.isUpdatable && !property.isManaged) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        ${property.getter.returnType.asString} relation${index} = entityManager.find(${property.getter.returnType.asString}.class, data.${property.getter.simpleName}${joinModel.capitalizedName}());")
                    if (property.isNotNull) {
                        writer.newLine()
                        writer.write("        if (relation${index} == null) {")
                        writer.newLine()
                        writer.write("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                        writer.newLine()
                        writer.write("        }")
                    }
                    writer.newLine()
                    writer.write("        entity.${property.setter.simpleName}(relation${index});")
                }
            }
        }
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("        ${entityModel.dtoFullyQualifiedName} result = new ${entityModel.dtoFullyQualifiedName}();")
        for (property in entityModel.properties) {
            if (property.isColumn) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        if (entity.${property.getter.simpleName}() != null) {")
                    writer.newLine()
                    writer.write("            result.${property.setter.simpleName}${joinModel.capitalizedName}(entity.${property.getter.simpleName}().${joinModel.getter.simpleName}());")
                    writer.newLine()
                    writer.write("        }")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    @Deprecated("DO NOT USE")
    fun generateDeleteById(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public void delete${entityModel.simpleName}ById(${getter.returnType.asString} id) {")
        writer.newLine()
        writer.write("        delete${entityModel.simpleName}By${capitalizedName}(id, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public void delete${entityModel.simpleName}ById(${getter.returnType.asString} id, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        ${entityModel.fullyQualifiedName} entity;")
        writer.newLine()
        writer.write("        if (lockMode == null) {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.fullyQualifiedName}.class, id);")
        writer.newLine()
        writer.write("        } else {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.fullyQualifiedName}.class, id, lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (entity == null) {")
        writer.newLine()
        writer.write("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException(\"Entity Not Found\");")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        entityManager.remove(entity);")
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("    }")
    }

    @Deprecated("DO NOT USE")
    fun generateFindByAltId(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} find${entityModel.simpleName}By${capitalizedName}(${getter.returnType.asString} altId) {")
        writer.newLine()
        writer.write("        return find${entityModel.simpleName}By${capitalizedName}(altId, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} find${entityModel.simpleName}By${capitalizedName}(${getter.returnType.asString} altId, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.fullyQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${entityModel.fullyQualifiedName}> root = criteriaQuery.from(${entityModel.fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        criteriaQuery.select(root);")
        writer.newLine()
        writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.fullyQualifiedName}_.${propertyName}), altId));")
        writer.newLine()
        writer.write("        javax.persistence.TypedQuery<${entityModel.fullyQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
        writer.newLine()
        writer.write("        typedQuery.setFirstResult(0);")
        writer.newLine()
        writer.write("        typedQuery.setMaxResults(1);")
        writer.newLine()
        writer.write("        if (lockMode != null) {")
        writer.newLine()
        writer.write("            typedQuery.setLockMode(lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        java.util.List<${entityModel.fullyQualifiedName}> list = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        if (list.isEmpty()) {")
        writer.newLine()
        writer.write("            return null;")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        ${entityModel.fullyQualifiedName} entity = list.get(0);")
        writer.newLine()
        writer.write("        ${entityModel.dtoFullyQualifiedName} result = new ${entityModel.dtoFullyQualifiedName}();")
        for (property in entityModel.properties) {
            if (property.isColumn) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        if (entity.${property.getter.simpleName}() != null) {")
                    writer.newLine()
                    writer.write("            result.${property.setter.simpleName}${joinModel.capitalizedName}(entity.${property.getter.simpleName}().${joinModel.getter.simpleName}());")
                    writer.newLine()
                    writer.write("        }")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    @Deprecated("DO NOT USE")
    fun generateUpdateByAltId(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} update${entityModel.simpleName}By${capitalizedName}(${getter.returnType.asString} altId, ${entityModel.dtoFullyQualifiedName} data) {")
        writer.newLine()
        writer.write("        return update${entityModel.simpleName}By${capitalizedName}(altId, data, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoFullyQualifiedName} update${entityModel.simpleName}By${capitalizedName}(${getter.returnType.asString} altId, ${entityModel.dtoFullyQualifiedName} data, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.fullyQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${entityModel.fullyQualifiedName}> root = criteriaQuery.from(${entityModel.fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        criteriaQuery.select(root);")
        writer.newLine()
        writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.fullyQualifiedName}_.${propertyName}), altId));")
        writer.newLine()
        writer.write("        javax.persistence.TypedQuery<${entityModel.fullyQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
        writer.newLine()
        writer.write("        typedQuery.setFirstResult(0);")
        writer.newLine()
        writer.write("        typedQuery.setMaxResults(1);")
        writer.newLine()
        writer.write("        if (lockMode != null) {")
        writer.newLine()
        writer.write("            typedQuery.setLockMode(lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        java.util.List<${entityModel.fullyQualifiedName}> list = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        if (list.isEmpty()) {")
        writer.newLine()
        writer.write("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException(\"Entity Not Found\");")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        ${entityModel.fullyQualifiedName} entity = list.get(0);")
        val timestamps = entityModel.properties.filter { it.isColumn && it.isUpdatable && it.isUpdateTimestamp }
        if (timestamps.isNotEmpty()) {
            writer.newLine()
            writer.write("        long now = System.currentTimeMillis();")
            for (timestamp in timestamps) {
                writer.newLine()
                writer.write("        entity.${timestamp.setter.simpleName}(now);")
            }
        }
        for ((index, property) in entityModel.properties.withIndex()) {
            if (property.isColumn && property.isUpdatable && !property.isManaged) {
                writer.newLine()
                writer.write("        entity.${property.setter.simpleName}(data.${property.getter.simpleName}());")
            }
            if (property.isJoinColumn && property.isUpdatable && !property.isManaged) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        ${property.getter.returnType.asString} relation${index} = entityManager.find(${property.getter.returnType.asString}.class, data.${property.getter.simpleName}${joinModel.capitalizedName}());")
                    if (property.isNotNull) {
                        writer.newLine()
                        writer.write("        if (relation${index} == null) {")
                        writer.newLine()
                        writer.write("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                        writer.newLine()
                        writer.write("        }")
                    }
                    writer.newLine()
                    writer.write("        entity.${property.setter.simpleName}(relation${index});")
                }
            }
        }
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("        ${entityModel.dtoFullyQualifiedName} result = new ${entityModel.dtoFullyQualifiedName}();")
        for (property in entityModel.properties) {
            if (property.isColumn) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("        if (entity.${property.getter.simpleName}() != null) {")
                    writer.newLine()
                    writer.write("            result.${property.setter.simpleName}${joinModel.capitalizedName}(entity.${property.getter.simpleName}().${joinModel.getter.simpleName}());")
                    writer.newLine()
                    writer.write("        }")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                writer.write("        result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    @Deprecated("DO NOT USE")
    fun generateDeleteByAltId(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public void delete${entityModel.simpleName}By${capitalizedName}(${getter.returnType.asString} altId) {")
        writer.newLine()
        writer.write("        delete${entityModel.simpleName}By${capitalizedName}(altId, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public void delete${entityModel.simpleName}By${capitalizedName}(${getter.returnType.asString} altId, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.fullyQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${entityModel.fullyQualifiedName}> root = criteriaQuery.from(${entityModel.fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        criteriaQuery.select(root);")
        writer.newLine()
        writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.fullyQualifiedName}_.${propertyName}), altId));")
        writer.newLine()
        writer.write("        javax.persistence.TypedQuery<${entityModel.fullyQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
        writer.newLine()
        writer.write("        typedQuery.setFirstResult(0);")
        writer.newLine()
        writer.write("        typedQuery.setMaxResults(1);")
        writer.newLine()
        writer.write("        if (lockMode != null) {")
        writer.newLine()
        writer.write("            typedQuery.setLockMode(lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        java.util.List<${entityModel.fullyQualifiedName}> list = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        if (list.isEmpty()) {")
        writer.newLine()
        writer.write("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException(\"Entity Not Found\");")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        ${entityModel.fullyQualifiedName} entity = list.get(0);")
        writer.newLine()
        writer.write("        entityManager.remove(entity);")
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("    }")
    }
}