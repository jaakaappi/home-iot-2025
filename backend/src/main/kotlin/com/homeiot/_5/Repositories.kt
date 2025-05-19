package com.homeiot._5

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface DataRepository : CrudRepository<Data, Int> {
    fun findFirstByOrderByTimestampDesc(): Data?

    @Query(
        "SELECT * FROM data WHERE timestamp >= ?1",
        nativeQuery = true
    )
    fun getAfterTimestamp(timeStamp: Long): List<Data>
}

interface IrrigationRepository : CrudRepository<Irrigation, Int>