package com.pelotcl.app.generic.utils.date

import kotlinx.datetime.LocalDate as KxLocalDate

fun java.time.LocalDate.toKx(): KxLocalDate =
    KxLocalDate(this.year, this.monthValue, this.dayOfMonth)
