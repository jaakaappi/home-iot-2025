package com.homeiot._5

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
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
            "Wrong number of data fields in parsed data: ${values.count()}"
        )

        dataRepository.save(
            Data(
                timestamp = Timestamp.from(Instant.now()),
                airHumidity = 0.0f,
                airTemperature = 0.0f,
                brightness = 0.0f,
                soilHumidity1 = 0.0f,
                soilHumidity2 = 0.0f,
                soilHumidity3 = 0.0f
            )
        )

        return ""
    }
}