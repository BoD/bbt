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
import org.jraf.bbt.shared.bookmarks.BookmarksDocument
import org.jraf.bbt.shared.logging.LogLevel
import org.jraf.bbt.shared.syncstate.SyncState
import kotlin.js.Promise

class Messenger private constructor() {
  private fun sendMessage(message: Message): Promise<Any?> = chrome.runtime.sendMessage(message)

  fun sendLogMessage(source: String, level: LogLevel, format: String, params: Array<out Any?>) {
    val logPayload = LogPayload(
      source = source,
      level = level.ordinal,
      format = format,
      params = params,
    )
    val message = Message(type = MessageType.LOG.ordinal, payload = logPayload)
    chrome.runtime.sendMessage(message)
  }

  fun sendSettingsChangedMessage() {
    val message = Message(type = MessageType.SETTINGS_CHANGED.ordinal, payload = null)
    chrome.runtime.sendMessage(message)
  }

  suspend fun sendOffscreenExtractBookmarksFromFeedMessage(body: String): BookmarksDocument? {
    val message =
      Message(type = MessageType.OFFSCREEN_EXTRACT_BOOKMARKS_FROM_FEED.ordinal, payload = OffscreenExtractBookmarksFromFeedPayload(body))
    return chrome.runtime.sendMessage(message).await().unsafeCast<BookmarksDocument?>()
  }

  suspend fun sendOffscreenExtractBookmarksFromHtmlMessage(
    body: String,
    elementXPath: String?,
    documentUrl: String,
  ): BookmarksDocument? {
    val message = Message(
      type = MessageType.OFFSCREEN_EXTRACT_BOOKMARKS_FROM_HTML.ordinal,
      payload = OffscreenExtractBookmarksFromHtmlPayload(
        body = body,
        elementXPath = elementXPath,
        documentUrl = documentUrl,
      )
    )
    return chrome.runtime.sendMessage(message).await().unsafeCast<BookmarksDocument?>()
  }

  fun sendSyncStateChangedMessage(syncState: SyncState) {
    val message = Message(type = MessageType.SYNC_STATE_CHANGED.ordinal, payload = SyncStateChangedPayload(syncState.toJsonSyncState()))
    chrome.runtime.sendMessage(message)
  }

  fun sendGetSyncStateMessage() {
    val message = Message(type = MessageType.GET_SYNC_STATE.ordinal, payload = null)
    chrome.runtime.sendMessage(message)
  }

  companion object {
    val messenger = Messenger()
  }
}
