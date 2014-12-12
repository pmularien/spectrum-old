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

package org.quantintel.ql.time.calendars


import org.quantintel.ql.time.{Date, Calendar}
import org.quantintel.ql.time.Weekday._

object JointCalendarRule extends Enumeration {

  type JointCalendarRule = Value
  val JOINHOLIDAYS = Value(1)
  val JOINBUSINESSDAYS = Value(2)

  def valueOf(market: Int) : JointCalendarRule = market match {
    case 1 => JOINHOLIDAYS
    case 2 => JOINBUSINESSDAYS
    case _ => throw new Exception("Valid units = 1 to 2")
  }

}


/**
 * The basic design goal of the JointCalendar is to provide
 * methods which act on a collection of calendars.
 */
class JointCalendar extends Calendar {

  import org.quantintel.ql.time.calendars.JointCalendarRule._


  private class Impl (rule : JointCalendarRule, calendars : Calendar*)
    extends org.quantintel.ql.time.Impl {

    import org.quantintel.ql.time.calendars.JointCalendarRule._

    override def name : String = {

      val sb : StringBuilder = new StringBuilder

      rule match {
        case JOINHOLIDAYS => sb.append("JoinHolidays(")
        case JOINBUSINESSDAYS => sb.append("JoinBusinessDays(")
        case _ => throw new Exception("invalid rule type")
      }

      var count = 0
      for (calendar <- calendars) {
          if (count > 0) sb.append(", ")
          sb.append(calendar.name)
          count = count + 1
      }

      sb.append(')')
      sb.toString()

    }

    def this(c1: Calendar, c2: Calendar, rule: JointCalendarRule) {
      this(rule, c1, c2)
    }

    def this(c1: Calendar, c2: Calendar)  {
      this(JOINHOLIDAYS, c1, c2)
    }

    def this (c1: Calendar, c2: Calendar, c3: Calendar, rule: JointCalendarRule)  {
      this(rule, c1, c2, c3)
    }

    def this(c1: Calendar, c2: Calendar, c3: Calendar)   {
      this(JOINHOLIDAYS, c1, c2, c3)
    }

    def this (c1: Calendar, c2: Calendar, c3: Calendar, c4: Calendar, rule: JointCalendarRule)  {
      this(rule, c1, c2, c3, c4)
    }

    def this (c1: Calendar, c2: Calendar, c3: Calendar, c4: Calendar)  {
      this(JOINHOLIDAYS, c1, c2, c3, c4)
    }



    override def isWeekend(w: Weekday) : Boolean = {
      rule match {
        case JOINHOLIDAYS => if (calendars.exists(p => p.isWeekend(w))) true else false
        case JOINBUSINESSDAYS => if (calendars.exists(p => p.isWeekend(w))) false else true
        case _ => throw new Exception("invalid rule type")
      }
    }



    override def isBusinessDay(date: Date) : Boolean = {
      rule match {
        case JOINHOLIDAYS => if (calendars.exists(p => p.isHoliday(date))) false else true
        case JOINBUSINESSDAYS => if (calendars.exists(p => p.isBusinessDay(date))) true else false
        case _ => throw new Exception("invalid rule type")
      }
    }

  }


}
