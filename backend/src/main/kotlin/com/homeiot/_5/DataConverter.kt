package com.homeiot._5

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class DataConverter {
    val RESISTIVE_HIGH = 2400.0f
    val RESISTIVE_LOW = 0.0f
    val CAPACITIVE_1_HIGH = 4095.0f
    val CAPACITIVE_1_LOW = 1500.0f
    val CAPACITIVE_2_HIGH = 4095.0f
    val CAPACITIVE_2_LOW = 2600.0f

    private fun convertReading(reading: Float, highLimit: Float, lowLimit: Float, invert: Boolean? = true): Float {
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
            convertReading(data.soilHumidity1, RESISTIVE_HIGH, RESISTIVE_LOW, false),
            convertReading(data.soilHumidity2, CAPACITIVE_1_HIGH, CAPACITIVE_1_LOW),
            convertReading(data.soilHumidity3, CAPACITIVE_2_HIGH, CAPACITIVE_2_LOW)
        )
    }
}