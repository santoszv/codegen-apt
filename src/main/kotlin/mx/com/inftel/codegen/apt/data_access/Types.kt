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

import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

sealed class Type {
    abstract fun toCode(): String
}

sealed class BaseType : Type()
sealed class RefType : Type()

object BaseVoid : BaseType() {
    override fun toCode(): String {
        return "void";
    }
}

object BaseByte : BaseType() {
    override fun toCode(): String {
        return "byte";
    }
}

object BaseShort : BaseType() {
    override fun toCode(): String {
        return "short";
    }
}

object BaseInt : BaseType() {
    override fun toCode(): String {
        return "int";
    }
}

object BaseLong : BaseType() {
    override fun toCode(): String {
        return "long";
    }
}

object BaseFloat : BaseType() {
    override fun toCode(): String {
        return "float";
    }
}

object BaseDouble : BaseType() {
    override fun toCode(): String {
        return "double";
    }
}

object BaseBoolean : BaseType() {
    override fun toCode(): String {
        return "boolean";
    }
}

object BaseChar : BaseType() {
    override fun toCode(): String {
        return "char";
    }
}

object VoidClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Void";
    }
}

object ByteClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Byte";
    }
}

object ShortClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Short";
    }
}

object IntClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Integer";
    }
}

object LongClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Long";
    }
}

object FloatClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Float";
    }
}

object DoubleClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Double";
    }
}

object BooleanClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Boolean";
    }
}

object CharClass : RefType() {
    override fun toCode(): String {
        return "java.lang.Char";
    }
}

object StringClass : RefType() {
    override fun toCode(): String {
        return "java.lang.String";
    }
}

class RefArray(val arrayType: ArrayType) : RefType() {
    override fun toCode(): String {
        return "${arrayType.componentType.toType().toCode()}[]";
    }
}

class RefClass(val declaredType: DeclaredType) : RefType() {
    override fun toCode(): String {
        val typeElement = declaredType.asElement() as TypeElement
        return "${typeElement.qualifiedName}";
    }
}

object Unknown : Type() {
    override fun toCode(): String {
        return "java.lang.Object";
    }
}

fun TypeMirror.toType(): Type {
    return when (this.kind) {
        TypeKind.BOOLEAN -> BaseBoolean
        TypeKind.BYTE -> BaseByte
        TypeKind.SHORT -> BaseShort
        TypeKind.INT -> BaseInt
        TypeKind.LONG -> BaseLong
        TypeKind.CHAR -> BaseChar
        TypeKind.FLOAT -> BaseFloat
        TypeKind.DOUBLE -> BaseDouble
        TypeKind.VOID -> BaseVoid
        TypeKind.ARRAY -> {
            val arrayType = this as ArrayType
            RefArray(arrayType)
        }
        TypeKind.DECLARED -> {
            val declaredType = this as DeclaredType
            val typeElement = declaredType.asElement() as TypeElement
            when (typeElement.qualifiedName.toString()) {
                "java.lang.Boolean" -> BooleanClass
                "java.lang.Byte" -> ByteClass
                "java.lang.Short" -> ShortClass
                "java.lang.Int" -> IntClass
                "java.lang.Long" -> LongClass
                "java.lang.Char" -> CharClass
                "java.lang.Float" -> FloatClass
                "java.lang.Double" -> DoubleClass
                "java.lang.Void" -> VoidClass
                "java.lang.String" -> StringClass
                else -> {
                    RefClass(declaredType)
                }
            }
        }
        else -> Unknown
    }
}