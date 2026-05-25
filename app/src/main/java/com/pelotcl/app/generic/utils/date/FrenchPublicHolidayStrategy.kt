package com.pelotcl.app.generic.utils.date

import java.time.LocalDate
import java.time.Month

/**
 * Implementation of PublicHolidayStrategy for France
 */
class FrenchPublicHolidayStrategy : PublicHolidayStrategy {

    override fun isPublicHoliday(date: LocalDate): Boolean {
        val year = date.year

        // Fixed holidays
        val fixedHolidays = listOf(
            LocalDate.of(year, Month.JANUARY, 1),        // New Year's Day
            LocalDate.of(year, Month.MAY, 1),            // Labour Day
            LocalDate.of(year, Month.MAY, 8),            // Victory in Europe Day
            LocalDate.of(year, Month.JULY, 14),          // Bastille Day
            LocalDate.of(year, Month.AUGUST, 15),        // Assumption of Mary
            LocalDate.of(year, Month.NOVEMBER, 1),       // All Saints' Day
            LocalDate.of(year, Month.NOVEMBER, 11),      // Armistice Day
            LocalDate.of(year, Month.DECEMBER, 25)       // Christmas Day
        )

        if (fixedHolidays.contains(date)) {
            return true
        }

        // Moveable holidays (based on Easter)
        val easterDate = calculateEasterDate(year)
        val easterMonday = easterDate.plusDays(1)
        val ascensionDay = easterDate.plusDays(39)
        val whitMonday = easterDate.plusDays(50)

        return date == easterMonday || date == ascensionDay || date == whitMonday
    }

    /**
     * Calculate Easter date for a given year using the Computus algorithm
     */
    private fun calculateEasterDate(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1

        return LocalDate.of(year, month, day)
    }
}
