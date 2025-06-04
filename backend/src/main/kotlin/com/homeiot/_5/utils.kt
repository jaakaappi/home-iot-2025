package com.homeiot._5

import java.time.Duration

fun prettyPrintDuration(duration: Duration): String {
    return "%sd %sh %sm %ss".format(
        duration.toDaysPart(),
        duration.toHoursPart(),
        duration.toMinutesPart(),
        duration.toSecondsPart()
    )
}