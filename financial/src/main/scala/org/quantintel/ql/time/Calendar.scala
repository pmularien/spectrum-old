/*
 * Copyright (c) 2014  Paul Bernard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Spectrum Finance is based in part on:
 *        QuantLib. http://quantlib.org/
 *
 */
package org.quantintel.ql.time

import org.quantintel.ql.time.BusinessDayConvention.BusinessDayConventionEnum
import org.quantintel.ql.time.BusinessDayConvention._
import org.quantintel.ql.time.TimeUnit.TimeUnit
import org.quantintel.ql.time.TimeUnit.{DAYS, WEEKS}
import org.quantintel.ql.time.Weekday._
import scala.collection.mutable
import scala.collection.mutable.HashSet

import scala.language.postfixOps


abstract class Impl {

    var addedHolidays = new mutable.HashSet[Date]()
    var removedHolidays = new mutable.HashSet[Date]()

    def name : String
    def isBusinessDay(d: Date): Boolean
    def isWeekend(w: Weekday) : Boolean

}

object Calendar {

  var UNKNOWN_MARKET = "Unknown market"
  var UNKNOWN_BUSINESS_DAY_CONVENTION = "Unknown business day convention"
}

/**
 * This class provides methods for determining whether a data is a business day
 * or a holiday for a given market, and for incrementing/decrementing a date of
 * a given number of business days.
 *
 * A calendar should be defined for a specific exchange holiday schedule or for
 * general country holiday schedule. Legacy city holiday schedule calendars will
 * be moved to the exchange/country convention
 *
 * @author Paul Bernard
 */
abstract class Calendar {

  private val addedHolidays = new mutable.HashSet[Date]
  private val removedHolidays = new mutable.HashSet[Date]

  protected var impl: Impl = null

  def empty : Boolean = impl == null

  def name : String = impl.name

  /**
   * This implementation of isBusinessDay will consult the set of
   * added and removed holidays first, before delegating to the implementation.
   * 
   * @param d the date to check
   */
  def isBusinessDay(d: Date) : Boolean = {
    if (addedHolidays.contains(d)) false
    else if (removedHolidays.contains(d)) true
    else impl.isBusinessDay(d)
  }

  def isHoliday(d: Date) : Boolean = {
    !isBusinessDay(d)
  }

  def isWeekend(w: Weekday): Boolean = impl.isWeekend(w)

  def isEndOfMonth(d: Date) : Boolean = {
    // TODO: d.month != adjust(d.add(1)).month
    true
  }
  def endOfMonth(d: Date): Date = {
    adjust(Date.endOfMonth(d), BusinessDayConvention.PRECEDING)
  }

  /**
   * Adds a date to the set of holidays for the given calendar
   * @param d date to be added
   */
  def addHoliday(d: Date) {
    impl.removedHolidays.remove(d)
    if (isBusinessDay(d)) addedHolidays.add(d)
  }

  /**
   * Remove the date from the set of holidays for a given calendar
   * @param d date to be removed
   */
  def removeHoliday(d: Date) {
    impl.addedHolidays.remove(d)
    if (isBusinessDay(d)) removedHolidays.add(d)
  }


  def holidayList(c: Calendar, from: Date, toDate: Date, includeWeekEnds : Boolean)  : Seq[Date] = {

    var result = scala.collection.mutable.ArrayBuffer[Date]()

      var d: Date = from.clone
      while(d <= toDate){
        if (c.isHoliday(d) && (includeWeekEnds || !c.isWeekend(d.weekday))) {
          result :+ d
        }

        d += 1
      }
    result
  }

  def adjust(date: Date) : Date = {
    adjust(date, BusinessDayConvention.FOLLOWING)
  }

  def adjust(d: Date, c: BusinessDayConventionEnum) : Date = {
    if (c == BusinessDayConvention.UNADJUSTED) return d.clone
    val d1: Date = d.clone
    if(c == BusinessDayConvention.FOLLOWING || c == BusinessDayConvention.MODIFIED_FOLLOWING) {
      while (isHoliday(d1)) d1.++

      if (c == BusinessDayConvention.MODIFIED_FOLLOWING) {
        if (d1.month != d.month) return adjust(d, BusinessDayConvention.PRECEDING)
      }
    } else if (c == BusinessDayConvention.PRECEDING || c == BusinessDayConvention.MODIFIED_PRECEDING) {
      while(isHoliday(d1)) { d1.-- }
      if (c == BusinessDayConvention.MODIFIED_PRECEDING && d1.month != d.month)
        return adjust(d, BusinessDayConvention.FOLLOWING)
    } else {
      throw new Exception(Calendar.UNKNOWN_BUSINESS_DAY_CONVENTION)
    }
   d1
  }

  def advance(date: Date, period: Period, convention: BusinessDayConventionEnum) : Date =
    advance(date, period, convention, endOfMonth = false)

  def advance(date: Date, period: Period, convention: BusinessDayConventionEnum,
               endOfMonth: Boolean) : Date =
    advance(date, period.length, period.units, convention, endOfMonth)

  def advance(date: Date, n: Int, unit: TimeUnit) : Date =
    advance(date, n, unit, FOLLOWING, endOfMonth = false)

  def advance (date : Date, period: Period) : Date =
    advance(date, period, FOLLOWING, endOfMonth = false)

  def advance(d: Date, n: Int, unit: TimeUnit, c: BusinessDayConventionEnum,
              endOfMonth: Boolean) : Date = {

    var ln = n

    if (ln ==0) adjust(d, c)
    else if (unit == DAYS) {
      var d1: Date = d
      if(ln>0) {
        while(n>0) {
          d1 = d1 + 1
          while (isHoliday(d1)) d1.++;
          ln = ln - 1
        }
      } else {
          while (ln <0){
            d1 = d1 -1
            while(isHoliday(d1)) d1.--;
            ln = ln + 1
          }
        }
      d1
      } else if (unit == WEEKS) {
        val d1: Date = d + ln * unit.id
        adjust(d1, c)
      } else {
        val d1: Date = d + n * unit.id
        if(endOfMonth && isEndOfMonth(d)) return this.endOfMonth(d1)
        adjust(d1, c)
      }

  }


  def businessDaysBetween(from: Date, to: Date) : Int = {
    businessDaysBetween(from, to, includeFirst = true, includeLast = false)
  }
  def businessDaysBetween(from: Date, to: Date, includeFirst: Boolean, includeLast: Boolean) : Int = {

    var wd = 0

    if (from.ne(to)) {
      if (from < to) {
        var d : Date = from.clone()
        while (d < to) {
          d = d + 1
          if (isBusinessDay(d)) {
            wd = wd + 1
          }
        }
        if (isBusinessDay(to)) {
          wd = wd + 1
        }
      } else if (from > to) {
        var d = to.clone
        while (d < from) {
          d = d + 1
          if (isBusinessDay(d)) {
            wd = wd + 1
          }
        }
        if (isBusinessDay(from)) {
          wd = wd + 1
        }
      }

      if (isBusinessDay(from) && !includeFirst) {
        wd = wd -1
      }
      if (isBusinessDay(to) && !includeLast) {
        wd = wd -1
      }

      if (from > to) {
        wd = -wd
      }
    }

    wd
  }



  //def eq (c1: Calendar, c2: Calendar): Boolean = ???
  //def ne (c1: Calendar, c2: Calendar): Boolean = ???
}


abstract class Western extends Impl {

  override def name = "Western"

  val easterMondayData = Array[Short](
    98,  90, 103,  95, 114, 106,  91, 111, 102,
    87, 107,  99,  83, 103,  95, 115,  99,  91, 111,    // 1910-1919
    96,  87, 107,  92, 112, 103,  95, 108, 100,  91,    // 1920-1929
    111,  96,  88, 107,  92, 112, 104,  88, 108, 100,   // 1930-1939
    85, 104,  96, 116, 101,  92, 112,  97,  89, 108,    // 1940-1949
    100,  85, 105,  96, 109, 101,  93, 112,  97,  89,   // 1950-1959
    109,  93, 113, 105,  90, 109, 101,  86, 106,  97,   // 1960-1969
    89, 102,  94, 113, 105,  90, 110, 101,  86, 106,    // 1970-1979
    98, 110, 102,  94, 114,  98,  90, 110,  95,  86,    // 1980-1989
    106,  91, 111, 102,  94, 107,  99,  90, 103,  95,   // 1990-1999
    115, 106,  91, 111, 103,  87, 107,  99,  84, 103,   // 2000-2009
    95, 115, 100,  91, 111,  96,  88, 107,  92, 112,    // 2010-2019
    104,  95, 108, 100,  92, 111,  96,  88, 108,  92,   // 2020-2029
    112, 104,  89, 108, 100,  85, 105,  96, 116, 101,   // 2030-2039
    93, 112,  97,  89, 109, 100,  85, 105,  97, 109,    // 2040-2049
    101,  93, 113,  97,  89, 109,  94, 113, 105,  90,   // 2050-2059
    110, 101,  86, 106,  98,  89, 102,  94, 114, 105,   // 2060-2069
    90, 110, 102,  86, 106,  98, 111, 102,  94, 114,    // 2070-2079
    99,  90, 110,  95,  87, 106,  91, 111, 103,  94,    // 2080-2089
    107,  99,  91, 103,  95, 115, 107,  91, 111, 103,   // 2090-2099
    88, 108, 100,  85, 105,  96, 109, 101,  93, 112,    // 2100-2109
    97,  89, 109,  93, 113, 105,  90, 109, 101,  86,    // 2110-2119
    106,  97,  89, 102,  94, 113, 105,  90, 110, 101,   // 2120-2129
    86, 106,  98, 110, 102,  94, 114,  98,  90, 110,    // 2130-2139
    95,  86, 106,  91, 111, 102,  94, 107,  99,  90,    // 2140-2149
    103,  95, 115, 106,  91, 111, 103,  87, 107,  99,   // 2150-2159
    84, 103,  95, 115, 100,  91, 111,  96,  88, 107,    // 2160-2169
    92, 112, 104,  95, 108, 100,  92, 111,  96,  88,    // 2170-2179
    108,  92, 112, 104,  89, 108, 100,  85, 105,  96,   // 2180-2189
    116, 101,  93, 112,  97,  89, 109, 100,  85, 105)   // 2190-2199)


  override def isWeekend(w: Weekday) : Boolean = {
    w == SATURDAY || w == SUNDAY
  }

  /**
   * The offset of the Easter Monday relative to the first day of year
   * @param y the year
   * @return offset
   */
  final def easterMonday(y : Int) : Int = {
    easterMondayData(y - 1901)
  }

}


abstract class Orthodox extends Impl {

  override def name = "Orthodox"

  val easterMondayData = Array[Short] (
    105, 118, 110, 102, 121, 106, 126, 118, 102,        // 1901-1909
    122, 114,  99, 118, 110,  95, 115, 106, 126, 111,   // 1910-1919
    103, 122, 107,  99, 119, 110, 123, 115, 107, 126,   // 1920-1929
    111, 103, 123, 107,  99, 119, 104, 123, 115, 100,   // 1930-1939
    120, 111,  96, 116, 108, 127, 112, 104, 124, 115,   // 1940-1949
    100, 120, 112,  96, 116, 108, 128, 112, 104, 124,   // 1950-1959
    109, 100, 120, 105, 125, 116, 101, 121, 113, 104,   // 1960-1969
    117, 109, 101, 120, 105, 125, 117, 101, 121, 113,   // 1970-1979
    98, 117, 109, 129, 114, 105, 125, 110, 102, 121,    // 1980-1989
    106,  98, 118, 109, 122, 114, 106, 118, 110, 102,   // 1990-1999
    122, 106, 126, 118, 103, 122, 114,  99, 119, 110,   // 2000-2009
    95, 115, 107, 126, 111, 103, 123, 107,  99, 119,    // 2010-2019
    111, 123, 115, 107, 127, 111, 103, 123, 108,  99,   // 2020-2029
    119, 104, 124, 115, 100, 120, 112,  96, 116, 108,   // 2030-2039
    128, 112, 104, 124, 116, 100, 120, 112,  97, 116,   // 2040-2049
    108, 128, 113, 104, 124, 109, 101, 120, 105, 125,   // 2050-2059
    117, 101, 121, 113, 105, 117, 109, 101, 121, 105,   // 2060-2069
    125, 110, 102, 121, 113,  98, 118, 109, 129, 114,   // 2070-2079
    106, 125, 110, 102, 122, 106,  98, 118, 110, 122,   // 2080-2089
    114,  99, 119, 110, 102, 115, 107, 126, 118, 103,   // 2090-2099
    123, 115, 100, 120, 112,  96, 116, 108, 128, 112,   // 2100-2109
    104, 124, 109, 100, 120, 105, 125, 116, 108, 121,   // 2110-2119
    113, 104, 124, 109, 101, 120, 105, 125, 117, 101,   // 2120-2129
    121, 113,  98, 117, 109, 129, 114, 105, 125, 110,   // 2130-2139
    102, 121, 113,  98, 118, 109, 129, 114, 106, 125,   // 2140-2149
    110, 102, 122, 106, 126, 118, 103, 122, 114,  99,   // 2150-2159
    119, 110, 102, 115, 107, 126, 111, 103, 123, 114,   // 2160-2169
    99, 119, 111, 130, 115, 107, 127, 111, 103, 123,    // 2170-2179
    108,  99, 119, 104, 124, 115, 100, 120, 112, 103,   // 2180-2189
    116, 108, 128, 119, 104, 124, 116, 100, 120, 112    // 2190-2199
  )

  override def isWeekend(w: Weekday) : Boolean = {
    w == SATURDAY || w == SUNDAY
  }


  /**
   * The offset of the Easter Monday relative to the first day of year
   * @param y the year
   * @return offset
   */
  final def easterMonday(y : Int) : Int = {
    easterMondayData(y - 1901)
  }

}

