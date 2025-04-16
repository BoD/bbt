/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2020-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.bbt.shared.logging

import kotlin.js.Date

enum class LogLevel {
  DEBUG,
  INFO,
  WARN,
}

fun log(
  level: LogLevel,
  format: String,
  vararg params: Any?,
) {
  val date = Date()
  when (level) {
    LogLevel.DEBUG -> console.log("${date.toLocaleDateString()} ${date.toLocaleTimeString()} - $format", *params)
    LogLevel.INFO -> console.info("${date.toLocaleDateString()} ${date.toLocaleTimeString()} - $format", *params)
    LogLevel.WARN -> console.warn("${date.toLocaleDateString()} ${date.toLocaleTimeString()} - $format", *params)
  }
}

fun logd(format: String, vararg params: Any?) {
  log(
    level = LogLevel.DEBUG,
    format = format,
    params = params,
  )
}

fun logi(format: String, vararg params: Any?) {
  log(
    level = LogLevel.INFO,
    format = format,
    params = params,
  )
}

fun logw(format: String, vararg params: Any?) {
  log(
    level = LogLevel.WARN,
    format = format,
    params = params,
  )
}
