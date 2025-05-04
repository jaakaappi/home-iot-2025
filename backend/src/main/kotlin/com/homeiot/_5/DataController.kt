package com.homeiot._5

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import java.time.Instant


@RestController
class DataController {

    @Autowired
    lateinit var dataRepository: DataRepository

    @Autowired
    lateinit var irrigationRepository: IrrigationRepository

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
        return ""
    }

    @GetMapping("/")
    fun getPage(): ModelAndView {
        val data: List<Any> = dataRepository.findAll().toList()

        print(data)

        return ModelAndView("index", mapOf("data" to data))
    }
}