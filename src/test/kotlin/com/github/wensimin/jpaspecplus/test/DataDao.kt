package com.github.wensimin.jpaspecplus.test

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface DataDao : JpaRepository<Data, String>, JpaSpecificationExecutor<Data>