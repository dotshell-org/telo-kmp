package com.pelotcl.app.generic.utils.date

import java.time.LocalDate

/**
 * Strategy for calculating public holidays in a specific region
 */
interface PublicHolidayStrategy {
    /**
     * Check if a given date is a public holiday
     */
    fun isPublicHoliday(date: LocalDate): Boolean
}
