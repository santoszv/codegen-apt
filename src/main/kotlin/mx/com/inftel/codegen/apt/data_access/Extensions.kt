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

import java.beans.Introspector
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

val ExecutableElement.capitalizedName: String
    get() {
        return when {
            simpleName.startsWith("get") -> simpleName.substring(3)
            simpleName.startsWith("set") -> simpleName.substring(3)
            simpleName.startsWith("is") -> simpleName.substring(2)
            else -> throw RuntimeException()
        }
    }

val ExecutableElement.propertyName: String
    get() {
        return Introspector.decapitalize(capitalizedName)
    }

val TypeMirror.asString: String
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