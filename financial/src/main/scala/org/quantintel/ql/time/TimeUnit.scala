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

package org.quantintel.ql.time

object TimeUnit extends Enumeration {

  type TimeUnit = Value
  val Days = Value("Days")
  val Weeks = Value("Weeks")
  val Months = Value("Months")
  val Years = Value("Years")

  def valueOf(unit: Int) = unit match {
    case 1 => TimeUnit.Days
    case 2 => TimeUnit.Weeks
    case 3 => TimeUnit.Months
    case 4 => TimeUnit.Years
    case _ => throw new Exception("Valid units = 1 to 4")
  }
}