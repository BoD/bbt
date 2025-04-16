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

package org.jraf.bbt.shared.messaging

import kotlinx.coroutines.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.json.encodeToDynamic
import org.jraf.bbt.shared.bookmarks.BookmarksDocument
import kotlin.js.Promise

class Messenger private constructor() {
  @OptIn(ExperimentalSerializationApi::class)
  private fun sendMessage(message: Message): Promise<Any?> {
    return chrome.runtime.sendMessage(Json.encodeToDynamic(message))
  }

  fun sendSettingsChangedMessage() {
    sendMessage(SettingsChangedMessage)
  }

  suspend fun sendOffscreenExtractBookmarksFromFeedMessage(body: String): BookmarksDocument? {
    val message = OffscreenExtractBookmarksFromFeedMessage(body)
    return sendMessage(message).await().unsafeCast<BookmarksDocument?>()
  }

  suspend fun sendOffscreenExtractBookmarksFromOpmlMessage(body: String): BookmarksDocument? {
    val message = OffscreenExtractBookmarksFromOpmlMessage(body)
    return sendMessage(message).await().unsafeCast<BookmarksDocument?>()
  }

  suspend fun sendOffscreenExtractBookmarksFromHtmlMessage(
    body: String,
    xPath: String?,
    documentUrl: String,
  ): BookmarksDocument? {
    val message = OffscreenExtractBookmarksFromHtmlMessage(
      body = body,
      xPath = xPath,
      documentUrl = documentUrl,
    )
    return sendMessage(message).await().unsafeCast<BookmarksDocument?>()
  }

  companion object {
    val messenger = Messenger()
  }
}

@OptIn(ExperimentalSerializationApi::class)
fun Any.asMessage(): Message {
  return Json.decodeFromDynamic(this)
}
