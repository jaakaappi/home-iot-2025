package com.homeiot._5

import org.springframework.data.repository.CrudRepository

interface DataRepository : CrudRepository<Data, Int>
interface IrrigationRepository : CrudRepository<Irrigation, Int>