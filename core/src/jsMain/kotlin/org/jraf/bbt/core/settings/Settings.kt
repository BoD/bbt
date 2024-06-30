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

package org.jraf.bbt.core.settings

import kotlinx.coroutines.await

data class Settings(
  val syncEnabled: Boolean,
  val syncItems: List<SyncItem>,
)

data class SyncItem(
  val folderName: String,
  val remoteBookmarksUrl: String,
)

/**
 * Version of [Settings] that is safe to store to / load from Chrome storage.
 */
@JsExport
class JsonSettings(
  val syncEnabled: Boolean,
  val syncItems: Array<JsonSyncItem>,
)

/**
 * Version of [SyncItem] that is safe to store to / load from Chrome storage.
 */
@JsExport
class JsonSyncItem(
  val folderName: String,
  val remoteBookmarksUrl: String,
)


private fun Settings.toJsonSettings() = JsonSettings(
  syncEnabled = syncEnabled,
  syncItems = syncItems.map {
    JsonSyncItem(
      folderName = it.folderName,
      remoteBookmarksUrl = it.remoteBookmarksUrl
    )
  }.toTypedArray()
)

private fun JsonSettings.toSettings() = Settings(
  syncEnabled = syncEnabled,
  syncItems = syncItems.map {
    SyncItem(
      folderName = it.folderName,
      remoteBookmarksUrl = it.remoteBookmarksUrl
    )
  }
)

suspend fun loadSettingsFromStorage(): Settings {
  val items = chrome.storage.sync.get("settings").await()
  val obj = items.settings
  return if (obj == undefined) {
    Settings(
      syncEnabled = true,
      syncItems = listOf(
        SyncItem(
          folderName = "Sample",
          remoteBookmarksUrl = "https://en.wikipedia.org/wiki/Wikipedia:Featured_articles#__element=//h3[@data-mw-thread-id='h-Elements-Chemistry_and_mineralogy']/following-sibling::*[1]"
        )
      )
    )
  } else {
    val jsonSettings = obj.unsafeCast<JsonSettings>()
    jsonSettings.toSettings()
  }
}

suspend fun saveSettingsToStorage(settings: Settings) {
  val obj = js("{}")
  obj.settings = settings.toJsonSettings()
  chrome.storage.sync.set(obj).await()
}
