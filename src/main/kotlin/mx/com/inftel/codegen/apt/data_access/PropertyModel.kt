package mx.com.inftel.codegen.apt.data_access

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ExecutableElement

class PropertyModel {

    var propertyName = ""
    var capitalizedName = ""

    lateinit var propertyGetter: ExecutableElement
    lateinit var propertySetter: ExecutableElement

    val isGetterInitialized: Boolean
        get() = this::propertyGetter.isInitialized

    val isGeneratedCode: Boolean
        get() = this::propertyGetter.isInitialized && this::propertySetter.isInitialized

    var isColumn = false
    var isJoinColumn = false

    var isInsertable = true
    var isUpdatable = true

    var isId = false
    var isGeneratedValue = false
    var isVersion = false
    var isInsertTimestamp = false
    var isUpdateTimestamp = false
    var isAltId = false

    val isTimestamp: Boolean
        get() = isInsertTimestamp || isUpdateTimestamp

    val isManaged: Boolean
        get() = isId || isVersion || isTimestamp

    val validations = mutableListOf<AnnotationMirror>()

    var joinModel: EntityModel? = null
}