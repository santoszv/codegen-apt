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

import javax.lang.model.element.AnnotationMirror

class PropertyModel(val propertyName: String, val field: FieldModel?, val getter: MethodModel?, val setter: MethodModel?, val superProperty: PropertyModel?) {

    val capitalizedName: String by lazy {
        "${propertyName.substring(0, 1).uppercase()}${propertyName.substring(1)}"
    }

    val propertyType: Type by lazy {
        field?.fieldType ?: getter?.methodResultType ?: superProperty?.propertyType ?: Unknown
    }

    val isId: Boolean by lazy {
        (field?.isId ?: false) || (getter?.isId ?: false) || (superProperty?.isId ?: false)
    }

    val isEmbeddedId: Boolean by lazy {
        (field?.isEmbeddedId ?: false) || (getter?.isEmbeddedId ?: false) || (superProperty?.isEmbeddedId ?: false)
    }

    val isVersion: Boolean by lazy {
        (field?.isVersion ?: false) || (getter?.isVersion ?: false) || (superProperty?.isVersion ?: false)
    }

    val isColumn: Boolean by lazy {
        (field?.isColumn ?: false) || (getter?.isColumn ?: false) || (superProperty?.isColumn ?: false)
    }

    val isJoinColumn: Boolean by lazy {
        (field?.isJoinColumn ?: false) || (getter?.isJoinColumn ?: false) || (superProperty?.isJoinColumn ?: false)
    }

    val isEmbedded: Boolean by lazy {
        (field?.isEmbedded ?: false) || (getter?.isEmbedded ?: false) || (superProperty?.isEmbedded ?: false)
    }

    val isGeneratedValue: Boolean by lazy {
        (field?.isGeneratedValue ?: false) || (getter?.isGeneratedValue ?: false) || (superProperty?.isGeneratedValue ?: false)
    }

    val isNotNull: Boolean by lazy {
        (field?.isNotNull ?: false) || (getter?.isNotNull ?: false) || (superProperty?.isNotNull ?: false)
    }

    val isInsertTimestamp: Boolean by lazy {
        (field?.isInsertTimestamp ?: false) || (getter?.isInsertTimestamp ?: false) || (superProperty?.isInsertTimestamp ?: false)
    }

    val isUpdateTimestamp: Boolean by lazy {
        (field?.isUpdateTimestamp ?: false) || (getter?.isUpdateTimestamp ?: false) || (superProperty?.isUpdateTimestamp ?: false)
    }

    val isAlternativeId: Boolean by lazy {
        (field?.isAlternativeId ?: false) || (getter?.isAlternativeId ?: false) || (superProperty?.isAlternativeId ?: false)
    }

    val isInsertable: Boolean by lazy {
        (field?.isInsertable ?: false) || (getter?.isInsertable ?: false) || (superProperty?.isInsertable ?: false)
    }

    val isUpdatable: Boolean by lazy {
        (field?.isUpdatable ?: false) || (getter?.isUpdatable ?: false) || (superProperty?.isUpdatable ?: false)
    }

    val isTimestamp: Boolean by lazy {
        isInsertTimestamp || isUpdateTimestamp
    }

    val isManaged: Boolean by lazy {
        isId || isVersion || isTimestamp
    }

    val validations: List<AnnotationMirror> by lazy {
        (field?.validations ?: emptyList()) + (getter?.validations ?: emptyList()) + (superProperty?.validations ?: emptyList())
    }
}