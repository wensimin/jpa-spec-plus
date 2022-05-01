import org.springframework.data.jpa.domain.Specification
import org.springframework.util.ObjectUtils
import java.lang.reflect.Field
import javax.persistence.criteria.*
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.kotlinProperty

/**
 * 查询参数接口,实现了一组依赖注解的查询规范
 * 未进行标记的成员会生成 eq 查询规范
 * 空值成员会进行跳过
 */
interface QueryParam {
    /**
     * 通过当前类的成员以及注解生成查询参数
     */
    fun <T> toSpecification(): Specification<T> {
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
                    .filter { annotation -> annotation.annotationClass.hasAnnotation<Query>() }
                    .ifEmpty {
                        // 没有任何query注解则默认eq
                        specs.add(eqSpecification(join ?: root, criteriaBuilder, it, value, null))
                        emptyList()
                    }
                    .forEach { annotation ->
                        specs.add(
                            when (annotation) {
                                is Eq ->
                                    eqSpecification(join ?: root, criteriaBuilder, it, value, annotation)
                                is Like ->
                                    likeSpecification(annotation, join ?: root, criteriaBuilder, it, value)
                                is Less ->
                                    lessSpecification(annotation, join ?: root, criteriaBuilder, value)
                                is Greater ->
                                    greaterSpecification(annotation, join ?: root, criteriaBuilder, value)
                                else -> throw RuntimeException("没有处理方式的query运算符: ${annotation.annotationClass.simpleName}")
                            }
                        )
                    }
            }
            query.where(*specs.toTypedArray()).restriction
        }
    }

    private fun <T> createJoinMap(root: Root<T>, query: CriteriaQuery<*>): MutableMap<String, Path<*>> {
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
        return if (query.resultType.name == "java.lang.Long") {
            root.join<Any, Any>(fetch.attrName, fetch.joinType)
        } else {
            root.fetch<Any, Any>(fetch.attrName, fetch.joinType) as Path<*>
        }
    }


    /**
     * 生成大于目标规范
     */
    private fun <T> greaterSpecification(
        greater: Greater,
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        value: Any
    ): Predicate {
        @Suppress("UNCHECKED_CAST")
        value as Comparable<Any>
        return when (greater.eq) {
            true -> criteriaBuilder.greaterThanOrEqualTo(root.get(greater.fieldName), value)
            false -> criteriaBuilder.greaterThan(root.get(greater.fieldName), value)
        }
    }

    /**
     * 生成小于目标查询规范
     */
    private fun <T> lessSpecification(
        less: Less,
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        value: Any
    ): Predicate {
        @Suppress("UNCHECKED_CAST")
        value as Comparable<Any>
        return when (less.eq) {
            true -> criteriaBuilder.lessThanOrEqualTo(root.get(less.fieldName), value)
            false -> criteriaBuilder.lessThan(root.get(less.fieldName), value)
        }
    }

    /**
     * 生成eq查询规范
     */
    private fun <T> eqSpecification(
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any,
        eq: Eq? = null
    ): Predicate {
        val filedName = eq?.fieldName.orEmpty().ifEmpty { field.name }
        return if (eq?.igCase == true && value is String) {
            criteriaBuilder.equal(criteriaBuilder.upper(root.get(filedName)), value.uppercase())
        } else {
            criteriaBuilder.equal(root.get<Any>(filedName), value)
        }
    }


    /**
     * 生成like查询方式
     */
    private fun <T> likeSpecification(
        like: Like,
        root: Path<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any
    ): Predicate {
        val filedName = like.fieldName.ifEmpty { field.name }
        // like must string
        if (value !is String) throw TypeCastException("like must string")
        val predicateSet = mutableSetOf<Predicate>()
        // 若是分隔符存在则分割关键字查询
        if (like.separator.isNotEmpty()) {
            value.split(like.separator).forEach {
                criteriaBuilder.like(root.get(filedName), getLikeValue(like.type, it))
                predicateSet.add(criteriaBuilder.like(root.get(filedName), getLikeValue(like.type, it)))
            }
        } else {
            predicateSet.add(criteriaBuilder.like(root.get(filedName), getLikeValue(like.type, value)))
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


/**
 * 忽略标记，该字段不会用于查询
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Ignore

/**
 * 相等
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
@Query
annotation class Eq(val fieldName: String = "", val igCase: Boolean = false)


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