package com.github.wensimin.jpaspecplus.test

import com.github.wensimin.jpaspecplus.Ignore
import com.github.wensimin.jpaspecplus.findBySpec
import com.github.wensimin.jpaspecplus.specification.Greater
import com.github.wensimin.jpaspecplus.specification.Like
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
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
        dataDao.findBySpec(Query()).first().id

        assert(dataDao.findBySpec(Query()).count() == 3)
        assert(dataDao.findBySpec(Query(id = "2")).count() == 1)
        assert(dataDao.findBySpec(Query(name = "er")).count() == 1)
        assert(dataDao.findBySpec(Query(number = 1000)).count() == 1)
        assert(dataDao.findBySpec(Query(number = 30)).count() == 2)

    }
}