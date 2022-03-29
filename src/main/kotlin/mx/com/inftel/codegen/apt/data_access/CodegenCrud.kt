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

import java.io.BufferedWriter
import javax.lang.model.element.TypeElement

fun generateCrud(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("// Origin: ${classModel.qualifiedName}")
    if (classModel.crudPackageName.isNotBlank()) {
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("package ${classModel.crudPackageName};")
    }
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("public class ${classModel.crudSimpleName} {")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    protected javax.persistence.EntityManager entityManager;")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.crudSimpleName}(javax.persistence.EntityManager entityManager) {")
    bufferedWriter.appendLine("        this.entityManager = entityManager;")
    bufferedWriter.appendLine("    }")
    val idPropertyModel = classModel.idProperty ?: classModel.embeddedIdProperty
    if (idPropertyModel != null) {
        if (idPropertyModel.isGeneratedValue) {
            generateCreateWithGeneratedValue(bufferedWriter, classModel)
        } else {
            generateCreateWithoutGeneratedValue(bufferedWriter, classModel, idPropertyModel)
        }
    }
    generateCount(bufferedWriter, classModel)
    generateList(bufferedWriter, classModel)
    if (idPropertyModel != null) {
        generateFindById(bufferedWriter, classModel, idPropertyModel)
        generateUpdateById(bufferedWriter, classModel, idPropertyModel)
        generateDeleteById(bufferedWriter, classModel, idPropertyModel)
    }
    for (altIdPropertyModel in classModel.properties) {
        if (altIdPropertyModel.isAlternativeId) {
            generateFindByAltId(bufferedWriter, classModel, altIdPropertyModel)
            generateUpdateByAltId(bufferedWriter, classModel, altIdPropertyModel)
            generateDeleteByAltId(bufferedWriter, classModel, altIdPropertyModel)
        }
    }
    bufferedWriter.appendLine("}")
}

fun generateCreateWithGeneratedValue(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} create${classModel.simpleName}(${classModel.dtoQualifiedName} data) {")
    bufferedWriter.appendLine("        ${classModel.qualifiedName} entity = new ${classModel.qualifiedName}();")
    val timestamps = classModel.properties.filter { it.isColumn && it.isInsertable && it.isTimestamp }
    if (timestamps.isNotEmpty()) {
        bufferedWriter.appendLine("        long now = System.currentTimeMillis();")
        for (timestamp in timestamps) {
            bufferedWriter.appendLine("        entity.set${timestamp.capitalizedName}(now);")
        }
    }
    for ((index, propertyModel) in classModel.properties.withIndex()) {
        if (propertyModel.isManaged) continue
        if (!propertyModel.isInsertable) continue
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(data.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    if (relationIdProperty.propertyType is BaseType) {
                        bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                    } else {
                        bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index};")
                        bufferedWriter.appendLine("        if (data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() == null) {")
                        bufferedWriter.appendLine("            relation${index} = null;")
                        bufferedWriter.appendLine("        } else {")
                        bufferedWriter.appendLine("            relation${index} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    }
                    if (propertyModel.isNotNull) {
                        bufferedWriter.appendLine("        if (relation${index} == null) {")
                        bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                        bufferedWriter.appendLine("        }")
                    }
                    bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(relation${index});")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                bufferedWriter.appendLine("        ${embeddedModel.propertyType.toCode()} embeddable${index} = new ${embeddedModel.propertyType.toCode()}();")
                bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(embeddable${index});")
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for ((index2, @Suppress("NAME_SHADOWING") propertyModel) in embeddableModel.properties.withIndex()) {
                    if (propertyModel.isColumn && propertyModel.isInsertable) {
                        bufferedWriter.appendLine("        embeddable${index}.set${propertyModel.capitalizedName}(data.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${embeddedModel.capitalizedName}${propertyModel.capitalizedName}());")
                    } else if (propertyModel.isJoinColumn && propertyModel.isInsertable) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                if (relationIdProperty.propertyType is BaseType) {
                                    bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index}_${index2} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                                } else {
                                    bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index}_${index2};")
                                    bufferedWriter.appendLine("        if (data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() == null) {")
                                    bufferedWriter.appendLine("            relation${index}_${index2} = null;")
                                    bufferedWriter.appendLine("        } else {")
                                    bufferedWriter.appendLine("            relation${index}_${index2} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                                    bufferedWriter.appendLine("        }")
                                }
                                if (propertyModel.isNotNull) {
                                    bufferedWriter.appendLine("        if (relation${index}_${index2} == null) {")
                                    bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                                    bufferedWriter.appendLine("        }")
                                }
                                bufferedWriter.appendLine("        embeddable${index}.set${propertyModel.capitalizedName}(relation${index}_${index2});")
                            }
                        }
                    }
                }
            }
        }
    }
    bufferedWriter.appendLine("        entityManager.persist(entity);")
    bufferedWriter.appendLine("        entityManager.flush();")
    bufferedWriter.appendLine("        ${classModel.dtoQualifiedName} result = new ${classModel.dtoQualifiedName}();")
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    bufferedWriter.appendLine("        if (entity.get${propertyModel.capitalizedName}() != null) {")
                    bufferedWriter.appendLine("            result.set${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                    bufferedWriter.appendLine("        }")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for (@Suppress("NAME_SHADOWING") propertyModel in embeddableModel.properties) {
                    if (propertyModel.isColumn) {
                        bufferedWriter.appendLine("        if (entity.get${embeddedModel.capitalizedName}() != null) {")
                        bufferedWriter.appendLine("            result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}(entity.get${embeddedModel.capitalizedName}().${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    } else if (propertyModel.isJoinColumn) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                bufferedWriter.appendLine("            if (entity.get${embeddedModel.capitalizedName}() != null && entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}() != null) {")
                                bufferedWriter.appendLine("                result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                                bufferedWriter.appendLine("            }")
                            }
                        }
                    }
                }
            }
        } else if (propertyModel.isEmbeddedId) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.get${propertyModel.capitalizedName}());")
        }
    }
    bufferedWriter.appendLine("        return result;")
    bufferedWriter.appendLine("    }")
}

fun generateCreateWithoutGeneratedValue(bufferedWriter: BufferedWriter, classModel: ClassModel, idPropertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} create${classModel.simpleName}(${idPropertyModel.propertyType.toCode()} id, ${classModel.dtoQualifiedName} data) {")
    bufferedWriter.appendLine("        ${classModel.qualifiedName} entity = new ${classModel.qualifiedName}();")
    bufferedWriter.appendLine("        entity.set${idPropertyModel.capitalizedName}(id);")
    val timestamps = classModel.properties.filter { it.isColumn && it.isInsertable && it.isTimestamp }
    if (timestamps.isNotEmpty()) {
        bufferedWriter.appendLine("        long now = System.currentTimeMillis();")
        for (timestamp in timestamps) {
            bufferedWriter.appendLine("        entity.set${timestamp.capitalizedName}(now);")
        }
    }
    for ((index, propertyModel) in classModel.properties.withIndex()) {
        if (propertyModel.isManaged) continue
        if (!propertyModel.isInsertable) continue
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(data.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    if (relationIdProperty.propertyType is BaseType) {
                        bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                    } else {
                        bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index};")
                        bufferedWriter.appendLine("        if (data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() == null) {")
                        bufferedWriter.appendLine("            relation${index} = null;")
                        bufferedWriter.appendLine("        } else {")
                        bufferedWriter.appendLine("            relation${index} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    }
                    if (propertyModel.isNotNull) {
                        bufferedWriter.appendLine("        if (relation${index} == null) {")
                        bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                        bufferedWriter.appendLine("        }")
                    }
                    bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(relation${index});")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                bufferedWriter.appendLine("        ${embeddedModel.propertyType.toCode()} embeddable${index} = new ${embeddedModel.propertyType.toCode()}();")
                bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(embeddable${index});")
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for ((index2, @Suppress("NAME_SHADOWING") propertyModel) in embeddableModel.properties.withIndex()) {
                    if (propertyModel.isColumn && propertyModel.isInsertable) {
                        bufferedWriter.appendLine("        embeddable${index}.set${propertyModel.capitalizedName}(data.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${embeddedModel.capitalizedName}${propertyModel.capitalizedName}());")
                    } else if (propertyModel.isJoinColumn && propertyModel.isInsertable) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                if (relationIdProperty.propertyType is BaseType) {
                                    bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index}_${index2} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                                } else {
                                    bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index}_${index2};")
                                    bufferedWriter.appendLine("        if (data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() == null) {")
                                    bufferedWriter.appendLine("            relation${index}_${index2} = null;")
                                    bufferedWriter.appendLine("        } else {")
                                    bufferedWriter.appendLine("            relation${index}_${index2} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                                    bufferedWriter.appendLine("        }")
                                }
                                if (propertyModel.isNotNull) {
                                    bufferedWriter.appendLine("        if (relation${index}_${index2} == null) {")
                                    bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                                    bufferedWriter.appendLine("        }")
                                }
                                bufferedWriter.appendLine("        embeddable${index}.set${propertyModel.capitalizedName}(relation${index}_${index2});")
                            }
                        }
                    }
                }
            }
        }
    }
    bufferedWriter.appendLine("        entityManager.persist(entity);")
    bufferedWriter.appendLine("        entityManager.flush();")
    bufferedWriter.appendLine("        ${classModel.dtoQualifiedName} result = new ${classModel.dtoQualifiedName}();")
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    bufferedWriter.appendLine("        if (entity.get${propertyModel.capitalizedName}() != null) {")
                    bufferedWriter.appendLine("            result.set${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                    bufferedWriter.appendLine("        }")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for (@Suppress("NAME_SHADOWING") propertyModel in embeddableModel.properties) {
                    if (propertyModel.isColumn) {
                        bufferedWriter.appendLine("        if (entity.get${embeddedModel.capitalizedName}() != null) {")
                        bufferedWriter.appendLine("            result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}(entity.get${embeddedModel.capitalizedName}().${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    } else if (propertyModel.isJoinColumn) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                bufferedWriter.appendLine("            if (entity.get${embeddedModel.capitalizedName}() != null && entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}() != null) {")
                                bufferedWriter.appendLine("                result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                                bufferedWriter.appendLine("            }")
                            }
                        }
                    }
                }
            }
        } else if (propertyModel.isEmbeddedId) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.get${propertyModel.capitalizedName}());")
        }
    }
    bufferedWriter.appendLine("        return result;")
    bufferedWriter.appendLine("    }")
}

fun generateCount(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public java.lang.Long count${classModel.simpleName}(java.util.function.Consumer<mx.com.inftel.codegen.data_access.CountContext<${classModel.qualifiedName}>> consumer) {")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaQuery<java.lang.Long> criteriaQuery = criteriaBuilder.createQuery(java.lang.Long.class);")
    bufferedWriter.appendLine("        javax.persistence.criteria.Root<${classModel.qualifiedName}> root = criteriaQuery.from(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        mx.com.inftel.codegen.data_access.CountContext<${classModel.qualifiedName}> context = new mx.com.inftel.codegen.data_access.CountContext<>(criteriaBuilder, root);")
    bufferedWriter.appendLine("        consumer.accept(context);")
    bufferedWriter.appendLine("        criteriaQuery.select(criteriaBuilder.count(root));")
    bufferedWriter.appendLine("        if (!context.getPredicates().isEmpty()) {")
    bufferedWriter.appendLine("            criteriaQuery.where(context.getPredicates().toArray(new javax.persistence.criteria.Predicate[0]));")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        javax.persistence.TypedQuery<java.lang.Long> typedQuery = entityManager.createQuery(criteriaQuery);")
    bufferedWriter.appendLine("        java.util.List<java.lang.Long> list = typedQuery.getResultList();")
    bufferedWriter.appendLine("        if (list.isEmpty()) {")
    bufferedWriter.appendLine("            return null;")
    bufferedWriter.appendLine("        } else {")
    bufferedWriter.appendLine("            return list.get(0);")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("    }")
}

fun generateList(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public java.util.List<${classModel.dtoQualifiedName}> list${classModel.simpleName}(java.util.function.Consumer<mx.com.inftel.codegen.data_access.ListContext<${classModel.qualifiedName}>> consumer) {")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaQuery<${classModel.qualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        javax.persistence.criteria.Root<${classModel.qualifiedName}> root = criteriaQuery.from(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        mx.com.inftel.codegen.data_access.ListContext<${classModel.qualifiedName}> context = new mx.com.inftel.codegen.data_access.ListContext<>(criteriaBuilder, root);")
    bufferedWriter.appendLine("        consumer.accept(context);")
    bufferedWriter.appendLine("        criteriaQuery.select(root);")
    bufferedWriter.appendLine("        if (!context.getPredicates().isEmpty()) {")
    bufferedWriter.appendLine("            criteriaQuery.where(context.getPredicates().toArray(new javax.persistence.criteria.Predicate[0]));")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        if (!context.getOrders().isEmpty()) {")
    bufferedWriter.appendLine("            criteriaQuery.orderBy(context.getOrders().toArray(new javax.persistence.criteria.Order[0]));")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        javax.persistence.TypedQuery<${classModel.qualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
    bufferedWriter.appendLine("        if (context.getFirstResult() >= 0) {")
    bufferedWriter.appendLine("            typedQuery.setFirstResult(context.getFirstResult());")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        if (context.getMaxResults() >= 0) {")
    bufferedWriter.appendLine("            typedQuery.setMaxResults(context.getMaxResults());")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        if (context.getLockMode() != null) {")
    bufferedWriter.appendLine("            typedQuery.setLockMode(context.getLockMode());")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        java.util.List<${classModel.qualifiedName}> list1 = typedQuery.getResultList();")
    bufferedWriter.appendLine("        java.util.ArrayList<${classModel.dtoQualifiedName}> list2 = new java.util.ArrayList<>();")
    bufferedWriter.appendLine("        list2.ensureCapacity(list1.size());")
    bufferedWriter.appendLine("        for (${classModel.qualifiedName} entity: list1) {")
    bufferedWriter.appendLine("            ${classModel.dtoQualifiedName} result = new ${classModel.dtoQualifiedName}();")
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("            result.set${propertyModel.capitalizedName}(entity.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    bufferedWriter.appendLine("            if (entity.get${propertyModel.capitalizedName}() != null) {")
                    bufferedWriter.appendLine("                result.set${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                    bufferedWriter.appendLine("            }")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for (@Suppress("NAME_SHADOWING") propertyModel in embeddableModel.properties) {
                    if (propertyModel.isColumn) {
                        bufferedWriter.appendLine("            if (entity.get${embeddedModel.capitalizedName}() != null) {")
                        bufferedWriter.appendLine("                result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}(entity.get${embeddedModel.capitalizedName}().${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
                        bufferedWriter.appendLine("            }")
                    } else if (propertyModel.isJoinColumn) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                bufferedWriter.appendLine("                if (entity.get${embeddedModel.capitalizedName}() != null && entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}() != null) {")
                                bufferedWriter.appendLine("                    result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                                bufferedWriter.appendLine("                }")
                            }
                        }
                    }
                }
            }
        } else if (propertyModel.isEmbeddedId) {
            bufferedWriter.appendLine("            result.set${propertyModel.capitalizedName}(entity.get${propertyModel.capitalizedName}());")
        }
    }
    bufferedWriter.appendLine("            list2.add(result);")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        return list2;")
    bufferedWriter.appendLine("    }")
}

fun generateFindById(bufferedWriter: BufferedWriter, classModel: ClassModel, idPropertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} find${classModel.simpleName}ById(${idPropertyModel.propertyType.toCode()} id) {")
    bufferedWriter.appendLine("        return find${classModel.simpleName}ById(id, null);")
    bufferedWriter.appendLine("    }")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} find${classModel.simpleName}ById(${idPropertyModel.propertyType.toCode()} id, javax.persistence.LockModeType lockMode) {")
    bufferedWriter.appendLine("        ${classModel.qualifiedName} entity;")
    bufferedWriter.appendLine("        if (lockMode == null) {")
    bufferedWriter.appendLine("            entity = entityManager.find(${classModel.qualifiedName}.class, id);")
    bufferedWriter.appendLine("        } else {")
    bufferedWriter.appendLine("            entity = entityManager.find(${classModel.qualifiedName}.class, id, lockMode);")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        if (entity == null) {")
    bufferedWriter.appendLine("            return null;")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        ${classModel.dtoQualifiedName} result = new ${classModel.dtoQualifiedName}();")
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    bufferedWriter.appendLine("        if (entity.get${propertyModel.capitalizedName}() != null) {")
                    bufferedWriter.appendLine("            result.set${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                    bufferedWriter.appendLine("        }")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for (@Suppress("NAME_SHADOWING") propertyModel in embeddableModel.properties) {
                    if (propertyModel.isColumn) {
                        bufferedWriter.appendLine("        if (entity.get${embeddedModel.capitalizedName}() != null) {")
                        bufferedWriter.appendLine("            result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}(entity.get${embeddedModel.capitalizedName}().${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    } else if (propertyModel.isJoinColumn) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                bufferedWriter.appendLine("            if (entity.get${embeddedModel.capitalizedName}() != null && entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}() != null) {")
                                bufferedWriter.appendLine("                result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                                bufferedWriter.appendLine("            }")
                            }
                        }
                    }
                }
            }
        } else if (propertyModel.isEmbeddedId) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.get${propertyModel.capitalizedName}());")
        }
    }
    bufferedWriter.appendLine("        return result;")
    bufferedWriter.appendLine("    }")
}

fun generateUpdateById(bufferedWriter: BufferedWriter, classModel: ClassModel, idPropertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} update${classModel.simpleName}ById(${idPropertyModel.propertyType.toCode()} id, ${classModel.dtoQualifiedName} data) {")
    bufferedWriter.appendLine("        return update${classModel.simpleName}ById(id, data, null);")
    bufferedWriter.appendLine("    }")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} update${classModel.simpleName}ById(${idPropertyModel.propertyType.toCode()} id, ${classModel.dtoQualifiedName} data, javax.persistence.LockModeType lockMode) {")
    bufferedWriter.appendLine("        ${classModel.qualifiedName} entity;")
    bufferedWriter.appendLine("        if (lockMode == null) {")
    bufferedWriter.appendLine("            entity = entityManager.find(${classModel.qualifiedName}.class, id);")
    bufferedWriter.appendLine("        } else {")
    bufferedWriter.appendLine("            entity = entityManager.find(${classModel.qualifiedName}.class, id, lockMode);")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        if (entity == null) {")
    bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException(\"Entity Not Found\");")
    bufferedWriter.appendLine("        }")
    val timestamps = classModel.properties.filter { it.isColumn && it.isInsertable && it.isUpdateTimestamp }
    if (timestamps.isNotEmpty()) {
        bufferedWriter.appendLine("        long now = System.currentTimeMillis();")
        for (timestamp in timestamps) {
            bufferedWriter.appendLine("        entity.set${timestamp.capitalizedName}(now);")
        }
    }
    for ((index, propertyModel) in classModel.properties.withIndex()) {
        if (propertyModel.isManaged) continue
        if (!propertyModel.isUpdatable) continue
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(data.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    if (relationIdProperty.propertyType is BaseType) {
                        bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                    } else {
                        bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index};")
                        bufferedWriter.appendLine("        if (data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() == null) {")
                        bufferedWriter.appendLine("            relation${index} = null;")
                        bufferedWriter.appendLine("        } else {")
                        bufferedWriter.appendLine("            relation${index} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    }
                    if (propertyModel.isNotNull) {
                        bufferedWriter.appendLine("        if (relation${index} == null) {")
                        bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                        bufferedWriter.appendLine("        }")
                    }
                    bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(relation${index});")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                bufferedWriter.appendLine("        ${embeddedModel.propertyType.toCode()} embeddable${index} = new ${embeddedModel.propertyType.toCode()}();")
                bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(embeddable${index});")
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for ((index2, @Suppress("NAME_SHADOWING") propertyModel) in embeddableModel.properties.withIndex()) {
                    if (propertyModel.isColumn && propertyModel.isInsertable) {
                        bufferedWriter.appendLine("        embeddable${index}.set${propertyModel.capitalizedName}(data.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${embeddedModel.capitalizedName}${propertyModel.capitalizedName}());")
                    } else if (propertyModel.isJoinColumn && propertyModel.isInsertable) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                if (relationIdProperty.propertyType is BaseType) {
                                    bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index}_${index2} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                                } else {
                                    bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index}_${index2};")
                                    bufferedWriter.appendLine("        if (data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() == null) {")
                                    bufferedWriter.appendLine("            relation${index}_${index2} = null;")
                                    bufferedWriter.appendLine("        } else {")
                                    bufferedWriter.appendLine("            relation${index}_${index2} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                                    bufferedWriter.appendLine("        }")
                                }
                                if (propertyModel.isNotNull) {
                                    bufferedWriter.appendLine("        if (relation${index}_${index2} == null) {")
                                    bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                                    bufferedWriter.appendLine("        }")
                                }
                                bufferedWriter.appendLine("        embeddable${index}.set${propertyModel.capitalizedName}(relation${index}_${index2});")
                            }
                        }
                    }
                }
            }
        }
    }
    bufferedWriter.appendLine("        entityManager.flush();")
    bufferedWriter.appendLine("        ${classModel.dtoQualifiedName} result = new ${classModel.dtoQualifiedName}();")
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    bufferedWriter.appendLine("        if (entity.get${propertyModel.capitalizedName}() != null) {")
                    bufferedWriter.appendLine("            result.set${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                    bufferedWriter.appendLine("        }")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for (@Suppress("NAME_SHADOWING") propertyModel in embeddableModel.properties) {
                    if (propertyModel.isColumn) {
                        bufferedWriter.appendLine("        if (entity.get${embeddedModel.capitalizedName}() != null) {")
                        bufferedWriter.appendLine("            result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}(entity.get${embeddedModel.capitalizedName}().${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    } else if (propertyModel.isJoinColumn) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                bufferedWriter.appendLine("            if (entity.get${embeddedModel.capitalizedName}() != null && entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}() != null) {")
                                bufferedWriter.appendLine("                result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                                bufferedWriter.appendLine("            }")
                            }
                        }
                    }
                }
            }
        } else if (propertyModel.isEmbeddedId) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.get${propertyModel.capitalizedName}());")
        }
    }
    bufferedWriter.appendLine("        return result;")
    bufferedWriter.appendLine("    }")
}

fun generateDeleteById(bufferedWriter: BufferedWriter, classModel: ClassModel, idPropertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public void delete${classModel.simpleName}ById(${idPropertyModel.propertyType.toCode()} id) {")
    bufferedWriter.appendLine("        delete${classModel.simpleName}ById(id, null);")
    bufferedWriter.appendLine("    }")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public void delete${classModel.simpleName}ById(${idPropertyModel.propertyType.toCode()} id, javax.persistence.LockModeType lockMode) {")
    bufferedWriter.appendLine("        ${classModel.qualifiedName} entity;")
    bufferedWriter.appendLine("        if (lockMode == null) {")
    bufferedWriter.appendLine("            entity = entityManager.find(${classModel.qualifiedName}.class, id);")
    bufferedWriter.appendLine("        } else {")
    bufferedWriter.appendLine("            entity = entityManager.find(${classModel.qualifiedName}.class, id, lockMode);")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        if (entity == null) {")
    bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException(\"Entity Not Found\");")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        entityManager.remove(entity);")
    bufferedWriter.appendLine("        entityManager.flush();")
    bufferedWriter.appendLine("    }")
}

fun generateFindByAltId(bufferedWriter: BufferedWriter, classModel: ClassModel, altIdPropertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} find${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(${altIdPropertyModel.propertyType.toCode()} altId) {")
    bufferedWriter.appendLine("        return find${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(altId, null);")
    bufferedWriter.appendLine("    }")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} find${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(${altIdPropertyModel.propertyType.toCode()} altId, javax.persistence.LockModeType lockMode) {")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaQuery<${classModel.qualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        javax.persistence.criteria.Root<${classModel.qualifiedName}> root = criteriaQuery.from(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        criteriaQuery.select(root);")
    bufferedWriter.appendLine("        criteriaQuery.where(criteriaBuilder.equal(root.get(${classModel.qualifiedName}_.${altIdPropertyModel.propertyName}), altId));")
    bufferedWriter.appendLine("        javax.persistence.TypedQuery<${classModel.qualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
    bufferedWriter.appendLine("        typedQuery.setFirstResult(0);")
    bufferedWriter.appendLine("        typedQuery.setMaxResults(1);")
    bufferedWriter.appendLine("        if (lockMode != null) {")
    bufferedWriter.appendLine("            typedQuery.setLockMode(lockMode);")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        java.util.List<${classModel.qualifiedName}> list = typedQuery.getResultList();")
    bufferedWriter.appendLine("        if (list.isEmpty()) {")
    bufferedWriter.appendLine("            return null;")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        ${classModel.qualifiedName} entity = list.get(0);")
    bufferedWriter.appendLine("        ${classModel.dtoQualifiedName} result = new ${classModel.dtoQualifiedName}();")
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    bufferedWriter.appendLine("        if (entity.get${propertyModel.capitalizedName}() != null) {")
                    bufferedWriter.appendLine("            result.set${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                    bufferedWriter.appendLine("        }")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for (@Suppress("NAME_SHADOWING") propertyModel in embeddableModel.properties) {
                    if (propertyModel.isColumn) {
                        bufferedWriter.appendLine("        if (entity.get${embeddedModel.capitalizedName}() != null) {")
                        bufferedWriter.appendLine("            result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}(entity.get${embeddedModel.capitalizedName}().${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    } else if (propertyModel.isJoinColumn) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                bufferedWriter.appendLine("            if (entity.get${embeddedModel.capitalizedName}() != null && entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}() != null) {")
                                bufferedWriter.appendLine("                result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                                bufferedWriter.appendLine("            }")
                            }
                        }
                    }
                }
            }
        } else if (propertyModel.isEmbeddedId) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.get${propertyModel.capitalizedName}());")
        }
    }
    bufferedWriter.appendLine("        return result;")
    bufferedWriter.appendLine("    }")
}

fun generateUpdateByAltId(bufferedWriter: BufferedWriter, classModel: ClassModel, altIdPropertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} update${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(${altIdPropertyModel.propertyType.toCode()} altId, ${classModel.dtoQualifiedName} data) {")
    bufferedWriter.appendLine("        return update${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(altId, data, null);")
    bufferedWriter.appendLine("    }")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public ${classModel.dtoQualifiedName} update${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(${altIdPropertyModel.propertyType.toCode()} altId, ${classModel.dtoQualifiedName} data, javax.persistence.LockModeType lockMode) {")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaQuery<${classModel.qualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        javax.persistence.criteria.Root<${classModel.qualifiedName}> root = criteriaQuery.from(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        criteriaQuery.select(root);")
    bufferedWriter.appendLine("        criteriaQuery.where(criteriaBuilder.equal(root.get(${classModel.qualifiedName}_.${altIdPropertyModel.propertyName}), altId));")
    bufferedWriter.appendLine("        javax.persistence.TypedQuery<${classModel.qualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
    bufferedWriter.appendLine("        typedQuery.setFirstResult(0);")
    bufferedWriter.appendLine("        typedQuery.setMaxResults(1);")
    bufferedWriter.appendLine("        if (lockMode != null) {")
    bufferedWriter.appendLine("            typedQuery.setLockMode(lockMode);")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        java.util.List<${classModel.qualifiedName}> list = typedQuery.getResultList();")
    bufferedWriter.appendLine("        if (list.isEmpty()) {")
    bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException(\"Entity Not Found\");")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        ${classModel.qualifiedName} entity = list.get(0);")
    val timestamps = classModel.properties.filter { it.isColumn && it.isInsertable && it.isUpdateTimestamp }
    if (timestamps.isNotEmpty()) {
        bufferedWriter.appendLine("        long now = System.currentTimeMillis();")
        for (timestamp in timestamps) {
            bufferedWriter.appendLine("        entity.set${timestamp.capitalizedName}(now);")
        }
    }
    for ((index, propertyModel) in classModel.properties.withIndex()) {
        if (propertyModel.isManaged) continue
        if (!propertyModel.isUpdatable) continue
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(data.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    if (relationIdProperty.propertyType is BaseType) {
                        bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                    } else {
                        bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index};")
                        bufferedWriter.appendLine("        if (data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() == null) {")
                        bufferedWriter.appendLine("            relation${index} = null;")
                        bufferedWriter.appendLine("        } else {")
                        bufferedWriter.appendLine("            relation${index} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    }
                    if (propertyModel.isNotNull) {
                        bufferedWriter.appendLine("        if (relation${index} == null) {")
                        bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                        bufferedWriter.appendLine("        }")
                    }
                    bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(relation${index});")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                bufferedWriter.appendLine("        ${embeddedModel.propertyType.toCode()} embeddable${index} = new ${embeddedModel.propertyType.toCode()}();")
                bufferedWriter.appendLine("        entity.set${propertyModel.capitalizedName}(embeddable${index});")
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for ((index2, @Suppress("NAME_SHADOWING") propertyModel) in embeddableModel.properties.withIndex()) {
                    if (propertyModel.isColumn && propertyModel.isInsertable) {
                        bufferedWriter.appendLine("        embeddable${index}.set${propertyModel.capitalizedName}(data.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${embeddedModel.capitalizedName}${propertyModel.capitalizedName}());")
                    } else if (propertyModel.isJoinColumn && propertyModel.isInsertable) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                if (relationIdProperty.propertyType is BaseType) {
                                    bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index}_${index2} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                                } else {
                                    bufferedWriter.appendLine("        ${propertyModel.propertyType.toCode()} relation${index}_${index2};")
                                    bufferedWriter.appendLine("        if (data.get${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() == null) {")
                                    bufferedWriter.appendLine("            relation${index}_${index2} = null;")
                                    bufferedWriter.appendLine("        } else {")
                                    bufferedWriter.appendLine("            relation${index}_${index2} = entityManager.find(${propertyModel.propertyType.toCode()}.class, data.get${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}());")
                                    bufferedWriter.appendLine("        }")
                                }
                                if (propertyModel.isNotNull) {
                                    bufferedWriter.appendLine("        if (relation${index}_${index2} == null) {")
                                    bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.RelationNotFoundException(\"Relation Not Found\");")
                                    bufferedWriter.appendLine("        }")
                                }
                                bufferedWriter.appendLine("        embeddable${index}.set${propertyModel.capitalizedName}(relation${index}_${index2});")
                            }
                        }
                    }
                }
            }
        }
    }
    bufferedWriter.appendLine("        entityManager.flush();")
    bufferedWriter.appendLine("        ${classModel.dtoQualifiedName} result = new ${classModel.dtoQualifiedName}();")
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
        } else if (propertyModel.isJoinColumn) {
            val propertyType = propertyModel.propertyType
            if (propertyType is RefClass) {
                val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                val relationIdProperty = relationModel.idProperty
                if (relationIdProperty != null) {
                    bufferedWriter.appendLine("        if (entity.get${propertyModel.capitalizedName}() != null) {")
                    bufferedWriter.appendLine("            result.set${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                    bufferedWriter.appendLine("        }")
                }
            }
        } else if (propertyModel.isEmbedded) {
            val embeddedModel = propertyModel
            val embeddedPropertyType = embeddedModel.propertyType
            if (embeddedPropertyType is RefClass) {
                val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
                for (@Suppress("NAME_SHADOWING") propertyModel in embeddableModel.properties) {
                    if (propertyModel.isColumn) {
                        bufferedWriter.appendLine("        if (entity.get${embeddedModel.capitalizedName}() != null) {")
                        bufferedWriter.appendLine("            result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}(entity.get${embeddedModel.capitalizedName}().${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${propertyModel.capitalizedName}());")
                        bufferedWriter.appendLine("        }")
                    } else if (propertyModel.isJoinColumn) {
                        val propertyType = propertyModel.propertyType
                        if (propertyType is RefClass) {
                            val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
                            val relationIdProperty = relationModel.idProperty
                            if (relationIdProperty != null) {
                                bufferedWriter.appendLine("            if (entity.get${embeddedModel.capitalizedName}() != null && entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}() != null) {")
                                bufferedWriter.appendLine("                result.set${embeddedModel.capitalizedName}${propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(entity.get${embeddedModel.capitalizedName}().get${propertyModel.capitalizedName}().get${relationIdProperty.capitalizedName}());")
                                bufferedWriter.appendLine("            }")
                            }
                        }
                    }
                }
            }
        } else if (propertyModel.isEmbeddedId) {
            bufferedWriter.appendLine("        result.set${propertyModel.capitalizedName}(entity.get${propertyModel.capitalizedName}());")
        }
    }
    bufferedWriter.appendLine("        return result;")
    bufferedWriter.appendLine("    }")
}

fun generateDeleteByAltId(bufferedWriter: BufferedWriter, classModel: ClassModel, altIdPropertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public void delete${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(${altIdPropertyModel.propertyType.toCode()} altId) {")
    bufferedWriter.appendLine("        delete${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(altId, null);")
    bufferedWriter.appendLine("    }")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public void delete${classModel.simpleName}By${altIdPropertyModel.capitalizedName}(${altIdPropertyModel.propertyType.toCode()} altId, javax.persistence.LockModeType lockMode) {")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    bufferedWriter.appendLine("        javax.persistence.criteria.CriteriaQuery<${classModel.qualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        javax.persistence.criteria.Root<${classModel.qualifiedName}> root = criteriaQuery.from(${classModel.qualifiedName}.class);")
    bufferedWriter.appendLine("        criteriaQuery.select(root);")
    bufferedWriter.appendLine("        criteriaQuery.where(criteriaBuilder.equal(root.get(${classModel.qualifiedName}_.${altIdPropertyModel.propertyName}), altId));")
    bufferedWriter.appendLine("        javax.persistence.TypedQuery<${classModel.qualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
    bufferedWriter.appendLine("        typedQuery.setFirstResult(0);")
    bufferedWriter.appendLine("        typedQuery.setMaxResults(1);")
    bufferedWriter.appendLine("        if (lockMode != null) {")
    bufferedWriter.appendLine("            typedQuery.setLockMode(lockMode);")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        java.util.List<${classModel.qualifiedName}> list = typedQuery.getResultList();")
    bufferedWriter.appendLine("        if (list.isEmpty()) {")
    bufferedWriter.appendLine("            throw new mx.com.inftel.codegen.exceptions.EntityNotFoundException(\"Entity Not Found\");")
    bufferedWriter.appendLine("        }")
    bufferedWriter.appendLine("        ${classModel.qualifiedName} entity = list.get(0);")
    bufferedWriter.appendLine("        entityManager.remove(entity);")
    bufferedWriter.appendLine("        entityManager.flush();")
    bufferedWriter.appendLine("    }")
}