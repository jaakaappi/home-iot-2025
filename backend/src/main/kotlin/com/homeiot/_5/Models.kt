package com.homeiot._5

import jakarta.persistence.*

@Entity
@Table(name = "data")
data class Data(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int = 0,
    val timestamp: Long,
    val airHumidity: Float,
    val airTemperature: Float,
    val brightness: Float,
    /** Resistive */
    val soilHumidity1: Float,
    /** Capacitive PCB */
    val soilHumidity2: Float,
    /** Capacitive Metal */
    val soilHumidity3: Float,
)

@Entity
@Table(name = "irrigations")
data class Irrigation(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int = 0,
    val timestamp: Long,
)