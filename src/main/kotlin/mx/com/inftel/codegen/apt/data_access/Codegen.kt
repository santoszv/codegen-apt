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

import java.io.BufferedWriter

fun generateCount(writer: BufferedWriter, entityModel: EntityModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public java.lang.Long count${entityModel.simpleName}(java.util.function.Consumer<mx.com.inftel.codegen.data_access.CountContext<${entityModel.fullyQualifiedName}>> consumer) {")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaQuery<java.lang.Long> criteriaQuery = criteriaBuilder.createQuery(java.lang.Long.class);")
    writer.newLine()
    writer.write("        javax.persistence.criteria.Root<${entityModel.fullyQualifiedName}> root = criteriaQuery.from(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        mx.com.inftel.codegen.data_access.CountContext<${entityModel.fullyQualifiedName}> context = new mx.com.inftel.codegen.data_access.CountContext<>(criteriaBuilder, root);")
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

fun generateList(writer: BufferedWriter, entityModel: EntityModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public java.util.List<${entityModel.dtoFullyQualifiedName}> list${entityModel.simpleName}(java.util.function.Consumer<mx.com.inftel.codegen.data_access.ListContext<${entityModel.fullyQualifiedName}>> consumer) {")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.fullyQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        javax.persistence.criteria.Root<${entityModel.fullyQualifiedName}> root = criteriaQuery.from(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        mx.com.inftel.codegen.data_access.ListContext<${entityModel.fullyQualifiedName}> context = new mx.com.inftel.codegen.data_access.ListContext<>(criteriaBuilder, root);")
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
    writer.write("        javax.persistence.TypedQuery<${entityModel.fullyQualifiedName}> typedQuery = entityManager.createQuery(criteriaQuery);")
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
    writer.write("        java.util.List<${entityModel.fullyQualifiedName}> list1 = typedQuery.getResultList();")
    writer.newLine()
    writer.write("        java.util.ArrayList<${entityModel.dtoFullyQualifiedName}> list2 = new java.util.ArrayList<>();")
    writer.newLine()
    writer.write("        list2.ensureCapacity(list1.size());")
    writer.newLine()
    writer.write("        for (${entityModel.fullyQualifiedName} entity: list1) {")
    writer.newLine()
    writer.write("            ${entityModel.dtoFullyQualifiedName} result = new ${entityModel.dtoFullyQualifiedName}();")
    for (property in entityModel.properties) {
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
        } else if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("            result.${ebdProperty.setter.simpleName}${property.capitalizedName}(entity.${property.getter.simpleName}().${ebdProperty.getter.simpleName}());")
                    }
                }
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

fun generateCreateWithGeneratedValue(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
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
        if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                writer.newLine()
                writer.write("        ${property.getter.returnType.asString} embeddable${index} = new ${property.getter.returnType.asString}();")
                writer.newLine()
                writer.write("        entity.${property.setter.simpleName}(embeddable${index});")
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("        embeddable${index}.${ebdProperty.setter.simpleName}(data.${ebdProperty.getter.simpleName}${property.capitalizedName}());")
                    }
                }
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
        } else if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("        result.${ebdProperty.setter.simpleName}${property.capitalizedName}(entity.${property.getter.simpleName}().${ebdProperty.getter.simpleName}());")
                    }
                }
            }
        }
    }
    writer.newLine()
    writer.write("        return result;")
    writer.newLine()
    writer.write("    }")
}

fun generateCreateWithoutGeneratedValue(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} create${entityModel.simpleName}(${idModel.getter.returnType.asString} id, ${entityModel.dtoFullyQualifiedName} data) {")
    writer.newLine()
    writer.write("        ${entityModel.fullyQualifiedName} entity = new ${entityModel.fullyQualifiedName}();")
    writer.newLine()
    writer.write("        entity.${idModel.setter.simpleName}(id);")
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
        if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                writer.newLine()
                writer.write("        ${property.getter.returnType.asString} embeddable${index} = new ${property.getter.returnType.asString}();")
                writer.newLine()
                writer.write("        entity.${property.setter.simpleName}(embeddable${index});")
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("        embeddable${index}.${ebdProperty.setter.simpleName}(data.${ebdProperty.getter.simpleName}${property.capitalizedName}());")
                    }
                }
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
        } else if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("        result.${ebdProperty.setter.simpleName}${property.capitalizedName}(entity.${property.getter.simpleName}().${ebdProperty.getter.simpleName}());")
                    }
                }
            }
        }
    }
    writer.newLine()
    writer.write("        return result;")
    writer.newLine()
    writer.write("    }")
}

fun generateFindById(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} find${entityModel.simpleName}ById(${idModel.getter.returnType.asString} id) {")
    writer.newLine()
    writer.write("        return find${entityModel.simpleName}By${idModel.capitalizedName}(id, null);")
    writer.newLine()
    writer.write("    }")
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} find${entityModel.simpleName}ById(${idModel.getter.returnType.asString} id, javax.persistence.LockModeType lockMode) {")
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
        } else if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("        result.${ebdProperty.setter.simpleName}${property.capitalizedName}(entity.${property.getter.simpleName}().${ebdProperty.getter.simpleName}());")
                    }
                }
            }
        }
    }
    writer.newLine()
    writer.write("        return result;")
    writer.newLine()
    writer.write("    }")
}

fun generateUpdateById(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} update${entityModel.simpleName}ById(${idModel.getter.returnType.asString} id, ${entityModel.dtoFullyQualifiedName} data) {")
    writer.newLine()
    writer.write("        return update${entityModel.simpleName}By${idModel.capitalizedName}(id, data, null);")
    writer.newLine()
    writer.write("    }")
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} update${entityModel.simpleName}By${idModel.capitalizedName}(${idModel.getter.returnType.asString} id, ${entityModel.dtoFullyQualifiedName} data, javax.persistence.LockModeType lockMode) {")
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
        if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                writer.newLine()
                writer.write("        ${property.getter.returnType.asString} embeddable${index} = entity.${property.getter.simpleName}();")
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isUpdatable) {
                        writer.newLine()
                        writer.write("        embeddable${index}.${ebdProperty.setter.simpleName}(data.${ebdProperty.getter.simpleName}${property.capitalizedName}());")
                    }
                }
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
        } else if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("        result.${ebdProperty.setter.simpleName}${property.capitalizedName}(entity.${property.getter.simpleName}().${ebdProperty.getter.simpleName}());")
                    }
                }
            }
        }
    }
    writer.newLine()
    writer.write("        return result;")
    writer.newLine()
    writer.write("    }")
}

fun generateDeleteById(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public void delete${entityModel.simpleName}ById(${idModel.getter.returnType.asString} id) {")
    writer.newLine()
    writer.write("        delete${entityModel.simpleName}By${idModel.capitalizedName}(id, null);")
    writer.newLine()
    writer.write("    }")
    writer.newLine()
    writer.newLine()
    writer.write("    public void delete${entityModel.simpleName}ById(${idModel.getter.returnType.asString} id, javax.persistence.LockModeType lockMode) {")
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

fun generateFindByAltId(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} find${entityModel.simpleName}By${idModel.capitalizedName}(${idModel.getter.returnType.asString} altId) {")
    writer.newLine()
    writer.write("        return find${entityModel.simpleName}By${idModel.capitalizedName}(altId, null);")
    writer.newLine()
    writer.write("    }")
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} find${entityModel.simpleName}By${idModel.capitalizedName}(${idModel.getter.returnType.asString} altId, javax.persistence.LockModeType lockMode) {")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.fullyQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        javax.persistence.criteria.Root<${entityModel.fullyQualifiedName}> root = criteriaQuery.from(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        criteriaQuery.select(root);")
    writer.newLine()
    writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.fullyQualifiedName}_.${idModel.propertyName}), altId));")
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
        } else if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("        result.${ebdProperty.setter.simpleName}${property.capitalizedName}(entity.${property.getter.simpleName}().${ebdProperty.getter.simpleName}());")
                    }
                }
            }
        }
    }
    writer.newLine()
    writer.write("        return result;")
    writer.newLine()
    writer.write("    }")
}

fun generateUpdateByAltId(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} update${entityModel.simpleName}By${idModel.capitalizedName}(${idModel.getter.returnType.asString} altId, ${entityModel.dtoFullyQualifiedName} data) {")
    writer.newLine()
    writer.write("        return update${entityModel.simpleName}By${idModel.capitalizedName}(altId, data, null);")
    writer.newLine()
    writer.write("    }")
    writer.newLine()
    writer.newLine()
    writer.write("    public ${entityModel.dtoFullyQualifiedName} update${entityModel.simpleName}By${idModel.capitalizedName}(${idModel.getter.returnType.asString} altId, ${entityModel.dtoFullyQualifiedName} data, javax.persistence.LockModeType lockMode) {")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.fullyQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        javax.persistence.criteria.Root<${entityModel.fullyQualifiedName}> root = criteriaQuery.from(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        criteriaQuery.select(root);")
    writer.newLine()
    writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.fullyQualifiedName}_.${idModel.propertyName}), altId));")
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
        if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                writer.newLine()
                writer.write("        ${property.getter.returnType.asString} embeddable${index} = entity.${property.getter.simpleName}();")
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isUpdatable) {
                        writer.newLine()
                        writer.write("        embeddable${index}.${ebdProperty.setter.simpleName}(data.${ebdProperty.getter.simpleName}${property.capitalizedName}());")
                    }
                }
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
        } else if (property.isEmbedded) {
            val embeddableModel = property.embeddableModel
            if (embeddableModel != null) {
                for ((ebdIndex, ebdProperty) in embeddableModel.properties.withIndex()) {
                    if (ebdProperty.isColumn && ebdProperty.isInsertable) {
                        writer.newLine()
                        writer.write("        result.${ebdProperty.setter.simpleName}${property.capitalizedName}(entity.${property.getter.simpleName}().${ebdProperty.getter.simpleName}());")
                    }
                }
            }
        }
    }
    writer.newLine()
    writer.write("        return result;")
    writer.newLine()
    writer.write("    }")
}

fun generateDeleteByAltId(writer: BufferedWriter, entityModel: EntityModel, idModel: PropertyModel) {
    writer.newLine()
    writer.newLine()
    writer.write("    public void delete${entityModel.simpleName}By${idModel.capitalizedName}(${idModel.getter.returnType.asString} altId) {")
    writer.newLine()
    writer.write("        delete${entityModel.simpleName}By${idModel.capitalizedName}(altId, null);")
    writer.newLine()
    writer.write("    }")
    writer.newLine()
    writer.newLine()
    writer.write("    public void delete${entityModel.simpleName}By${idModel.capitalizedName}(${idModel.getter.returnType.asString} altId, javax.persistence.LockModeType lockMode) {")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();")
    writer.newLine()
    writer.write("        javax.persistence.criteria.CriteriaQuery<${entityModel.fullyQualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        javax.persistence.criteria.Root<${entityModel.fullyQualifiedName}> root = criteriaQuery.from(${entityModel.fullyQualifiedName}.class);")
    writer.newLine()
    writer.write("        criteriaQuery.select(root);")
    writer.newLine()
    writer.write("        criteriaQuery.where(criteriaBuilder.equal(root.get(${entityModel.fullyQualifiedName}_.${idModel.propertyName}), altId));")
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