package com.github.wensimin.jpaspecplus.test

import com.github.wensimin.jpaspecplus.Ignore
import com.github.wensimin.jpaspecplus.findBySpec
import com.github.wensimin.jpaspecplus.findPageBySpec
import com.github.wensimin.jpaspecplus.specification.Greater
import com.github.wensimin.jpaspecplus.specification.Like
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import javax.persistence.Entity
import javax.persistence.Id


data class Query(
    val id: String? = null,
    @Like(Like.Type.ALL)
    val name: String? = null,
    @Greater("number")
    val number: Int? = null,
    @Ignore
    val nothing: String? = null,
)

@Entity
class Data(
    @Id
    val id: String,
    val name: String,
    val number: Int,
    val nothing: String,
)

@SpringBootApplication
class App

@SpringBootTest
class SpecTest {

    @Test
    fun test(@Autowired dataDao: DataDao) {
        dataDao.saveAll(
            listOf(
                Data("1", "diyige", 0, "oaaz"),
                Data("2", "dierge", 30, "oaaz"),
                Data("3", "disange", 1650, "oaaz")
            )
        )
        assert(dataDao.findBySpec().count() == 3)
        assert(dataDao.findBySpec(Query(id = "2")).count() == 1)
        assert(dataDao.findBySpec(Query(name = "er")).count() == 1)
        assert(dataDao.findBySpec(Query(number = 1000)).count() == 1)
        assert(dataDao.findBySpec(Query(number = 30)).count() == 2)
        assert(dataDao.findBySpec(Query(name = "er", number = 1000)).isEmpty())
        dataDao.findPageBySpec(Query(number = 30), PageRequest.of(0, 1)).also {
            assert(it.totalElements == 2L)
            assert(it.content.size == 1)
        }

    }
}