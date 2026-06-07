package com.pelotcl.app.generic.utils.date

import android.content.Context
import com.pelotcl.app.generic.data.repository.itinerary.holiday.HolidaysData
import kotlinx.datetime.LocalDate as KxLocalDate
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Generic school holiday detector.
 * Loads holiday periods from a JSON file.
 */
class HolidayDetector(
    private val context: Context,
    private val holidayFileName: String,
    private val publicHolidayStrategy: PublicHolidayStrategy? = null
) {
    private val schoolHolidays: List<HolidayPeriod>

    init {
        schoolHolidays = loadSchoolHolidays()
    }

    private fun loadSchoolHolidays(): List<HolidayPeriod> {
        return try {
            val json = context.assets.open(holidayFileName).bufferedReader().use { it.readText() }
            val jsonConfig = Json { ignoreUnknownKeys = true }
            val holidaysData = jsonConfig.decodeFromString<HolidaysData>(json)
            holidaysData.holidays.mapNotNull { holiday ->
                val startDate = try {
                    LocalDate.parse(holiday.startDateInclusive, DateTimeFormatter.ISO_DATE)
                } catch (e: Exception) {
                    null
                }
                val endDate = try {
                    holiday.endDateInclusive?.let {
                        LocalDate.parse(it, DateTimeFormatter.ISO_DATE)
                    }
                } catch (e: Exception) {
                    null
                }

                if (startDate != null) {
                    HolidayPeriod(
                        name = holiday.name,
                        startDate = startDate,
                        endDate = endDate ?: startDate.plusMonths(2)
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if a given date is a school holiday
     */
    fun isSchoolHoliday(date: LocalDate): Boolean {
        return schoolHolidays.any { period ->
            !date.isBefore(period.startDate) && !date.isAfter(period.endDate)
        }
    }

    /**
     * Check if a given date is a public holiday
     */
    fun isPublicHoliday(date: LocalDate): Boolean {
        if (publicHolidayStrategy == null) return false
        val kxDate = KxLocalDate(date.year, date.monthValue, date.dayOfMonth)
        return publicHolidayStrategy.isPublicHoliday(kxDate)
    }

    data class HolidayPeriod(
        val name: String,
        val startDate: LocalDate,
        val endDate: LocalDate
    )
}
