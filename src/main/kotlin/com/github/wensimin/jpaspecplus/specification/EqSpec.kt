package com.github.wensimin.jpaspecplus.specification

import com.github.wensimin.jpaspecplus.Query
import com.github.wensimin.jpaspecplus.SpecAdapter
import java.lang.reflect.Field
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate


/**
 * 相等
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
@Query
annotation class Eq(val fieldName: String = "", val igCase: Boolean = false)

class EqSpec : SpecAdapter {
    override fun <T> predicate(
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any,
        annotation: Annotation
    ): Predicate {
        annotation as Eq
        val filedName = annotation.fieldName.ifEmpty { field.name }
        return if (annotation.igCase && value is String) {
            criteriaBuilder.equal(criteriaBuilder.upper(root.get(filedName)), value.uppercase())
        } else {
            criteriaBuilder.equal(root.get<Any>(filedName), value)
        }
    }

}
