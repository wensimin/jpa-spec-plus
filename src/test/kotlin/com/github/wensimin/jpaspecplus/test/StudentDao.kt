package com.github.wensimin.jpaspecplus.test

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface StudentDao : JpaRepository<Student, String>, JpaSpecificationExecutor<Student>

interface SclassDao : JpaRepository<SClass, String>, JpaSpecificationExecutor<SClass>

interface TeacherDao : JpaRepository<Teacher, String>, JpaSpecificationExecutor<Teacher>