package com.github.wensimin.jpaspecplus

import com.github.wensimin.jpaspecplus.specification.Eq
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.util.ObjectUtils
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.criteria.*
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.kotlinProperty


/**
 * 简写版本findBySpec
 */
fun <T> JpaSpecificationExecutor<T>.findBySpec(query: Any? = null): MutableList<T> = findAll(query.toSpecification())

fun <T> JpaSpecificationExecutor<T>.findPageBySpec(query: Any? = null, pageRequest: PageRequest): Page<T> =
    findAll(query.toSpecification(), pageRequest)


/**
 * 基于对象生成查询规范
 * 可以使用注解标记成员需要的查询方式
 * 未进行标记的成员会生成 eq 查询规范
 * 空值成员会进行跳过
 */
fun <T> Any?.toSpecification(): Specification<T> {
    /**
     * 通过当前类的成员以及注解生成查询参数
     */
    val fields = this?.javaClass?.declaredFields
    return Specification { root, query, criteriaBuilder ->
        // add join
        @Suppress("UNCHECKED_CAST")
        root as From<Any, Any>
        val joinMap = createJoinMap(root.model.bindableJavaType, root, query)
        val specs = mutableListOf<Predicate>()
        fields?.forEach {
            //忽略
            if (it.isAnnotationPresent(Ignore::class.java)) {
                return@forEach
            }
            val value = it.kotlinProperty?.javaGetter?.invoke(this)
            //忽略空值项
            if (ObjectUtils.isEmpty(value)) {
                return@forEach
            }
            // 声明为非空过编译
            value!!
            // 判断是否为join字段
            val join = it.getAnnotation(JoinPrefix::class.java)?.let { join -> joinMap[join.prefix] }
            it.annotations
                //过滤非 query 运算符 注解
                .filter { annotation -> annotation.annotationClass.hasAnnotation<Query>() }.ifEmpty {
                    // 没有任何query注解则默认eq
                    specs.add(
                        SpecConfig.specificationAdapters[Eq::class]!!.predicate(
                            join ?: root,
                            criteriaBuilder,
                            it,
                            value,
                            Eq(it.name)
                        )
                    )
                    emptyList()
                }.forEach { annotation ->
                    val specAdapter = SpecConfig.specificationAdapters[annotation.annotationClass]
                        ?: throw RuntimeException("没有处理方式的query运算符: ${annotation.annotationClass.simpleName}")
                    specs.add(specAdapter.predicate(join ?: root, criteriaBuilder, it, value, annotation))
                }
        }
        query.where(*specs.toTypedArray()).restriction
    }
}

/**
 * 通过实体创建join map
 */
private fun createJoinMap(
    entity: Class<*>,
    root: From<Any, Any>,
    query: CriteriaQuery<*>,
    prefix: String = ""
): MutableMap<String, Path<*>> {
    val joinMap = mutableMapOf<String, Path<*>>()
    entity.declaredFields.forEach { field ->
        // 重复代码,未能找到好的解决方式 原因为注解的非继承问题
        field.getAnnotation(ManyToOne::class.java)?.let {
            if (it.fetch == FetchType.EAGER) {
                val targetClass = if (it.targetEntity == Void::class) field.type else it.targetEntity.java
                val simpleName = field.name
                val key = "${prefix}${simpleName}"
                val from = createJoin(root, query, simpleName)
                joinMap[key] = from
                // 递归建立map
                joinMap.putAll(createJoinMap(targetClass, from, query, "$simpleName."))
            }
        }
        field.getAnnotation(OneToOne::class.java)?.let {
            if (it.fetch == FetchType.EAGER) {
                val targetClass = if (it.targetEntity == Void::class) field.type else it.targetEntity.java
                val simpleName = field.name
                val key = "${prefix}${simpleName}"
                val from = createJoin(root, query, simpleName)
                joinMap[key] = from
                // 递归建立map
                joinMap.putAll(createJoinMap(targetClass, from, query, "$simpleName."))
            }
        }
    }
    return joinMap
}

fun createJoin(root: From<Any, Any>, query: CriteriaQuery<*>, targetName: String): From<Any, Any> {
    // 分页使用count查询时会产生fetch错误,所以使用join
    return if (query.resultType.name == "java.lang.Long") {
        root.join(targetName, JoinType.LEFT)
    } else {
        @Suppress("UNCHECKED_CAST")
        root.fetch<Any, Any>(targetName, JoinType.LEFT) as From<Any, Any>
    }
}


/**
 * 忽略标记，该字段不会用于查询
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Ignore

/**
 * 元注解,表示被该注解注释的注解为查询表达式
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention
@MustBeDocumented
annotation class Query

/**
 * join字段前缀
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class JoinPrefix(val prefix: String = "")