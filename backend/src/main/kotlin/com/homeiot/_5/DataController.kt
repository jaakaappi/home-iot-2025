package com.homeiot._5

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

enum class TimeRange {
    twoHours, day,
    threeDays, week,
    month, all
}


@RestController
class DataController(
    private val dataRepository: DataRepository,
    private val irrigationRepository: IrrigationRepository,
    private val dataConverter: DataConverter
) {

    val logger: org.slf4j.Logger = LoggerFactory.getLogger(javaClass);

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

        val latestReading = dataRepository.findFirstByOrderByTimestampDesc()
            ?.let { if (it.timestamp > 1747849313389) dataConverter.convert(it) else it }


        if (latestReading != null) {
            val convertedSensorMoisture =
                dataConverter.convertReading(soilHumidity2, CAPACITIVE_1_HIGH, CAPACITIVE_1_LOW)
            logger.info("Sensor combined moisture $convertedSensorMoisture")

            if (convertedSensorMoisture <= 60) {
                val latestIrrigation = irrigationRepository.findFirstByOrderByTimestampDesc()

                if (latestIrrigation != null) {
                    val duration = Instant.now()
                        .toEpochMilli() - latestIrrigation.timestamp
                    if (duration > 3 * 60 * 60 * 1000
                    ) {
                        logger.info("Irrigation time limit passed, ${prettyPrintDuration(Duration.ofMillis(duration))} since last")

                        val readingBeforeLastIrrigation =
                            dataRepository.findFirstByTimestampLessThan(latestIrrigation.timestamp)
                                ?.let { dataConverter.convert(it) }
                        val readingAfterLastIrrigation =
                            dataRepository.findFirstByTimestampGreaterThan(latestIrrigation.timestamp)
                                ?.let { dataConverter.convert(it) }

                        if (readingAfterLastIrrigation == null) {
                            logger.error("Missing readings after last irrigation")
                            return ""
                        }

                        if (readingBeforeLastIrrigation != null) {
                            if (readingAfterLastIrrigation.soilHumidity2 <= readingBeforeLastIrrigation.soilHumidity2) {
                                logger.error(
                                    "Humidity has not increased after last irrigation at ${
                                        Instant.ofEpochMilli(latestIrrigation.timestamp)
                                    }: ${readingAfterLastIrrigation.soilHumidity2} vs. before ${readingBeforeLastIrrigation.soilHumidity2}!"
                                )
                            } else {
                                logger.info("Irrigating!")
                                saveIrrigation()
                                return "I"
                            }
                        } else {
                            logger.info("Irrigating!")
                            saveIrrigation()
                            return "I"
                        }
                    } else {
                        logger.warn("Soil dry but under 3h from last irrigation")
                    }
                } else {
                    logger.info("Irrigating!")
                    saveIrrigation()
                    return "I"
                }
            }
        }

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
        logger.info(timeRange.toString())

        val duration =
            when (timeRange) {
                null -> Duration.of(1, ChronoUnit.DAYS)
                TimeRange.twoHours -> Duration.of(2, ChronoUnit.HOURS)
                TimeRange.day -> Duration.of(1, ChronoUnit.DAYS)
                TimeRange.threeDays -> Duration.of(3, ChronoUnit.DAYS)
                TimeRange.week -> Duration.of(7, ChronoUnit.DAYS)
                TimeRange.month -> Duration.of(30, ChronoUnit.DAYS)
                TimeRange.all -> Duration.of(1, ChronoUnit.DAYS)
            }

        val rawData = (if (timeRange == TimeRange.all) dataRepository.findAll()
            .toList() else dataRepository.getAfterTimestamp(
            Clock.systemUTC().instant().minus(duration).toEpochMilli()
        ))
        val averagedData = rawData.mapIndexed { index: Int, it: Data ->
            if (index == 0) it else dataConverter.average(
                rawData[index - 1],
                it
            )
        }
        val data: List<Data> = averagedData.map { dataConverter.convert(it) }

        val irrigations = if (timeRange == TimeRange.all) irrigationRepository.findAll()
            .toList() else irrigationRepository.getAfterTimestamp(
            Clock.systemUTC().instant().minus(duration).toEpochMilli()
        )

        val timeUnit = when (timeRange) {
            TimeRange.twoHours -> "minute"
            TimeRange.day -> "minute"
            TimeRange.threeDays, TimeRange.week, TimeRange.month -> "day"
            TimeRange.all -> "day"
            null -> "day"
        }

        val latestReading = dataRepository.findFirstByOrderByTimestampDesc()
            ?.let { if (it.timestamp > 1747849313389) dataConverter.convert(it) else it }
        logger.info(
            dataRepository.findFirstByOrderByTimestampDesc()?.soilHumidity2.toString() + " " +
                    latestReading?.soilHumidity2.toString()
        )
        val updateTimestampText =
            if (latestReading != null) SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(latestReading.timestamp)) else "ei tiedossa"

        val latestIrrigation = irrigationRepository.findFirstByOrderByTimestampDesc()
        val latestIrrigationText =
            if (latestIrrigation != null) SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(latestIrrigation.timestamp)) else "ei tiedossa"

        return ModelAndView(
            "index",
            mapOf(
                "data" to data,
                "updateTimestamp" to updateTimestampText,
                "latestReading" to latestReading,
                "timeUnit" to timeUnit,
                "irrigations" to irrigations,
                "latestIrrigationTimestamp" to latestIrrigationText
            )
        )
    }
}