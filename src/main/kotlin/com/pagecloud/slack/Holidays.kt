package com.pagecloud.slack

import java.time.LocalDate
import java.time.Month

/**
 * New Year's Day	    Monday, January 1, 2018
 * Good Friday	        Friday, March 30, 2018
 * Easter Monday	    Monday, April 2, 2018
 * Victoria Day	        Monday, May 21, 2018
 * Canada Day	        Monday, July 2, 2018
 * Civic Holiday	    Monday, August 6, 2018
 * Labour Day	        Monday, September 3, 2018
 * Thanksgiving Day	    Monday October 8, 2018
 * Remembrance Day	    Monday, November 12, 2018
 * Christmas Day	    Tuesday, December 25, 2018
 * Boxing Day	        Wednesday, December 26, 2018
 */
val HOLIDAYS_2018 = setOf(
    LocalDate.of(2018, Month.JANUARY, 1),
    LocalDate.of(2018, Month.MARCH, 30),
    LocalDate.of(2018, Month.APRIL, 2),
    LocalDate.of(2018, Month.MAY, 21),
    LocalDate.of(2018, Month.JULY, 2),
    LocalDate.of(2018, Month.AUGUST, 6),
    LocalDate.of(2018, Month.SEPTEMBER, 3),
    LocalDate.of(2018, Month.OCTOBER, 8),
    LocalDate.of(2018, Month.NOVEMBER, 12),
    LocalDate.of(2018, Month.DECEMBER, 25),
    LocalDate.of(2018, Month.DECEMBER, 26)
)

fun isHoliday(date: LocalDate) = HOLIDAYS_2018.contains(date)