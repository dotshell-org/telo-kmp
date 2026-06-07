package com.pelotcl.app.generic.utils.date

import android.content.Context
import com.pelotcl.app.generic.data.repository.itinerary.holiday.HolidaysData
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json

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
                    LocalDate.parse(holiday.startDateInclusive)
                } catch (e: Exception) {
                    null
                }
                val endDate = try {
                    holiday.endDateInclusive?.let {
                        LocalDate.parse(it)
                    }
                } catch (e: Exception) {
                    null
                }

                if (startDate != null) {
                    HolidayPeriod(
                        name = holiday.name,
                        startDate = startDate,
                        endDate = endDate ?: startDate.plus(2, DateTimeUnit.MONTH)
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
            date >= period.startDate && date <= period.endDate
        }
    }

    /**
     * Check if a given date is a public holiday
     */
    fun isPublicHoliday(date: LocalDate): Boolean {
        if (publicHolidayStrategy == null) return false
        return publicHolidayStrategy.isPublicHoliday(date)
    }

    data class HolidayPeriod(
        val name: String,
        val startDate: LocalDate,
        val endDate: LocalDate
    )
}
