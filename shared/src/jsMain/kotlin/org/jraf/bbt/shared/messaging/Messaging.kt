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

@file:OptIn(ExperimentalJsExport::class)

package org.jraf.bbt.shared.messaging

import kotlinx.coroutines.await
import org.jraf.bbt.shared.logging.LogLevel
import org.jraf.bbt.shared.remote.model.BookmarksDocument
import org.jraf.bbt.shared.syncstate.FolderSyncState
import org.jraf.bbt.shared.syncstate.SyncState
import kotlin.js.Date
import kotlin.js.Promise

// Need @JsExport because these will be jsonified / dejsonified
@JsExport
class Message(
  val type: Int,
  val payload: Any?,
)

enum class MessageType {
  LOG,
  SETTINGS_CHANGED,
  OFFSCREEN_PARSE_FEED,
  OFFSCREEN_PARSE_HTML,
  SYNC_STATE_CHANGED,
}

fun sendMessage(message: Message): Promise<Any?> = chrome.runtime.sendMessage(message)

@JsExport
class LogPayload(
  val source: String,
  val level: Int,
  val format: String,
  val params: Array<out Any?>,
)

fun sendLogMessage(source: String, level: LogLevel, format: String, params: Array<out Any?>) {
  val logPayload = LogPayload(
    source = source,
    level = level.ordinal,
    format = format,
    params = params,
  )
  val message = Message(type = MessageType.LOG.ordinal, payload = logPayload)
  sendMessage(message)
}

fun sendSettingsChangedMessage() {
  val message = Message(type = MessageType.SETTINGS_CHANGED.ordinal, payload = null)
  sendMessage(message)
}

@JsExport
class OffscreenParseFeedPayload(
  val body: String,
)

suspend fun sendOffscreenParseFeedMessage(body: String): BookmarksDocument? {
  val message = Message(type = MessageType.OFFSCREEN_PARSE_FEED.ordinal, payload = OffscreenParseFeedPayload(body))
  return sendMessage(message).await().unsafeCast<BookmarksDocument?>()
}

@JsExport
class OffscreenParseHtmlPayload(
  val body: String,
  val elementXPath: String?,
  val documentUrl: String,
)

suspend fun sendOffscreenParseHtmlMessage(
  body: String,
  elementXPath: String?,
  documentUrl: String,
): BookmarksDocument? {
  val message = Message(
    type = MessageType.OFFSCREEN_PARSE_HTML.ordinal,
    payload = OffscreenParseHtmlPayload(
      body = body,
      elementXPath = elementXPath,
      documentUrl = documentUrl,
    )
  )
  return sendMessage(message).await().unsafeCast<BookmarksDocument?>()
}

@JsExport
class SyncStateChangedPayload(
  val syncState: JsonSyncState,
)

@JsExport
class JsonSyncState(
  val lastSync: String?,
  val folderSyncStates: Array<JsonFolderSyncState>,
)

@JsExport
class JsonFolderSyncState(
  val folderName: String,
  val state: Int,
  val errorMessage: String?,
)

fun sendSyncStateChangedMessage(syncState: SyncState) {
  val message = Message(type = MessageType.SYNC_STATE_CHANGED.ordinal, payload = SyncStateChangedPayload(syncState.toJsonSyncState()))
  sendMessage(message)
}

private fun SyncState.toJsonSyncState(): JsonSyncState {
  return JsonSyncState(
    lastSync = lastSync?.toISOString(),
    folderSyncStates = folderSyncStates.mapValues { (folderName, folderSyncState) ->
      when (folderSyncState) {
        FolderSyncState.Syncing -> JsonFolderSyncState(folderName, 0, null)
        is FolderSyncState.Error -> JsonFolderSyncState(folderName, 1, folderSyncState.message)
        FolderSyncState.Success -> JsonFolderSyncState(folderName, 2, null)
      }
    }.values.toTypedArray()
  )
}

fun JsonSyncState.toSyncState(): SyncState {
  return SyncState(
    lastSync = lastSync?.let { Date(it) },
    folderSyncStates = folderSyncStates.map { jsonFolderSyncState ->
      val folderSyncState = when (jsonFolderSyncState.state) {
        0 -> FolderSyncState.Syncing
        1 -> FolderSyncState.Error(jsonFolderSyncState.errorMessage!!)
        2 -> FolderSyncState.Success
        else -> throw RuntimeException("Unknown state: ${jsonFolderSyncState.state}")
      }
      jsonFolderSyncState.folderName to folderSyncState
    }.toMap()
  )
}
