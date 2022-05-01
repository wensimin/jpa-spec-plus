package com.github.wensimin.jpaspecplus

import com.github.wensimin.jpaspecplus.specification.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.util.ObjectUtils
import javax.persistence.criteria.*
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.kotlinProperty

val specificationAdapters = mutableMapOf(
    Eq::class to EqSpec(),
    Like::class to LikeSpec(),
    Greater::class to GreaterSpec(),
    Less::class to LessSpec()
)

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
fun <T> Any?.toSpecification(): Specification<T>? {
    if (this == null) return null
    /**
     * 通过当前类的成员以及注解生成查询参数
     */
    val fields = this.javaClass.declaredFields
    return Specification { root, query, criteriaBuilder ->
        // add join
        val joinMap = createJoinMap(root, query)
        val specs = mutableListOf<Predicate>()
        fields.forEach {
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
            // 获取该字段是否为join字段
            val join = it.getAnnotation(JoinPath::class.java)?.let { joinPath -> joinMap[joinPath.attrName] }
            it.annotations
                //过滤非 query 运算符 注解
                .filter { annotation -> annotation.annotationClass.hasAnnotation<Query>() }.ifEmpty {
                    // 没有任何query注解则默认eq
                    specs.add(
                        specificationAdapters[Eq::class]!!.predicate(
                            join ?: root,
                            criteriaBuilder,
                            it,
                            value,
                            Eq(it.name)
                        )
                    )
                    emptyList()
                }.forEach { annotation ->
                    val specAdapter = specificationAdapters[annotation.annotationClass]
                        ?: throw RuntimeException("没有处理方式的query运算符: ${annotation.annotationClass.simpleName}")
                    specs.add(specAdapter.predicate(join ?: root, criteriaBuilder, it, value, annotation))
                }
        }
        query.where(*specs.toTypedArray()).restriction
    }
}


private fun <T> Any.createJoinMap(root: Root<T>, query: CriteriaQuery<*>): MutableMap<String, Path<*>> {
    val joinMap = mutableMapOf<String, Path<*>>()
    this.javaClass.getAnnotation(Joins::class.java)?.also {
        it.joins.forEach { fetch ->
            joinMap[fetch.attrName] = createJoin(root, query, fetch)
        }
    }
    this.javaClass.getAnnotation(Join::class.java)?.also {
        joinMap[it.attrName] = createJoin(root, query, it)
    }
    return joinMap
}

private fun <T> createJoin(root: Root<T>, query: CriteriaQuery<*>, fetch: Join): Path<*> {
    // 分页使用count查询时会产生fetch错误,所以使用join
    return if (query.resultType.name == "java.lang.Long") {
        root.join<Any, Any>(fetch.attrName, fetch.joinType)
    } else {
        root.fetch<Any, Any>(fetch.attrName, fetch.joinType) as Path<*>
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
 * 建立连接关系
 * @param attrName 目标实体字段
 * @param joinType join方式
 */
@Target(AnnotationTarget.CLASS)
@Retention
@MustBeDocumented
annotation class Join(val attrName: String, val joinType: JoinType = JoinType.LEFT)

/**
 * 批量连接关系
 * @see Join
 */
@Target(AnnotationTarget.CLASS)
@Retention
@MustBeDocumented
annotation class Joins(val joins: Array<Join>)

/**
 * 连接查询的path
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class JoinPath(val attrName: String)

/**
 * 元注解,表示被该注解注释的注解为查询表达式
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention
@MustBeDocumented
annotation class Query