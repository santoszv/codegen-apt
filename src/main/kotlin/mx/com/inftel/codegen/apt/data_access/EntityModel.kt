package mx.com.inftel.codegen.apt.data_access

class EntityModel {

    var entityQualifiedName = ""
    val entityPackageName: String
        get() = entityQualifiedName.substringBeforeLast('.', "")
    val entitySimpleName: String
        get() = entityQualifiedName.substringAfterLast('.')

    var crudQualifiedName = ""
    val crudPackageName: String
        get() = crudQualifiedName.substringBeforeLast('.', "")
    val crudSimpleName: String
        get() = crudQualifiedName.substringAfterLast('.')

    var dtoQualifiedName = ""
    val dtoPackageName: String
        get() = dtoQualifiedName.substringBeforeLast('.', "")
    val dtoSimpleName: String
        get() = dtoQualifiedName.substringAfterLast('.')

    val isGeneratedCodeDto: Boolean
        get() = dtoQualifiedName.isNotBlank()
    val isGeneratedCodeCrud: Boolean
        get() = isGeneratedCodeDto && crudQualifiedName.isNotBlank()

    val properties = mutableMapOf<String, PropertyModel>()
}