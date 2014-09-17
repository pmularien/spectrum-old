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
 * Scala Finance is based in part on:
 *        QuantLib. http://quantlib.org/
 *
 */

package org.quantintel.ql.time.calendars

object NorwayEnum extends Enumeration {

  type NorwayEnum = Value
  val NORWAY = Value(1)

  def valueOf(market: Int) = market match {
    case 1 => NORWAY
    case _ => throw new Exception("Valid units = 1")
  }

}

/**
 *
 * Norwegian calendar
 * Holidays:
 *  Saturdays
 *  Sundays
 *  Holy Thursday
 *  Good Friday
 *  Easter Monday
 *  Ascension
 *  Whit(Pentecost) Monday
 *  New Year's Day, JANUARY 1st
 *  May Day, May 1st
 *  National Independence Day, May 17st
 *  Christmas, December 25th
 *  Boxing Day, December 26th
 *
 * @author Paul Bernard
 */
object Norway {

}