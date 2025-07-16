package com.homeiot._5

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

val RESISTIVE_HIGH = 1300.0f
val RESISTIVE_LOW = 350.0f
//    val RESISTIVE_HIGH = 2660.0f
//    val RESISTIVE_LOW = 2100.0f

//    val CAPACITIVE_1_HIGH = 2205.0f
val CAPACITIVE_1_HIGH = 1600.0f
val CAPACITIVE_1_LOW = 1220.0f

val RESISTIVE_2_HIGH = 2700.0f
val RESISTIVE_2_LOW = 2000.0f
//    val RESISTIVE_2_HIGH = 1140.0f
//    val RESISTIVE_2_LOW = 350.0f

@Component
class DataConverter {

    fun convertReading(reading: Float, highLimit: Float, lowLimit: Float, invert: Boolean? = true): Float {
        val scaled = (reading - lowLimit) / (highLimit - lowLimit)
        val maybeInverted = if (invert == true) 1.0f - scaled else scaled
        return BigDecimal((maybeInverted * 100.0f).toString()).setScale(
            2,
            RoundingMode.HALF_UP
        ).toFloat()
    }

    fun convert(data: Data): Data {
        return Data(
            0,
            data.timestamp,
            data.airHumidity,
            data.airTemperature,
            data.brightness,
            convertReading(data.soilHumidity1, RESISTIVE_HIGH, RESISTIVE_LOW).coerceIn(0.0f, 100.0f),
            convertReading(data.soilHumidity2, CAPACITIVE_1_HIGH, CAPACITIVE_1_LOW).coerceIn(0.0f, 100.0f),
            convertReading(data.soilHumidity3, RESISTIVE_2_HIGH, RESISTIVE_2_LOW).coerceIn(
                0.0f,
                100.0f
            )
        )
    }

    fun average(previous: Data, current: Data): Data {
        return Data(
            0,
            current.timestamp,
            (previous.airHumidity + current.airHumidity) / 2,
            (previous.airTemperature + current.airTemperature) / 2,
            (previous.brightness + current.brightness) / 2,
            (previous.soilHumidity1 + current.soilHumidity1) / 2,
            (previous.soilHumidity2 + current.soilHumidity2) / 2,
            (previous.soilHumidity3 + current.soilHumidity3) / 2
        )
    }
}