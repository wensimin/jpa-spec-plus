package com.github.wensimin.jpaspecplus.specification

import com.github.wensimin.jpaspecplus.Query
import com.github.wensimin.jpaspecplus.SpecAdapter
import java.lang.reflect.Field
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate

/**
 * 指目标值应该小于标注的属性值
 * @param fieldName 必须,对应的查询目标field
 * 本注解只应该使用在实现了 [java.lang.Comparable] 接口的对象上
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
@Query
annotation class Less(val fieldName: String, val eq: Boolean = true)
class LessSpec : SpecAdapter {
    override fun <T> predicate(
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any,
        annotation: Annotation
    ): Predicate {
        annotation as Less
        @Suppress("UNCHECKED_CAST")
        value as Comparable<Any>
        return when (annotation.eq) {
            true -> criteriaBuilder.lessThanOrEqualTo(root.get(annotation.fieldName), value)
            false -> criteriaBuilder.lessThan(root.get(annotation.fieldName), value)
        }
    }

}
