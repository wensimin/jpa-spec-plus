package com.github.wensimin.jpaspecplus.specification

import com.github.wensimin.jpaspecplus.Query
import com.github.wensimin.jpaspecplus.SpecAdapter
import java.lang.reflect.Field
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate

/**
 * like查询
 * 本注解仅支持String
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
@Query
annotation class Like(
    /**
     * like模式
     */
    val type: Type = Type.START,
    val fieldName: String = "",
    /**
     * 分割符,如果该属性不为空则会对查询参数进行分割 and 匹配
     */
    val separator: String = ""
) {

    enum class Type {
        START, END, ALL
    }
}

class LikeSpec : SpecAdapter {
    override fun <T> predicate(
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any,
        annotation: Annotation
    ): Predicate {
        annotation as Like
        val filedName = annotation.fieldName.ifEmpty { field.name }
        // like must string
        if (value !is String) throw TypeCastException("like must string")
        val predicateSet = mutableSetOf<Predicate>()
        // 若是分隔符存在则分割关键字查询
        if (annotation.separator.isNotEmpty()) {
            value.split(annotation.separator).forEach {
                criteriaBuilder.like(root.get(filedName), getLikeValue(annotation.type, it))
                predicateSet.add(criteriaBuilder.like(root.get(filedName), getLikeValue(annotation.type, it)))
            }
        } else {
            predicateSet.add(criteriaBuilder.like(root.get(filedName), getLikeValue(annotation.type, value)))
        }
        return criteriaBuilder.and(*predicateSet.toTypedArray())
    }

    private fun getLikeValue(type: Like.Type, value: String): String {
        return when (type) {
            Like.Type.START -> "$value%"
            Like.Type.END -> "%$value"
            Like.Type.ALL -> "%$value%"
        }
    }
}