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

package org.jraf.bbt.util

open class Publisher<T> {
  private val observers = mutableSetOf<(T) -> Unit>()

  open fun addObserver(onChanged: (T) -> Unit) {
    observers += onChanged
  }

  fun removeObserver(onChanged: (T) -> Unit) {
    observers -= onChanged
  }

  open fun publish(t: T) {
    dispatch(t)
  }

  private fun dispatch(t: T) {
    for (observer in observers) {
      observer(t)
    }
  }
}

class CachedPublisher<T>(initialValue: T? = null) : Publisher<T>() {
  var value: T? = initialValue

  override fun publish(t: T) {
    value = t
    super.publish(t)
  }

  override fun addObserver(onChanged: (T) -> Unit) {
    super.addObserver(onChanged)
    value?.let { onChanged(it) }
  }
}
