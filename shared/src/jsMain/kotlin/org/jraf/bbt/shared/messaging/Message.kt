/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2024-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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

@file:OptIn(ExperimentalJsExport::class)

package org.jraf.bbt.shared.messaging

// Need @JsExport because these will be jsonified / dejsonified
@JsExport
class Message(
  val type: Int,
  val payload: Any?,
)

enum class MessageType {
  LOG,
  SETTINGS_CHANGED,
  OFFSCREEN_EXTRACT_BOOKMARKS_FROM_FEED,
  OFFSCREEN_EXTRACT_BOOKMARKS_FROM_HTML,
}


@JsExport
class LogPayload(
  val source: String,
  val level: Int,
  val format: String,
  val params: Array<out Any?>,
)

@JsExport
class OffscreenExtractBookmarksFromFeedPayload(
  val body: String,
)

@JsExport
class OffscreenExtractBookmarksFromHtmlPayload(
  val body: String,
  val xPath: String?,
  val documentUrl: String,
)
