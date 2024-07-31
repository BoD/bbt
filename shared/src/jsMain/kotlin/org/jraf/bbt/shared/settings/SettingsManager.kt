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

@file:OptIn(DelicateCoroutinesApi::class)

package org.jraf.bbt.shared.settings

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.json.encodeToDynamic
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.messaging.Message
import org.jraf.bbt.shared.messaging.MessageType
import org.jraf.bbt.shared.messaging.Messenger
import org.jraf.bbt.shared.settings.model.Settings
import org.jraf.bbt.shared.settings.model.SyncItem
import org.jraf.bbt.shared.settings.model.SyncState

class SettingsManager private constructor() {
  private val _settings = MutableStateFlow<Settings?>(null)
  val settings: Flow<Settings> = _settings.filterNotNull()

  private val messenger = Messenger.messenger

  init {
    GlobalScope.launch {
      _settings.value = loadSettingsFromStorage()
    }
    registerMessageListener()
  }

  private fun registerMessageListener() {
    chrome.runtime.onMessage.addListener { msg, _, _ ->
      val message = msg.unsafeCast<Message>()
      when (message.type) {
        MessageType.SETTINGS_CHANGED.ordinal -> {
          GlobalScope.launch {
            logd("SettingsManager: Received SETTINGS_CHANGED message")
            _settings.value = loadSettingsFromStorage().also { logd("SettingsManager: Settings from storage: %o", it) }
          }
        }
      }
    }
  }

  private suspend fun loadSettingsFromStorage(): Settings {
    val items = chrome.storage.sync.get("settings").await()
    val obj = items.settings
    return if (obj == undefined) {
      Settings(
        syncEnabled = true,
        syncItems = listOf(
          SyncItem(
            folderName = "Sample",
            remoteBookmarksUrl = "https://en.wikipedia.org/wiki/List_of_James_Bond_films#__xpath=//table//th/i//a"
          )
        ),
        syncState = SyncState.initialState(),
      )
    } else {
      toSettings(obj)
    }
  }

  suspend fun saveSettingsToStorage(settings: Settings) {
    val obj = js("{}")
    obj.settings = settings.toDynamic()
    chrome.storage.sync.set(obj).await()
    // For this frame
    _settings.value = settings
    // For other frames ("the runtime.onMessage event will be fired in every frame of your extension (except for the sender’s frame)")
    messenger.sendSettingsChangedMessage()
  }

  companion object {
    val settingsManager = SettingsManager()
  }
}

@OptIn(ExperimentalSerializationApi::class)
private fun Settings.toDynamic(): dynamic = Json.encodeToDynamic(this)

@OptIn(ExperimentalSerializationApi::class)
private fun toSettings(o: dynamic): Settings = Json.decodeFromDynamic<Settings>(o)
