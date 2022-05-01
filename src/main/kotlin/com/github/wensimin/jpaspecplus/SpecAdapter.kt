package com.github.wensimin.jpaspecplus

import java.lang.reflect.Field
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate

interface SpecAdapter {

    fun <T> predicate(
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any,
        annotation: Annotation
    ): Predicate
}