@file:Suppress("DuplicatedCode")

package mx.com.inftel.codegen.apt.data_access

import java.beans.Introspector
import java.io.BufferedWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class DataAccessCodegen : AbstractProcessor() {

    private val entities = mutableMapOf<String, EntityModel>()

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        try {
            val annotatedElements = roundEnv.getElementsAnnotatedWithAny(*annotations.toTypedArray())
            for (annotatedElement in annotatedElements) {
                if (annotatedElement is TypeElement && annotatedElement.isEntity) {
                    val entityModel = entities.getOrPut(annotatedElement.qualifiedName.toString()) { EntityModel().also { it.fillEntityModel(annotatedElement) } }
                    if (entityModel.isGeneratedCodeCrud) {
                        processingEnv.filer.createSourceFile(entityModel.crudQualifiedName, annotatedElement).openWriter().buffered().use { writer ->
                            generateCrudCode(writer, entityModel)
                        }
                    }
                    if (entityModel.isGeneratedCodeDto) {
                        processingEnv.filer.createSourceFile(entityModel.dtoQualifiedName, annotatedElement).openWriter().buffered().use { writer ->
                            generateDtoCode(writer, entityModel)
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, ex.stackTraceToString())
        }
        return false
    }

    private fun generateCrudCode(writer: BufferedWriter, entityModel: EntityModel) {
        writer.write("// Origin: ${entityModel.entityQualifiedName}")
        if (entityModel.crudPackageName.isNotBlank()) {
            writer.newLine()
            writer.newLine()
            writer.write("package ${entityModel.crudPackageName};")
        }
        writer.newLine()
        writer.newLine()
        writer.write("public class ${entityModel.crudSimpleName} {")
        writer.newLine()
        writer.newLine()
        writer.write("    protected javax.persistence.EntityManager entityManager;")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.crudSimpleName}(javax.persistence.EntityManager entityManager) {")
        writer.newLine()
        writer.write("        this.entityManager = entityManager;")
        writer.newLine()
        writer.write("    }")
        //
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn && propertyModel.isId) {
                if (propertyModel.isGeneratedValue) {
                    generateCrudCreate(writer, entityModel)
                } else {
                    generateCrudCreate(writer, entityModel, propertyModel)
                }
            }
        }
        generateCrudCount(writer, entityModel)
        generateCrudList(writer, entityModel)
        //
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn && propertyModel.isId) {
                generateCrudFindById(writer, entityModel, propertyModel)
                generateCrudUpdateById(writer, entityModel, propertyModel)
                generateCrudDeleteById(writer, entityModel, propertyModel)
            }
            if (propertyModel.isGeneratedCode && propertyModel.isColumn && propertyModel.isAltId) {
                generateCrudFindByAltId(writer, entityModel, propertyModel)
                generateCrudUpdateByAltId(writer, entityModel, propertyModel)
                generateCrudDeleteByAltId(writer, entityModel, propertyModel)
            }
        }
        //
        writer.newLine()
        writer.write("}")
    }

    private fun generateCrudCreate(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} create${entityModel.entitySimpleName}(${entityModel.dtoQualifiedName} dto) {")
        writer.newLine()
        writer.write("        ${entityModel.entityQualifiedName} entity = new ${entityModel.entityQualifiedName}();")
        val timestamps = entityModel.properties.values.filter { it.isTimestamp && it.isGeneratedCode && it.isColumn && it.isInsertable }
        if (timestamps.isNotEmpty()) {
            writer.newLine()
            writer.write("        long now = System.currentTimeMillis();")
            for (timestamp in timestamps) {
                writer.newLine()
                writer.write("        entity.${timestamp.propertySetter.simpleName}(now);")
            }
        }
        for ((index, propertyModel) in entityModel.properties.values.withIndex()) {
            if (!propertyModel.isManaged && propertyModel.isGeneratedCode && propertyModel.isColumn && propertyModel.isInsertable) {
                writer.newLine()
                writer.write("        entity.${propertyModel.propertySetter.simpleName}(dto.${propertyModel.propertyGetter.simpleName}());")
            }
            if (!propertyModel.isManaged && propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.isInsertable && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        ${propertyModel.propertyGetter.returnType.asString} relation${index} = entityManager.find(${propertyModel.propertyGetter.returnType.asString}.class, dto.${propertyModel.propertyGetter.simpleName}${joinIdModel.capitalizedName}());")
                    writer.newLine()
                    writer.write("        if (relation${index} == null) {")
                    writer.newLine()
                    writer.write("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException();")
                    writer.newLine()
                    writer.write("        }")
                    writer.newLine()
                    writer.write("        entity.${propertyModel.propertySetter.simpleName}(relation${index});")
                }
            }
        }
        writer.newLine()
        writer.write("        entityManager.persist(entity);")
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("        ${entityModel.dtoQualifiedName} result = new ${entityModel.dtoQualifiedName}();")
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                writer.newLine()
                writer.write("        result.${propertyModel.propertySetter.simpleName}(entity.${propertyModel.propertyGetter.simpleName}());")
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        result.${propertyModel.propertySetter.simpleName}${joinIdModel.capitalizedName}(entity.${propertyModel.propertyGetter.simpleName}().${joinIdModel.propertyGetter.simpleName}());")
                }
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateCrudCreate(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} create${entityModel.entitySimpleName}(${idModel.propertyGetter.returnType.asString} id, ${entityModel.dtoQualifiedName} dto) {")
        writer.newLine()
        writer.write("        ${entityModel.entityQualifiedName} entity = new ${entityModel.entityQualifiedName}();")
        writer.newLine()
        writer.write("        entity.${idModel.propertySetter.simpleName}(id);")
        val timestamps = entityModel.properties.values.filter { it.isTimestamp && it.isGeneratedCode && it.isColumn && it.isInsertable }
        if (timestamps.isNotEmpty()) {
            writer.newLine()
            writer.write("        long now = System.currentTimeMillis();")
            for (timestamp in timestamps) {
                writer.newLine()
                writer.write("        entity.${timestamp.propertySetter.simpleName}(now);")
            }
        }
        for ((index, propertyModel) in entityModel.properties.values.withIndex()) {
            if (!propertyModel.isManaged && propertyModel.isGeneratedCode && propertyModel.isColumn && propertyModel.isInsertable) {
                writer.newLine()
                writer.write("        entity.${propertyModel.propertySetter.simpleName}(dto.${propertyModel.propertyGetter.simpleName}());")
            }
            if (!propertyModel.isManaged && propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.isInsertable && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        ${propertyModel.propertyGetter.returnType.asString} relation${index} = entityManager.find(${propertyModel.propertyGetter.returnType.asString}.class, dto.${propertyModel.propertyGetter.simpleName}${joinIdModel.capitalizedName}());")
                    writer.newLine()
                    writer.write("        if (relation${index} == null) {")
                    writer.newLine()
                    writer.write("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException();")
                    writer.newLine()
                    writer.write("        }")
                    writer.newLine()
                    writer.write("        entity.${propertyModel.propertySetter.simpleName}(relation${index});")
                }
            }
        }
        writer.newLine()
        writer.write("        entityManager.persist(entity);")
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("        ${entityModel.dtoQualifiedName} result = new ${entityModel.dtoQualifiedName}();")
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                writer.newLine()
                writer.write("        result.${propertyModel.propertySetter.simpleName}(entity.${propertyModel.propertyGetter.simpleName}());")
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        result.${propertyModel.propertySetter.simpleName}${joinIdModel.capitalizedName}(entity.${propertyModel.propertyGetter.simpleName}().${joinIdModel.propertyGetter.simpleName}());")
                }
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateCrudCount(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public java.lang.Long count${entityModel.entitySimpleName}(java.util.function.Consumer<mx.com.inftel.codegen.data_access.CountContext<${entityModel.entityQualifiedName}>> consumer) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<java.lang.Long> criteriaQuery = criteriaBuilder.createQuery(java.lang.Long.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${entityModel.entityQualifiedName}> root = criteriaQuery.from(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        mx.com.inftel.codegen.data_access.CountContext<${entityModel.entityQualifiedName}> context = new mx.com.inftel.codegen.data_access.CountContext<>(criteriaBuilder, root);")
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

    private fun generateCrudList(writer: BufferedWriter, entityModel: EntityModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public java.util.List<${entityModel.dtoQualifiedName}> list${entityModel.entitySimpleName}(java.util.function.Consumer<mx.com.inftel.codegen.data_access.ListContext<${entityModel.entityQualifiedName}>> consumer) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.entityQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${entityModel.entityQualifiedName}> root = criteriaQuery.from(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        mx.com.inftel.codegen.data_access.ListContext<${entityModel.entityQualifiedName}> context = new mx.com.inftel.codegen.data_access.ListContext<>(criteriaBuilder, root);")
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
        writer.write("        javax.persistence.TypedQuery<${entityModel.entityQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
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
        writer.write("        java.util.List<${entityModel.entityQualifiedName}> list1 = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        java.util.ArrayList<${entityModel.dtoQualifiedName}> list2 = new java.util.ArrayList<>();")
        writer.newLine()
        writer.write("        list2.ensureCapacity(list1.size());")
        writer.newLine()
        writer.write("        for (${entityModel.entityQualifiedName} entity: list1) {")
        writer.newLine()
        writer.write("            ${entityModel.dtoQualifiedName} result = new ${entityModel.dtoQualifiedName}();")
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                writer.newLine()
                writer.write("            result.${propertyModel.propertySetter.simpleName}(entity.${propertyModel.propertyGetter.simpleName}());")
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("            result.${propertyModel.propertySetter.simpleName}${joinIdModel.capitalizedName}(entity.${propertyModel.propertyGetter.simpleName}().${joinIdModel.propertyGetter.simpleName}());")
                }
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

    private fun generateCrudFindById(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} find${entityModel.entitySimpleName}ById(${idModel.propertyGetter.returnType.asString} id) {")
        writer.newLine()
        writer.write("        return find${entityModel.entitySimpleName}By${idModel.capitalizedName}(id, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} find${entityModel.entitySimpleName}ById(${idModel.propertyGetter.returnType.asString} id, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        ${entityModel.entityQualifiedName} entity;")
        writer.newLine()
        writer.write("        if (lockMode == null) {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.entityQualifiedName}.class, id);")
        writer.newLine()
        writer.write("        } else {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.entityQualifiedName}.class, id, lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (entity == null) {")
        writer.newLine()
        writer.write("            return null;")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        ${entityModel.dtoQualifiedName} result = new ${entityModel.dtoQualifiedName}();")
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                writer.newLine()
                writer.write("        result.${propertyModel.propertySetter.simpleName}(entity.${propertyModel.propertyGetter.simpleName}());")
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        result.${propertyModel.propertySetter.simpleName}${joinIdModel.capitalizedName}(entity.${propertyModel.propertyGetter.simpleName}().${joinIdModel.propertyGetter.simpleName}());")
                }
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateCrudFindByAltId(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} find${entityModel.entitySimpleName}By${idModel.capitalizedName}(${idModel.propertyGetter.returnType.asString} altId) {")
        writer.newLine()
        writer.write("        return find${entityModel.entitySimpleName}By${idModel.capitalizedName}(altId, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} find${entityModel.entitySimpleName}By${idModel.capitalizedName}(${idModel.propertyGetter.returnType.asString} altId, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.entityQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${entityModel.entityQualifiedName}> root = criteriaQuery.from(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        criteriaQuery.select(root);")
        writer.newLine()
        writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.entityQualifiedName}_.${idModel.propertyName}), altId));")
        writer.newLine()
        writer.write("        javax.persistence.TypedQuery<${entityModel.entityQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
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
        writer.write("        java.util.List<${entityModel.entityQualifiedName}> list = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        if (list.isEmpty()) {")
        writer.newLine()
        writer.write("            return null;")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        ${entityModel.entityQualifiedName} entity = list.get(0);")
        writer.newLine()
        writer.write("        ${entityModel.dtoQualifiedName} result = new ${entityModel.dtoQualifiedName}();")
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                writer.newLine()
                writer.write("        result.${propertyModel.propertySetter.simpleName}(entity.${propertyModel.propertyGetter.simpleName}());")
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        result.${propertyModel.propertySetter.simpleName}${joinIdModel.capitalizedName}(entity.${propertyModel.propertyGetter.simpleName}().${joinIdModel.propertyGetter.simpleName}());")
                }
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateCrudUpdateById(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} update${entityModel.entitySimpleName}ById(${idModel.propertyGetter.returnType.asString} id, ${entityModel.dtoQualifiedName} dto) {")
        writer.newLine()
        writer.write("        return update${entityModel.entitySimpleName}By${idModel.capitalizedName}(id, dto, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} update${entityModel.entitySimpleName}By${idModel.capitalizedName}(${idModel.propertyGetter.returnType.asString} id, ${entityModel.dtoQualifiedName} dto, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        ${entityModel.entityQualifiedName} entity;")
        writer.newLine()
        writer.write("        if (lockMode == null) {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.entityQualifiedName}.class, id);")
        writer.newLine()
        writer.write("        } else {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.entityQualifiedName}.class, id, lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (entity == null) {")
        writer.newLine()
        writer.write("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException();")
        writer.newLine()
        writer.write("        }")
        val timestamps = entityModel.properties.values.filter { it.isUpdateTimestamp && it.isGeneratedCode && it.isColumn && it.isUpdatable }
        if (timestamps.isNotEmpty()) {
            writer.newLine()
            writer.write("        long now = System.currentTimeMillis();")
            for (timestamp in timestamps) {
                writer.newLine()
                writer.write("        entity.${timestamp.propertySetter.simpleName}(now);")
            }
        }
        for ((index, propertyModel) in entityModel.properties.values.withIndex()) {
            if (!propertyModel.isManaged && propertyModel.isGeneratedCode && propertyModel.isColumn && propertyModel.isUpdatable) {
                writer.newLine()
                writer.write("        entity.${propertyModel.propertySetter.simpleName}(dto.${propertyModel.propertyGetter.simpleName}());")
            }
            if (!propertyModel.isManaged && propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.isUpdatable && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        ${propertyModel.propertyGetter.returnType.asString} relation${index} = entityManager.find(${propertyModel.propertyGetter.returnType.asString}.class, dto.${propertyModel.propertyGetter.simpleName}${joinIdModel.capitalizedName}());")
                    writer.newLine()
                    writer.write("        if (relation${index} == null) {")
                    writer.newLine()
                    writer.write("                throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException();")
                    writer.newLine()
                    writer.write("        }")
                    writer.newLine()
                    writer.write("        entity.${propertyModel.propertySetter.simpleName}(relation${index});")
                }
            }
        }
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("        ${entityModel.dtoQualifiedName} result = new ${entityModel.dtoQualifiedName}();")
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                writer.newLine()
                writer.write("        result.${propertyModel.propertySetter.simpleName}(entity.${propertyModel.propertyGetter.simpleName}());")
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        result.${propertyModel.propertySetter.simpleName}${joinIdModel.capitalizedName}(entity.${propertyModel.propertyGetter.simpleName}().${joinIdModel.propertyGetter.simpleName}());")
                }
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateCrudUpdateByAltId(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} update${entityModel.entitySimpleName}By${idModel.capitalizedName}(${idModel.propertyGetter.returnType.asString} altId, ${entityModel.dtoQualifiedName} dto) {")
        writer.newLine()
        writer.write("        return update${entityModel.entitySimpleName}By${idModel.capitalizedName}(altId, dto, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public ${entityModel.dtoQualifiedName} update${entityModel.entitySimpleName}By${idModel.capitalizedName}(${idModel.propertyGetter.returnType.asString} altId, ${entityModel.dtoQualifiedName} dto, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.entityQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${entityModel.entityQualifiedName}> root = criteriaQuery.from(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        criteriaQuery.select(root);")
        writer.newLine()
        writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.entityQualifiedName}_.${idModel.propertyName}), altId));")
        writer.newLine()
        writer.write("        javax.persistence.TypedQuery<${entityModel.entityQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
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
        writer.write("        java.util.List<${entityModel.entityQualifiedName}> list = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        if (list.isEmpty()) {")
        writer.newLine()
        writer.write("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException();")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        ${entityModel.entityQualifiedName} entity = list.get(0);")
        val timestamps = entityModel.properties.values.filter { it.isUpdateTimestamp && it.isGeneratedCode && it.isColumn && it.isUpdatable }
        if (timestamps.isNotEmpty()) {
            writer.newLine()
            writer.write("        long now = System.currentTimeMillis();")
            for (timestamp in timestamps) {
                writer.newLine()
                writer.write("        entity.${timestamp.propertySetter.simpleName}(now);")
            }
        }
        for ((index, propertyModel) in entityModel.properties.values.withIndex()) {
            if (!propertyModel.isManaged && propertyModel.isGeneratedCode && propertyModel.isColumn && propertyModel.isUpdatable) {
                writer.newLine()
                writer.write("        entity.${propertyModel.propertySetter.simpleName}(dto.${propertyModel.propertyGetter.simpleName}());")
            }
            if (!propertyModel.isManaged && propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.isUpdatable && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        ${propertyModel.propertyGetter.returnType.asString} relation${index} = entityManager.find(${propertyModel.propertyGetter.returnType.asString}.class, dto.${propertyModel.propertyGetter.simpleName}${joinIdModel.capitalizedName}());")
                    writer.newLine()
                    writer.write("        if (relation${index} == null) {")
                    writer.newLine()
                    writer.write("                throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException();")
                    writer.newLine()
                    writer.write("        }")
                    writer.newLine()
                    writer.write("        entity.${propertyModel.propertySetter.simpleName}(relation${index});")
                }
            }
        }
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("        ${entityModel.dtoQualifiedName} result = new ${entityModel.dtoQualifiedName}();")
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                writer.newLine()
                writer.write("        result.${propertyModel.propertySetter.simpleName}(entity.${propertyModel.propertyGetter.simpleName}());")
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.write("        result.${propertyModel.propertySetter.simpleName}${joinIdModel.capitalizedName}(entity.${propertyModel.propertyGetter.simpleName}().${joinIdModel.propertyGetter.simpleName}());")
                }
            }
        }
        writer.newLine()
        writer.write("        return result;")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateCrudDeleteById(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public void delete${entityModel.entitySimpleName}ById(${idModel.propertyGetter.returnType.asString} id) {")
        writer.newLine()
        writer.write("        delete${entityModel.entitySimpleName}By${idModel.capitalizedName}(${idModel.propertyName}, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public void delete${entityModel.entitySimpleName}ById(${idModel.propertyGetter.returnType.asString} id, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        ${entityModel.entityQualifiedName} entity;")
        writer.newLine()
        writer.write("        if (lockMode == null) {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.entityQualifiedName}.class, id);")
        writer.newLine()
        writer.write("        } else {")
        writer.newLine()
        writer.write("            entity = entityManager.find(${entityModel.entityQualifiedName}.class, id, lockMode);")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        if (entity == null) {")
        writer.newLine()
        writer.write("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException();")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        entityManager.remove(entity);")
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateCrudDeleteByAltId(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
        writer.newLine()
        writer.newLine()
        writer.write("    public void delete${entityModel.entitySimpleName}By${idModel.capitalizedName}(${idModel.propertyGetter.returnType.asString} altId) {")
        writer.newLine()
        writer.write("        delete${entityModel.entitySimpleName}By${idModel.capitalizedName}(altId, null);")
        writer.newLine()
        writer.write("    }")
        writer.newLine()
        writer.newLine()
        writer.write("    public void delete${entityModel.entitySimpleName}By${idModel.capitalizedName}(${idModel.propertyGetter.returnType.asString} altId, javax.persistence.LockModeType lockMode) {")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
        writer.newLine()
        writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.entityQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        javax.persistence.criteria.Root<${entityModel.entityQualifiedName}> root = criteriaQuery.from(${entityModel.entityQualifiedName}.class);")
        writer.newLine()
        writer.write("        criteriaQuery.select(root);")
        writer.newLine()
        writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.entityQualifiedName}_.${idModel.propertyName}), altId));")
        writer.newLine()
        writer.write("        javax.persistence.TypedQuery<${entityModel.entityQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
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
        writer.write("        java.util.List<${entityModel.entityQualifiedName}> list = typedQuery.getResultList();")
        writer.newLine()
        writer.write("        if (list.isEmpty()) {")
        writer.newLine()
        writer.write("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException();")
        writer.newLine()
        writer.write("        }")
        writer.newLine()
        writer.write("        ${entityModel.entityQualifiedName} entity = list.get(0);")
        writer.newLine()
        writer.write("        entityManager.remove(entity);")
        writer.newLine()
        writer.write("        entityManager.flush();")
        writer.newLine()
        writer.write("    }")
    }

    private fun generateDtoCode(writer: BufferedWriter, entityModel: EntityModel) {
        writer.write("// Origin: ${entityModel.entityQualifiedName}")
        if (entityModel.dtoPackageName.isNotBlank()) {
            writer.newLine()
            writer.newLine()
            writer.write("package ${entityModel.dtoPackageName};")
        }
        writer.newLine()
        writer.newLine()
        writer.write("public class ${entityModel.dtoSimpleName} implements java.io.Serializable {")
        //
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                if (propertyModel.isNotNull) {
                    when (val returnTypeAsString = propertyModel.propertyGetter.returnType.asString) {
                        "java.lang.Byte" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Byte ${propertyModel.propertyName} = 0;")
                        }
                        "java.lang.Char" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Char ${propertyModel.propertyName} = ' ';")
                        }
                        "java.lang.Double" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Double ${propertyModel.propertyName} = 0.0;")
                        }
                        "java.lang.Float" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Float ${propertyModel.propertyName} = 0.0f;")
                        }
                        "java.lang.Integer" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Integer ${propertyModel.propertyName} = 0;")
                        }
                        "java.lang.Long" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Long ${propertyModel.propertyName} = 0L;")
                        }
                        "java.lang.Short" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Short ${propertyModel.propertyName} = 0;")
                        }
                        "java.lang.Boolean" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.Boolean ${propertyModel.propertyName} = false;")
                        }
                        "java.lang.String" -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private java.lang.String ${propertyModel.propertyName} = \"\";")
                        }
                        else -> {
                            writer.newLine()
                            writer.newLine()
                            writer.write("    private $returnTypeAsString ${propertyModel.propertyName};")
                        }
                    }
                } else {
                    writer.newLine()
                    writer.newLine()
                    writer.write("    private ${propertyModel.propertyGetter.returnType.asString} ${propertyModel.propertyName};")
                }
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    writer.newLine()
                    writer.write("    private ${joinIdModel.propertyGetter.returnType.asString} ${propertyModel.propertyName}${joinIdModel.capitalizedName};")
                }
            }
        }
        //
        for (propertyModel in entityModel.properties.values) {
            if (propertyModel.isGeneratedCode && propertyModel.isColumn) {
                writer.newLine()
                generateDtoValidations(writer, propertyModel.validations)
                writer.newLine()
                writer.write("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${propertyModel.propertyName}\")")
                writer.newLine()
                writer.write("    public ${propertyModel.propertyGetter.returnType.asString} ${propertyModel.propertyGetter.simpleName}() {")
                writer.newLine()
                writer.write("        return this.${propertyModel.propertyName};")
                writer.newLine()
                writer.write("    }")
                //
                writer.newLine()
                writer.newLine()
                writer.write("    public void ${propertyModel.propertySetter.simpleName}(${propertyModel.propertyGetter.returnType.asString} ${propertyModel.propertyName}) {")
                writer.newLine()
                writer.write("        this.${propertyModel.propertyName} = ${propertyModel.propertyName};")
                writer.newLine()
                writer.write("    }")
            }
            if (propertyModel.isGeneratedCode && propertyModel.isJoinColumn && propertyModel.joinModel != null) {
                val joinIdModel = propertyModel.joinModel!!.properties.values.find { it.isId }
                if (joinIdModel != null && joinIdModel.isGeneratedCode) {
                    writer.newLine()
                    generateDtoValidations(writer, propertyModel.validations)
                    writer.newLine()
                    writer.write("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${propertyModel.propertyName}.${joinIdModel.propertyName}\")")
                    writer.newLine()
                    writer.write("    public ${joinIdModel.propertyGetter.returnType.asString} ${propertyModel.propertyGetter.simpleName}${joinIdModel.capitalizedName}() {")
                    writer.newLine()
                    writer.write("        return this.${propertyModel.propertyName}${joinIdModel.capitalizedName};")
                    writer.newLine()
                    writer.write("    }")
                    //
                    writer.newLine()
                    writer.newLine()
                    writer.write("    public void ${propertyModel.propertySetter.simpleName}${joinIdModel.capitalizedName}(${joinIdModel.propertyGetter.returnType.asString} ${propertyModel.propertyName}${joinIdModel.capitalizedName}) {")
                    writer.newLine()
                    writer.write("        this.${propertyModel.propertyName}${joinIdModel.capitalizedName} = ${propertyModel.propertyName}${joinIdModel.capitalizedName};")
                    writer.newLine()
                    writer.write("    }")
                }
            }
        }
        //
        writer.newLine()
        writer.write("}")
    }

    private fun generateDtoValidations(writer: BufferedWriter, validations: List<AnnotationMirror>) {
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

    private fun EntityModel.fillEntityModel(typeElement: TypeElement) {
        entityQualifiedName = typeElement.qualifiedName.toString()
        //
        val entityAnnotations = processingEnv.elementUtils.getAllAnnotationMirrors(typeElement)
        //
        val crudAnnotation = entityAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Crud")
        }
        if (crudAnnotation != null) {
            crudQualifiedName = crudAnnotation.elementValues.values.first().value as String
        }
        //
        val dtoAnnotation = entityAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.Dto")
        }
        if (dtoAnnotation != null) {
            dtoQualifiedName = dtoAnnotation.elementValues.values.first().value as String
        }
        //
        for (elementMember in processingEnv.elementUtils.getAllMembers(typeElement)) {
            if (elementMember is ExecutableElement && !elementMember.isProhibited) {
                if (elementMember.isGetter) {
                    properties.getOrPut(elementMember.propertyName) { PropertyModel() }.fillPropertyModel(elementMember)
                }
                if (elementMember.isSetter) {
                    properties.getOrPut(elementMember.propertyName) { PropertyModel() }.propertySetter = elementMember
                }
            }
        }
    }

    private fun PropertyModel.fillPropertyModel(getter: ExecutableElement) {
        if (!this.isGetterInitialized) {
            this.propertyGetter = getter
            this.capitalizedName = getter.capitalizedName
            this.propertyName = getter.propertyName
        }
        //
        val getterAnnotations = processingEnv.elementUtils.getAllAnnotationMirrors(getter)
        //
        val columnAnnotation = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("javax.persistence.Column")
        }
        if (columnAnnotation != null) {
            isColumn = true
            val insertableKey = columnAnnotation.elementValues.keys.firstOrNull { it.simpleName.contentEquals("insertable") }
            if (insertableKey != null) {
                isInsertable = columnAnnotation.elementValues[insertableKey]!!.value as Boolean
            }
            val updatableKey = columnAnnotation.elementValues.keys.firstOrNull { it.simpleName.contentEquals("updatable") }
            if (updatableKey != null) {
                isUpdatable = columnAnnotation.elementValues[updatableKey]!!.value as Boolean
            }
        }
        //
        val joinColumnAnnotation = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("javax.persistence.JoinColumn")
        }
        if (joinColumnAnnotation != null) {
            isJoinColumn = true
            val insertableKey = joinColumnAnnotation.elementValues.keys.firstOrNull { it.simpleName.contentEquals("insertable") }
            if (insertableKey != null) {
                isInsertable = joinColumnAnnotation.elementValues[insertableKey]!!.value as Boolean
            }
            val updatableKey = joinColumnAnnotation.elementValues.keys.firstOrNull { it.simpleName.contentEquals("updatable") }
            if (updatableKey != null) {
                isUpdatable = joinColumnAnnotation.elementValues[updatableKey]!!.value as Boolean
            }
        }
        //
        val idAnnotation = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("javax.persistence.Id")
        }
        if (idAnnotation != null) {
            isId = true
        }
        //
        val generatedValueAnnotation = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("javax.persistence.GeneratedValue")
        }
        if (generatedValueAnnotation != null) {
            isGeneratedValue = true
        }
        //
        val versionAnnotation = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("javax.persistence.Version")
        }
        if (versionAnnotation != null) {
            isVersion = true
        }
        //
        val insertTimestampAnnotation = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.InsertTimestamp")
        }
        if (insertTimestampAnnotation != null) {
            isInsertTimestamp = true
        }
        //
        val updateTimestampAnnotation = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.UpdateTimestamp")
        }
        if (updateTimestampAnnotation != null) {
            isUpdateTimestamp = true
        }
        //
        val alternativeIdAnnotation = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("mx.com.inftel.codegen.data_access.AlternativeId")
        }
        if (alternativeIdAnnotation != null) {
            isAltId = true
        }
        //
        val notNullAnnotation1 = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("org.jetbrains.annotations.NotNull")
        }
        val notNullAnnotation2 = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("javax.validation.constraints.NotNull")
        }
        val notNullAnnotation3 = getterAnnotations.find {
            val qualifiedName = (it.annotationType.asElement() as TypeElement).qualifiedName
            qualifiedName.contentEquals("javax.validation.constraints.NotBlank")
        }
        if (notNullAnnotation1 != null || notNullAnnotation2 != null || notNullAnnotation3 != null) {
            isNotNull = true
        }
        //
        for (getterAnnotation in getterAnnotations) {
            for (annotationMirror in getterAnnotation.annotationType.asElement().annotationMirrors) {
                val qualifiedName = (annotationMirror.annotationType.asElement() as TypeElement).qualifiedName
                if (qualifiedName.contentEquals("javax.validation.Constraint")) {
                    validations.add(getterAnnotation)
                }
            }
        }
        //
        if (isJoinColumn) {
            val returnType = processingEnv.typeUtils.asElement(getter.returnType)
            if (returnType is TypeElement && returnType.isEntity) {
                joinModel = entities.getOrPut(returnType.qualifiedName.toString()) { EntityModel().also { it.fillEntityModel(returnType) } }
            }
        }
    }

    private val TypeElement.isEntity: Boolean
        get() {
            return kind == ElementKind.CLASS
                    && modifiers.contains(Modifier.PUBLIC)
                    && !modifiers.contains(Modifier.STATIC)
                    && !modifiers.contains(Modifier.ABSTRACT)
        }

    private val ExecutableElement.isGetter: Boolean
        get() {
            return kind == ElementKind.METHOD
                    && modifiers.contains(Modifier.PUBLIC)
                    && !modifiers.contains(Modifier.STATIC)
                    && !modifiers.contains(Modifier.ABSTRACT)
                    && (simpleName.startsWith("get") || simpleName.startsWith("is"))
        }

    private val ExecutableElement.isSetter: Boolean
        get() {
            return kind == ElementKind.METHOD
                    && modifiers.contains(Modifier.PUBLIC)
                    && !modifiers.contains(Modifier.STATIC)
                    && !modifiers.contains(Modifier.ABSTRACT)
                    && simpleName.startsWith("set")
        }

    private val ExecutableElement.isProhibited: Boolean
        get() {
            return simpleName.contentEquals("getClass")
        }

    private val ExecutableElement.capitalizedName: String
        get() {
            return when {
                simpleName.startsWith("get") -> simpleName.substring(3)
                simpleName.startsWith("set") -> simpleName.substring(3)
                simpleName.startsWith("is") -> simpleName.substring(2)
                else -> throw RuntimeException()
            }
        }

    private val ExecutableElement.propertyName: String
        get() {
            return Introspector.decapitalize(capitalizedName)
        }

    private val TypeMirror.asString: String
        get() = when (this.kind) {
            TypeKind.BOOLEAN -> "boolean"
            TypeKind.BYTE -> "byte"
            TypeKind.SHORT -> "short"
            TypeKind.INT -> "int"
            TypeKind.LONG -> "long"
            TypeKind.CHAR -> "char"
            TypeKind.FLOAT -> "float"
            TypeKind.DOUBLE -> "double"
            TypeKind.VOID -> "void"
            TypeKind.ARRAY -> "${(this as ArrayType).componentType.asString}[]"
            TypeKind.DECLARED -> ((this as DeclaredType).asElement() as TypeElement).qualifiedName.toString()
            else -> throw RuntimeException()
        }
}