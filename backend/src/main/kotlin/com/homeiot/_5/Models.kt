package com.homeiot._5

import jakarta.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "data")
data class Data(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int = 0,
    val timestamp: Timestamp,
    val airHumidity: Float,
    val airTemperature: Float,
    val brightness: Float,
    val soilHumidity1: Float,
    val soilHumidity2: Float,
    val soilHumidity3: Float,
)

@Entity
@Table(name = "irrigations")
data class Irrigations(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int,
    val timestamp: Timestamp,
)