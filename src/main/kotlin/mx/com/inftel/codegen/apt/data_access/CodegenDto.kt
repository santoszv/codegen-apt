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
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement

fun generateDto(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("// Origin: ${classModel.qualifiedName}")
    if (classModel.dtoPackageName.isNotBlank()) {
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("package ${classModel.dtoPackageName};")
    }
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("public class ${classModel.dtoSimpleName} implements java.io.Serializable {")
    //
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            generateDtoColumnField(bufferedWriter, classModel, propertyModel, null)
        } else if (propertyModel.isJoinColumn) {
            generateDtoJoinColumnField(bufferedWriter, classModel, propertyModel, null)
        } else if (propertyModel.isEmbedded) {
            generateDtoEmbeddedField(bufferedWriter, classModel, propertyModel)
        } else if (propertyModel.isEmbeddedId) {
            generateDtoEmbeddedIdField(bufferedWriter, classModel, propertyModel)
        }
    }
    //
    for (propertyModel in classModel.properties) {
        if (propertyModel.isColumn) {
            generateDtoColumnGetterSetter(bufferedWriter, classModel, propertyModel, null)
        } else if (propertyModel.isJoinColumn) {
            generateDtoJoinColumnGetterSetter(bufferedWriter, classModel, propertyModel, null)
        } else if (propertyModel.isEmbedded) {
            generateDtoEmbeddedGetterSetter(bufferedWriter, classModel, propertyModel)
        } else if (propertyModel.isEmbeddedId) {
            generateDtoEmbeddedIdGetterSetter(bufferedWriter, classModel, propertyModel)
        }
    }
    //
    bufferedWriter.appendLine("}")
}

fun generateDtoColumnField(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?) {
    if (propertyModel.isNotNull) {
        generateDtoNotNullColumnField(bufferedWriter, classModel, propertyModel, embeddedModel)
    } else {
        generateDtoNullableColumnField(bufferedWriter, classModel, propertyModel, embeddedModel)
    }
}

fun generateDtoColumnGetterSetter(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?) {
    if (propertyModel.isNotNull) {
        generateDtoNotNullColumnGetterSetter(bufferedWriter, classModel, propertyModel, embeddedModel)
    } else {
        generateDtoNullableColumnGetterSetter(bufferedWriter, classModel, propertyModel, embeddedModel)
    }
}

fun generateDtoNotNullColumnField(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?) {
    bufferedWriter.appendLine()
    when (val type = propertyModel.propertyType) {
        is BaseVoid -> Unit // <- Is it really this posible?
        is BaseByte -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = 0;")
        is BaseShort -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = 0;")
        is BaseInt -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = 0;")
        is BaseLong -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = 0;")
        is BaseFloat -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = 0.0f;")
        is BaseDouble -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = 0.0;")
        is BaseBoolean -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = false;")
        is BaseChar -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = ' ';")
        is VoidClass -> Unit // <- Is it really this posible?
        is ByteClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = ${type.toCode()}.valueOf(0);")
        is ShortClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = ${type.toCode()}.valueOf(0);")
        is IntClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = ${type.toCode()}.valueOf(0);")
        is LongClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = ${type.toCode()}.valueOf(0);")
        is FloatClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = 0.0f;")
        is DoubleClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = 0.0;")
        is BooleanClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = false;")
        is CharClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = ' ';")
        is StringClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = \"\";")
        is RefArray -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is RefClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is Unknown -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
    }
}

fun generateDtoNotNullColumnGetterSetter(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?) {
    bufferedWriter.appendLine()
    generateValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${if (embeddedModel != null) embeddedModel.propertyName + "." + propertyModel.propertyName else propertyModel.propertyName}\")")
    bufferedWriter.appendLine("    public ${propertyModel.propertyType.toCode()} ${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${if (embeddedModel != null) embeddedModel.capitalizedName + propertyModel.capitalizedName else propertyModel.capitalizedName}() {")
    bufferedWriter.appendLine("        return this.${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
    bufferedWriter.appendLine("    }")
    //
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public void set${if (embeddedModel != null) embeddedModel.capitalizedName + propertyModel.capitalizedName else propertyModel.capitalizedName}(${propertyModel.propertyType.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}) {")
    bufferedWriter.appendLine("        this.${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
    bufferedWriter.appendLine("    }")
}

fun generateDtoNullableColumnField(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?) {
    bufferedWriter.appendLine()
    when (val type = propertyModel.propertyType) {
        is BaseVoid -> Unit // <- Is it really this posible?
        is BaseByte -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is BaseShort -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is BaseInt -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is BaseLong -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is BaseFloat -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is BaseDouble -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is BaseBoolean -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is BaseChar -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is VoidClass -> Unit // <- Is it really this posible?
        is ByteClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is ShortClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is IntClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is LongClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is FloatClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is DoubleClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is BooleanClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is CharClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is StringClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is RefArray -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is RefClass -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
        is Unknown -> bufferedWriter.appendLine("    private ${type.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
    }
}

fun generateDtoNullableColumnGetterSetter(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?) {
    bufferedWriter.appendLine()
    generateValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${if (embeddedModel != null) embeddedModel.propertyName + "." + propertyModel.propertyName else propertyModel.propertyName}\")")
    bufferedWriter.appendLine("    public ${propertyModel.propertyType.toCode()} ${if (propertyModel.propertyType is BaseBoolean) "is" else "get"}${if (embeddedModel != null) embeddedModel.capitalizedName + propertyModel.capitalizedName else propertyModel.capitalizedName}() {")
    bufferedWriter.appendLine("        return this.${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
    bufferedWriter.appendLine("    }")
    //
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public void set${if (embeddedModel != null) embeddedModel.capitalizedName + propertyModel.capitalizedName else propertyModel.capitalizedName}(${propertyModel.propertyType.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}) {")
    bufferedWriter.appendLine("        this.${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName} = ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName};")
    bufferedWriter.appendLine("    }")
}

fun generateDtoJoinColumnField(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?) {
    val propertyType = propertyModel.propertyType
    if (propertyType is RefClass) {
        val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
        if (propertyModel.isNotNull) {
            generateDtoNotNullJoinColumnField(bufferedWriter, classModel, propertyModel, embeddedModel, relationModel)
        } else {
            generateDtoNullableJoinColumnField(bufferedWriter, classModel, propertyModel, embeddedModel, relationModel)
        }
    }
}

fun generateDtoJoinColumnGetterSetter(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?) {
    val propertyType = propertyModel.propertyType
    if (propertyType is RefClass) {
        val relationModel = ClassModel(propertyType.declaredType.asElement() as TypeElement)
        if (propertyModel.isNotNull) {
            generateDtoNotNullJoinColumnGetterSetter(bufferedWriter, classModel, propertyModel, embeddedModel, relationModel)
        } else {
            generateDtoNullableJoinColumnGetterSetter(bufferedWriter, classModel, propertyModel, embeddedModel, relationModel)
        }
    }
}

fun generateDtoNotNullJoinColumnField(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?, relationModel: ClassModel) {
    val relationIdProperty = relationModel.idProperty
    if (relationIdProperty != null) {
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("    private ${relationIdProperty.propertyType.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName};")
    }
}

fun generateDtoNotNullJoinColumnGetterSetter(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?, relationModel: ClassModel) {
    val relationIdProperty = relationModel.idProperty
    if (relationIdProperty != null) {
        bufferedWriter.appendLine()
        generateValidations(bufferedWriter, propertyModel.validations)
        bufferedWriter.appendLine("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${if (embeddedModel != null) embeddedModel.propertyName + "." + propertyModel.propertyName else propertyModel.propertyName}.${relationIdProperty.propertyName}\")")
        bufferedWriter.appendLine("    public ${relationIdProperty.propertyType.toCode()} get${if (embeddedModel != null) embeddedModel.capitalizedName + propertyModel.capitalizedName else propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() {")
        bufferedWriter.appendLine("        return this.${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName};")
        bufferedWriter.appendLine("    }")
        //
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("    public void set${if (embeddedModel != null) embeddedModel.capitalizedName + propertyModel.capitalizedName else propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(${relationIdProperty.propertyType.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName}) {")
        bufferedWriter.appendLine("        this.${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName} = ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName};")
        bufferedWriter.appendLine("    }")
    }
}

fun generateDtoNullableJoinColumnField(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?, relationModel: ClassModel) {
    val relationIdProperty = relationModel.idProperty
    if (relationIdProperty != null) {
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("    private ${relationIdProperty.propertyType.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName};")
    }
}

fun generateDtoNullableJoinColumnGetterSetter(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel, embeddedModel: PropertyModel?, relationModel: ClassModel) {
    val relationIdProperty = relationModel.idProperty
    if (relationIdProperty != null) {
        bufferedWriter.appendLine()
        generateValidations(bufferedWriter, propertyModel.validations)
        bufferedWriter.appendLine("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${if (embeddedModel != null) embeddedModel.propertyName + "." + propertyModel.propertyName else propertyModel.propertyName}.${relationIdProperty.propertyName}\")")
        bufferedWriter.appendLine("    public ${relationIdProperty.propertyType.toCode()} get${if (embeddedModel != null) embeddedModel.capitalizedName + propertyModel.capitalizedName else propertyModel.capitalizedName}${relationIdProperty.capitalizedName}() {")
        bufferedWriter.appendLine("        return this.${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName};")
        bufferedWriter.appendLine("    }")
        //
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("    public void set${if (embeddedModel != null) embeddedModel.capitalizedName + propertyModel.capitalizedName else propertyModel.capitalizedName}${relationIdProperty.capitalizedName}(${relationIdProperty.propertyType.toCode()} ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName}) {")
        bufferedWriter.appendLine("        this.${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName} = ${if (embeddedModel != null) embeddedModel.propertyName + propertyModel.capitalizedName else propertyModel.propertyName}${relationIdProperty.capitalizedName};")
        bufferedWriter.appendLine("    }")
    }
}

fun generateDtoEmbeddedField(bufferedWriter: BufferedWriter, classModel: ClassModel, embeddedModel: PropertyModel) {
    val embeddedPropertyType = embeddedModel.propertyType
    if (embeddedPropertyType is RefClass) {
        val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
        for (propertyModel in embeddableModel.properties) {
            if (propertyModel.isColumn) {
                generateDtoColumnField(bufferedWriter, classModel, propertyModel, embeddedModel)
            } else if (propertyModel.isJoinColumn) {
                generateDtoJoinColumnField(bufferedWriter, classModel, propertyModel, embeddedModel)
            }
        }
    }
}

fun generateDtoEmbeddedGetterSetter(bufferedWriter: BufferedWriter, classModel: ClassModel, embeddedModel: PropertyModel) {
    val embeddedPropertyType = embeddedModel.propertyType
    if (embeddedPropertyType is RefClass) {
        val embeddableModel = ClassModel(embeddedPropertyType.declaredType.asElement() as TypeElement)
        for (propertyModel in embeddableModel.properties) {
            if (propertyModel.isColumn) {
                generateDtoColumnGetterSetter(bufferedWriter, classModel, propertyModel, embeddedModel)
            } else if (propertyModel.isJoinColumn) {
                generateDtoJoinColumnGetterSetter(bufferedWriter, classModel, propertyModel, embeddedModel)
            }
        }
    }
}

fun generateDtoEmbeddedIdField(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    private ${propertyModel.propertyType.toCode()} ${propertyModel.propertyName};")
}

fun generateDtoEmbeddedIdGetterSetter(bufferedWriter: BufferedWriter, classModel: ClassModel, propertyModel: PropertyModel) {
    bufferedWriter.appendLine()
    generateValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("    @mx.com.inftel.codegen.data_access.MetaModelPath(\"${propertyModel.propertyName}\")")
    bufferedWriter.appendLine("    public ${propertyModel.propertyType.toCode()} get${propertyModel.capitalizedName}() {")
    bufferedWriter.appendLine("        return this.${propertyModel.propertyName};")
    bufferedWriter.appendLine("    }")
    //
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    public void set${propertyModel.capitalizedName}(${propertyModel.propertyType.toCode()} ${propertyModel.propertyName}) {")
    bufferedWriter.appendLine("        this.${propertyModel.propertyName} = ${propertyModel.propertyName};")
    bufferedWriter.appendLine("    }")
}

fun generateValidations(bufferedWriter: BufferedWriter, validations: List<AnnotationMirror>) {
    for (validation in validations) {
        val annotationElement = validation.annotationType.asElement() as TypeElement
        bufferedWriter.write("    @${annotationElement.qualifiedName}")
        if (validation.elementValues.isNotEmpty()) {
            bufferedWriter.write("(")
            var first = true
            for ((key, value) in validation.elementValues) {
                if (first) {
                    first = false
                } else {
                    bufferedWriter.write(", ")
                }
                bufferedWriter.write("${key.simpleName} = $value")
            }
            bufferedWriter.write(")")
        }
        bufferedWriter.appendLine()
    }
}