package com.techsultan.zenithpro.features.analytics.data

import java.time.DayOfWeek
import java.time.LocalDate

enum class ReportPeriod {
    TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR, CUSTOM;

    fun dateRange(customFrom: LocalDate? = null, customTo: LocalDate? = null): Pair<String, String> {
        val today = LocalDate.now()
        return when (this) {
            TODAY      -> today.toString() to today.toString()
            THIS_WEEK  -> today.with(DayOfWeek.MONDAY).toString() to today.toString()
            THIS_MONTH -> today.withDayOfMonth(1).toString() to today.toString()
            LAST_MONTH -> today.minusMonths(1).withDayOfMonth(1).toString() to
                    today.minusMonths(1).let { it.withDayOfMonth(it.lengthOfMonth()) }.toString()
            THIS_YEAR  -> today.withDayOfYear(1).toString() to today.toString()
            CUSTOM     -> (customFrom ?: today).toString() to (customTo ?: today).toString()
        }
    }

    fun label(): String = when (this) {
        TODAY      -> "Today"
        THIS_WEEK  -> "This week"
        THIS_MONTH -> "This month"
        LAST_MONTH -> "Last month"
        THIS_YEAR  -> "This year"
        CUSTOM     -> "Custom"
    }
}