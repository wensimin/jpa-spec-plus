package com.github.wensimin.jpaspecplus.test

import com.github.wensimin.jpaspecplus.Ignore
import com.github.wensimin.jpaspecplus.JoinPrefix
import com.github.wensimin.jpaspecplus.findBySpec
import com.github.wensimin.jpaspecplus.findPageBySpec
import com.github.wensimin.jpaspecplus.specification.Eq
import com.github.wensimin.jpaspecplus.specification.Greater
import com.github.wensimin.jpaspecplus.specification.In
import com.github.wensimin.jpaspecplus.specification.Like
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToOne


data class Query(
    val id: String? = null,
    @Like(Like.Type.ALL)
    val name: String? = null,
    @Greater("number")
    val number: Int? = null,
    @In("number")
    val numbers: List<Int> = emptyList(),
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


@Entity
class Student(
    @Id
    val name: String,
    @ManyToOne
    val sClass: SClass
)

@Entity
class Teacher(
    @Id
    val name: String,
)

@Entity
class SClass(
    @Id val name: String,
    @OneToOne
    val teacher: Teacher
)

data class StudentQuery(
    val name: String? = null,
    @Eq("name")
    @JoinPrefix("sClass")
    val className: String? = null,
    @Eq("name")
    @JoinPrefix("sClass.teacher")
    val teacherName: String? = null
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
        assert(dataDao.findBySpec(Query()).count() == 3)
        assert(dataDao.findBySpec(Query(id = "2")).count() == 1)
        assert(dataDao.findBySpec(Query(name = "er")).count() == 1)
        assert(dataDao.findBySpec(Query(number = 1000)).count() == 1)
        assert(dataDao.findBySpec(Query(number = 30)).count() == 2)
        assert(dataDao.findBySpec(Query(numbers = listOf(0, 30, 1640))).count() == 2)
        assert(dataDao.findBySpec(Query(name = "er", number = 1000)).isEmpty())
        dataDao.findPageBySpec(Query(number = 30), PageRequest.of(0, 1)).also {
            assert(it.totalElements == 2L)
            assert(it.content.size == 1)
        }
    }

    @Test
    fun joinTest(
        @Autowired studentDao: StudentDao,
        @Autowired sclassDao: SclassDao,
        @Autowired teacherDao: TeacherDao
    ) {
        val teacherA = Teacher("A老师")
        val teacherB = Teacher("B老师")
        teacherDao.saveAll(listOf(teacherA, teacherB))
        val sclassA = SClass("班级A", teacherA)
        val sclassB = SClass("班级B", teacherB)
        sclassDao.saveAll(listOf(sclassA, sclassB))
        val studentA = Student("a班学生", sclassA)
        val studentB = Student("b班学生", sclassB)
        studentDao.saveAll(listOf(studentA, studentB))
        assert(studentDao.findBySpec().count() == 2)
        assert(studentDao.findBySpec(StudentQuery("a班学生")).count() == 1)
        assert(studentDao.findBySpec(StudentQuery(className = "班级B")).count() == 1)
        assert(studentDao.findBySpec(StudentQuery(className = "无效班级")).isEmpty())
        assert(studentDao.findBySpec(StudentQuery(teacherName = "A老师")).count() == 1)
    }
}