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
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class EntityModel(private val processingEnv: ProcessingEnvironment, private val entityElement: Element) {

    val fullyQualifiedName: String by lazy {
        (entityElement as TypeElement).qualifiedName.toString()
    }

    val simpleName: String by lazy {
        (entityElement as TypeElement).simpleName.toString()
    }

    val crudFullyQualifiedName: String by lazy {
        val annotationMirror = processingEnv.elementUtils.getAllAnnotationMirrors(entityElement).first { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Crud")
        }
        annotationMirror.elementValues.values.first().value as String
    }

    val crudPackageName: String by lazy {
        crudFullyQualifiedName.substringBeforeLast('.', "")
    }

    val crudSimpleName: String by lazy {
        crudFullyQualifiedName.substringAfterLast('.')
    }

    val dtoFullyQualifiedName: String by lazy {
        val annotationMirror = processingEnv.elementUtils.getAllAnnotationMirrors(entityElement).first { annotationMirror ->
            (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Dto")
        }
        annotationMirror.elementValues.values.first().value as String
    }

    val dtoPackageName: String by lazy {
        dtoFullyQualifiedName.substringBeforeLast('.', "")
    }

    val dtoSimpleName: String by lazy {
        dtoFullyQualifiedName.substringAfterLast('.')
    }

    val properties: List<PropertyModel> by lazy {
        val executableElements = processingEnv.elementUtils.getAllMembers(entityElement as TypeElement).filterIsInstance<ExecutableElement>()
        val getters = executableElements.filter { executableElement ->
            val columnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(executableElement).firstOrNull { annotationMirror ->
                (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.Column")
            }
            val joinColumnAnn = processingEnv.elementUtils.getAllAnnotationMirrors(executableElement).firstOrNull { annotationMirror ->
                (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.JoinColumn")
            }
            val columnEmbId = processingEnv.elementUtils.getAllAnnotationMirrors(executableElement).firstOrNull { annotationMirror ->
                (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("javax.persistence.EmbeddedId")
            }
            (columnAnn != null || joinColumnAnn != null || columnEmbId != null)
        }
        getters.mapNotNull { getter ->
            val setter = executableElements.firstOrNull { executableElement ->
                executableElement.simpleName.contentEquals("set${getter.capitalizedName}")
            }
            if (setter != null) {
                PropertyModel(processingEnv, getter, setter)
            } else {
                null
            }
        }
    }

    fun generateCode() {
        processingEnv.filer.createSourceFile(crudFullyQualifiedName, entityElement).openWriter().buffered().use { writer ->
            generateCrudCode(writer)
        }
        processingEnv.filer.createSourceFile(dtoFullyQualifiedName, entityElement).openWriter().buffered().use { writer ->
            generateDtoCode(writer)
        }
    }

    private fun generateCrudCode(writer: BufferedWriter) {
        writer.write("// Origin: $fullyQualifiedName")
        if (crudPackageName.isNotBlank()) {
            writer.newLine()
            writer.newLine()
            writer.write("package ${crudPackageName};")
        }
        writer.newLine()
        writer.newLine()
        writer.write("public class $crudSimpleName {")
        writer.newLine()
        writer.newLine()
        writer.write("    protected javax.persistence.EntityManager entityManager;")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${crudSimpleName}(javax.persistence.EntityManager entityManager) {")
        writer.newLine()
        writer.write("        this.entityManager = entityManager;")
        writer.newLine()
        writer.write("    }")
        for (property in properties) {
            if (property.isId) {
                if (property.isGeneratedValue) {
                    property.generateCreate1(writer, this)
                } else {
                    property.generateCreate2(writer, this)
                }
            }
        }
        generateCount(writer)
        generateList(writer)
        for (property in properties) {
            if (property.isId) {
                property.generateFindById(writer, this)
                property.generateUpdateById(writer, this)
                property.generateDeleteById(writer, this)
            }
        }
        for (property in properties) {
            if (property.isAltId) {
                property.generateFindByAltId(writer, this)
                property.generateUpdateByAltId(writer, this)
                property.generateDeleteByAltId(writer, this)
            }
        }
        writer.newLine()
        writer.write("}")
    }

    private fun generateCount(writer: BufferedWriter) {
        writer.newLine()
        writer.newLine()
        writer.write("    public java.lang.Long count${simpleName}(java.util.function.Consumer<mx.com.inftel.codegen.data_access.CountContext<${fullyQualifiedName}>> consumer) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<java.lang.Long> criteriaQuery = criteriaBuilder.createQuery(java.lang.Long.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${fullyQualifiedName}> root = criteriaQuery.from(${fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        mx.com.inftel.codegen.data_access.CountContext<${fullyQualifiedName}> context = new mx.com.inftel.codegen.data_access.CountContext<>(criteriaBuilder, root);")
        writer.newLine()
        writer.write("        consumer.accept(context);")
        writer.newLine()
        writer.write("        criteriaQuery.select(criteriaBuilder.count(root));")
        writer.newLine()
        writer.write("        if (!context.getPredicates().isEmpty()) {")
        writer.newLine()
        writer.write("            criteriaQuery.where(context.getPredicates().toArray(new javax.persistence.criteria.Predicate[0]));")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        javax.persistence.TypedQuery<java.lang.Long> typedQuery = entityManager.createQuery(criteriaQuery);")
        writer.newLine()
        writer.write("        java.util.List<java.lang.Long> list = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        if (list.isEmpty()) {")
        writer.newLine()
        writer.write("            return null;")
        writer.newLine()
        writer.write("        } else {")
        writer.newLine()
        writer.write("            return list.get(0);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateList(writer: BufferedWriter) {
        writer.newLine()
        writer.newLine()
        writer.write("    public java.util.List<${dtoFullyQualifiedName}> list${simpleName}(java.util.function.Consumer<mx.com.inftel.codegen.data_access.ListContext<${fullyQualifiedName}>> consumer) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<${fullyQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${fullyQualifiedName}> root = criteriaQuery.from(${fullyQualifiedName}.class);")
        writer.newLine()
        writer.write("        mx.com.inftel.codegen.data_access.ListContext<${fullyQualifiedName}> context = new mx.com.inftel.codegen.data_access.ListContext<>(criteriaBuilder, root);")
        writer.newLine()
        writer.write("        consumer.accept(context);")
        writer.newLine()
        writer.write("        criteriaQuery.select(root);")
        writer.newLine()
        writer.write("        if (!context.getPredicates().isEmpty()) {")
        writer.newLine()
        writer.write("            criteriaQuery.where(context.getPredicates().toArray(new javax.persistence.criteria.Predicate[0]));")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (!context.getOrders().isEmpty()) {")
        writer.newLine()
        writer.write("            criteriaQuery.orderBy(context.getOrders().toArray(new javax.persistence.criteria.Order[0]));")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        javax.persistence.TypedQuery<${fullyQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
        writer.newLine()
        writer.write("        if (context.getFirstResult() >= 0) {")
        writer.newLine()
        writer.write("            typedQuery.setFirstResult(context.getFirstResult());")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (context.getMaxResults() >= 0) {")
        writer.newLine()
        writer.write("            typedQuery.setMaxResults(context.getMaxResults());")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (context.getLockMode() != null) {")
        writer.newLine()
        writer.write("            typedQuery.setLockMode(context.getLockMode());")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        java.util.List<${fullyQualifiedName}> list1 = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        java.util.ArrayList<${dtoFullyQualifiedName}> list2 = new java.util.ArrayList<>();")
        writer.newLine()
        writer.write("        list2.ensureCapacity(list1.size());")
        writer.newLine()
        writer.write("        for (${fullyQualifiedName} entity: list1) {")
        writer.newLine()
        writer.write("            $dtoFullyQualifiedName result = new ${dtoFullyQualifiedName}();")
        for (property in properties) {
            if (property.isColumn) {
                writer.newLine()
                writer.write("            result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.write("            if (entity.${property.getter.simpleName}() != null) {")
                    writer.newLine()
                    writer.write("                result.${property.setter.simpleName}${joinModel.capitalizedName}(entity.${property.getter.simpleName}().${joinModel.getter.simpleName}());")
                    writer.newLine()
                    writer.write("            }")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                writer.write("            result.${property.setter.simpleName}(entity.${property.getter.simpleName}());")
            }
        }
        writer.newLine()
        writer.write("            list2.add(result);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        return list2;")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateDtoCode(writer: BufferedWriter) {
        writer.write("// Origin: $fullyQualifiedName")
        if (dtoPackageName.isNotBlank()) {
            writer.newLine()
            writer.newLine()
            writer.write("package ${dtoPackageName};")
        }
        writer.newLine()
        writer.newLine()
        writer.write("public class $dtoSimpleName implements java.io.Serializable {")
        //
        for (property in properties) {
            if (property.isColumn) {
                if (property.isNotNull) {
                    when (val returnTypeAsString = property.getter.returnType.asString) {
                        "byte" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private byte ${property.propertyName} = 0;")
                        }
                        "char" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private char ${property.propertyName} = ' ';")
                        }
                        "double" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private double ${property.propertyName} = 0.0;")
                        }
                        "float" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private float ${property.propertyName} = 0.0f;")
                        }
                        "int" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private int ${property.propertyName} = 0;")
                        }
                        "long" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private long ${property.propertyName} = 0;")
                        }
                        "short" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private short ${property.propertyName} = 0;")
                        }
                        "boolean" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private boolean ${property.propertyName} = false;")
                        }
                        "java.lang.Byte" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Byte ${property.propertyName} = 0;")
                        }
                        "java.lang.Char" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Char ${property.propertyName} = ' ';")
                        }
                        "java.lang.Double" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Double ${property.propertyName} = 0.0;")
                        }
                        "java.lang.Float" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Float ${property.propertyName} = 0.0f;")
                        }
                        "java.lang.Integer" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Integer ${property.propertyName} = 0;")
                        }
                        "java.lang.Long" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Long ${property.propertyName} = 0;")
                        }
                        "java.lang.Short" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Short ${property.propertyName} = 0;")
                        }
                        "java.lang.Boolean" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Boolean ${property.propertyName} = false;")
                        }
                        "java.lang.String" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.String ${property.propertyName} = \"\";")
                        }
                        else -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private $returnTypeAsString ${property.propertyName};")
                        }
                    }
                } else {
                    writer.newLine()
                    writer.newLine()
                    writer.write("    private ${property.getter.returnType.asString} ${property.propertyName};")
                }
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    writer.newLine()
                    writer.write("    private ${joinModel.getter.returnType.asString} ${property.propertyName}${joinModel.capitalizedName};")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                writer.newLine()
                writer.write("    private ${property.getter.returnType.asString} ${property.propertyName};")
            }
        }
        //
        for (property in properties) {
            if (property.isColumn) {
                writer.newLine()
                generateValidations(writer, property.validations)
                writer.newLine()
                writer.write("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${property.propertyName}\")")
                writer.newLine()
                writer.write("    public ${property.getter.returnType.asString} ${property.getter.simpleName}() {")
                writer.newLine()
                writer.write("        return this.${property.propertyName};")
                writer.newLine()
                writer.write("    }")
                //
                writer.newLine()
                writer.newLine()
                writer.write("    public void ${property.setter.simpleName}(${property.getter.returnType.asString} ${property.propertyName}) {")
                writer.newLine()
                writer.write("        this.${property.propertyName} = ${property.propertyName};")
                writer.newLine()
                writer.write("    }")
            } else if (property.isJoinColumn) {
                val joinModel = property.joinModel
                if (joinModel != null) {
                    writer.newLine()
                    generateValidations(writer, property.validations)
                    writer.newLine()
                    writer.write("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${property.propertyName}.${joinModel.propertyName}\")")
                    writer.newLine()
                    writer.write("    public ${joinModel.getter.returnType.asString} ${property.getter.simpleName}${joinModel.capitalizedName}() {")
                    writer.newLine()
                    writer.write("        return this.${property.propertyName}${joinModel.capitalizedName};")
                    writer.newLine()
                    writer.write("    }")
                    //
                    writer.newLine()
                    writer.newLine()
                    writer.write("    public void ${property.setter.simpleName}${joinModel.capitalizedName}(${joinModel.getter.returnType.asString} ${property.propertyName}${joinModel.capitalizedName}) {")
                    writer.newLine()
                    writer.write("        this.${property.propertyName}${joinModel.capitalizedName} = ${property.propertyName}${joinModel.capitalizedName};")
                    writer.newLine()
                    writer.write("    }")
                }
            } else if (property.isEmbeddedId) {
                writer.newLine()
                generateValidations(writer, property.validations)
                writer.newLine()
                writer.write("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${property.propertyName}\")")
                writer.newLine()
                writer.write("    public ${property.getter.returnType.asString} ${property.getter.simpleName}() {")
                writer.newLine()
                writer.write("        return this.${property.propertyName};")
                writer.newLine()
                writer.write("    }")
                //
                writer.newLine()
                writer.newLine()
                writer.write("    public void ${property.setter.simpleName}(${property.getter.returnType.asString} ${property.propertyName}) {")
                writer.newLine()
                writer.write("        this.${property.propertyName} = ${property.propertyName};")
                writer.newLine()
                writer.write("    }")
            }
        }
        //
        writer.newLine()
        writer.write("}")
    }

    private fun generateValidations(writer: BufferedWriter, validations: List<AnnotationMirror>) {
        for (validation in validations) {
            val annotationElement = validation.annotationType.asElement() as TypeElement
            writer.newLine()
            writer.write("    @${annotationElement.qualifiedName}")
            if (validation.elementValues.isNotEmpty()) {
                writer.write("(")
                var first = true
                for ((key, value) in validation.elementValues) {
                    if (first) {
                        first = false
                    } else {
                        writer.write(", ")
                    }
                    writer.write("${key.simpleName} = $value")
                }
                writer.write(")")
            }
        }
    }
}