package com.github.wensimin.jpaspecplus.specification

import com.github.wensimin.jpaspecplus.Query
import com.github.wensimin.jpaspecplus.SpecAdapter
import java.lang.reflect.Field
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate

/**
 * in 表达式查询,标注的field需要为 Collection
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
@Query
annotation class In(val fieldName: String = "")
class InSpec : SpecAdapter {
    override fun <T> predicate(
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any,
        annotation: Annotation
    ): Predicate {
        annotation as In
        val filedName = annotation.fieldName.ifEmpty { field.name }
        value as Collection<*>
        val inSpec = criteriaBuilder.`in`(root.get<Any>(filedName))
        value.forEach { inSpec.value(it) }
        return inSpec
    }

}