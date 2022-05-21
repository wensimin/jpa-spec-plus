package com.github.wensimin.jpaspecplus

import com.github.wensimin.jpaspecplus.specification.*

/**
 * spec配置相关对象
 */
object SpecConfig {
    /**
     * 不同注解的处理器,需要新增自定义查询注解可以使用这个
     */
    val specificationAdapters = mutableMapOf(
        Eq::class to EqSpec(),
        Like::class to LikeSpec(),
        Greater::class to GreaterSpec(),
        Less::class to LessSpec(),
        In::class to InSpec()
    )
}