package com.github.wensimin.jpaspecplus.specification

import com.github.wensimin.jpaspecplus.Query
import com.github.wensimin.jpaspecplus.SpecAdapter
import java.lang.reflect.Field
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate


/**
 * 指目标值应该大于标注的属性值
 *  @param fieldName 必须,对应的查询目标field
 * 本注解只应该使用在实现了 [java.lang.Comparable] 接口的对象上
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
@Query
annotation class Greater(val fieldName: String, val eq: Boolean = true)
class GreaterSpec : SpecAdapter {
    override fun <T> predicate(
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any,
        annotation: Annotation
    ): Predicate {
        annotation as Greater
        @Suppress("UNCHECKED_CAST")
        value as Comparable<Any>
        return when (annotation.eq) {
            true -> criteriaBuilder.greaterThanOrEqualTo(root.get(annotation.fieldName), value)
            false -> criteriaBuilder.greaterThan(root.get(annotation.fieldName), value)
        }
    }

}
