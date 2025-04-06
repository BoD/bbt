/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2025-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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

package org.w3c.dom.xpath

import org.w3c.dom.Node

external class XPathResult {
  companion object {
    val ANY_TYPE: Int
    val NUMBER_TYPE: Int
    val ANY_UNORDERED_NODE_TYPE: Int
    val BOOLEAN_TYPE: Int
    val FIRST_ORDERED_NODE_TYPE: Int
    val ORDERED_NODE_ITERATOR_TYPE: Int
    val ORDERED_NODE_SNAPSHOT_TYPE: Int
    val STRING_TYPE: Int
    val UNORDERED_NODE_ITERATOR_TYPE: Int
    val UNORDERED_NODE_SNAPSHOT_TYPE: Int
  }

  val booleanValue: Boolean?
  val invalidIteratorState: Boolean
  val numberValue: Double?
  val resultType: Int
  val singleNodeValue: dynamic
  val snapshotLength: Int
  val stringValue: String?

  fun iterateNext(): Node?
  fun snapshotItem(index: Int): Node?
}
