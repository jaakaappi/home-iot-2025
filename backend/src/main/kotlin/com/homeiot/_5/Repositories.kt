package com.homeiot._5

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface DataRepository : CrudRepository<Data, Int> {
    fun findFirstByOrderByTimestampDesc(): Data?
    fun findFirstByTimestampLessThan(timeStamp: Long): Data?
    fun findFirstByTimestampMoreThan(timeStamp: Long): Data?

    @Query(
        "SELECT * FROM data WHERE timestamp >= ?1",
        nativeQuery = true
    )
    fun getAfterTimestamp(timeStamp: Long): List<Data>

    fun findAllByOrderByTimestampDesc(): List<Data>
}

interface IrrigationRepository : CrudRepository<Irrigation, Int> {
    fun findFirstByOrderByTimestampDesc(): Irrigation
}