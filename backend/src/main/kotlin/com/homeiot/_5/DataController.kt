package com.homeiot._5

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

enum class TimeRange {
    hour, day,
    threeDays, week,
    month, all
}


@RestController
class DataController(
    private val dataRepository: DataRepository,
    private val irrigationRepository: IrrigationRepository,
    private val dataConverter: DataConverter
) {

    @PostMapping("/data")
    fun saveData(@RequestBody data: String): String {
        val values = data.split(" ").toList()

        if (values.count() != 6) throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Wrong number of data fields in parsed data $data, count ${values.count()}"
        )

        val airHumidity = values[0].toFloatOrNull()
        val airTemperature = values[1].toFloatOrNull()
        val brightness = values[2].toFloatOrNull()
        val soilHumidity1 = values[3].toFloatOrNull()
        val soilHumidity2 = values[4].toFloatOrNull()
        val soilHumidity3 = values[5].toFloatOrNull()

        val valueList = listOf(
            airHumidity,
            airTemperature,
            brightness,
            soilHumidity1,
            soilHumidity2,
            soilHumidity3
        )

        if (valueList.any { it == null }
        ) throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Could not parse some value to float, input $data, values $valueList"
        )

        dataRepository.save(
            Data(
                timestamp = Instant.now().toEpochMilli(),
                airHumidity = airHumidity!!,
                airTemperature = airTemperature!!,
                brightness = brightness!!,
                soilHumidity1 = soilHumidity1!!,
                soilHumidity2 = soilHumidity2!!,
                soilHumidity3 = soilHumidity3!!
            )
        )

        // TODO calculate and return irrigation command
        //return "I"
        return ""
    }


    @PostMapping("/irrigation")
    fun saveIrrigation() {
        irrigationRepository.save(
            Irrigation(
                timestamp = Instant.now().toEpochMilli()
            )
        )
    }

    @GetMapping("", "/", "/{timeRange}")
    fun getPage(@PathVariable timeRange: TimeRange?): ModelAndView {
        val data: List<Any> = when (timeRange) {
            null -> dataRepository.findAllByOrderByTimestampDesc().toList()
            TimeRange.hour -> dataRepository.getAfterTimestamp(
                Clock.systemUTC().instant().minus(60, ChronoUnit.MINUTES).toEpochMilli()
            )

            TimeRange.day -> dataRepository.getAfterTimestamp(
                Clock.systemUTC().instant().minus(1, ChronoUnit.DAYS).toEpochMilli()
            )

            TimeRange.threeDays -> dataRepository.getAfterTimestamp(
                Clock.systemUTC().instant().minus(3, ChronoUnit.DAYS).toEpochMilli()
            )

            TimeRange.week -> dataRepository.getAfterTimestamp(
                Clock.systemUTC().instant().minus(7, ChronoUnit.DAYS).toEpochMilli()
            )

            TimeRange.month -> dataRepository.getAfterTimestamp(
                Clock.systemUTC().instant().minus(30, ChronoUnit.DAYS).toEpochMilli()
            )

            TimeRange.all -> dataRepository.findAll().toList()
        }.map { if (it.timestamp > 1747849313389) dataConverter.convert(it) else it }

        val latestReading = dataRepository.findFirstByOrderByTimestampDesc()
            ?.let { if (it.timestamp > 1747849313389) dataConverter.convert(it) else it }
        val updateTimestampText =
            if (latestReading != null) SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(latestReading.timestamp)) else ""

        return ModelAndView(
            "index",
            mapOf("data" to data, "updateTimestamp" to updateTimestampText, "latestReading" to latestReading)
        )
    }
}