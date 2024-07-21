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

package org.jraf.bbt.serviceworker.main

import chrome.action.BadgeBackgroundColor
import chrome.action.BadgeText
import chrome.action.setBadgeBackgroundColor
import chrome.action.setBadgeText
import chrome.alarms.AlarmCreateInfo
import chrome.alarms.onAlarm
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jraf.bbt.shared.EXTENSION_NAME
import org.jraf.bbt.shared.VERSION
import org.jraf.bbt.shared.logging.LogLevel
import org.jraf.bbt.shared.logging.initLogs
import org.jraf.bbt.shared.logging.log
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.logging.logi
import org.jraf.bbt.shared.messaging.LogPayload
import org.jraf.bbt.shared.messaging.Message
import org.jraf.bbt.shared.messaging.MessageType
import org.jraf.bbt.shared.settings.SettingsManager.Companion.settingsManager
import org.jraf.bbt.shared.settings.model.Settings

private const val SYNC_PERIOD_MINUTES = 30

private const val ALARM_NAME = EXTENSION_NAME

private val syncManager = SyncManager()

// This is executed once when the extension starts
fun main() {
  initLogs(logWithMessages = false, sourceName = "Core")
  logi("$EXTENSION_NAME $VERSION")
  registerMessageListener()
  registerAlarmListener()
  registerSettingsListener()
}

private fun registerMessageListener() {
  chrome.runtime.onMessage.addListener { msg, sender, sendResponse ->
    val message = msg.unsafeCast<Message>()
    when (message.type) {
      MessageType.LOG.ordinal -> {
        val logPayload = message.payload.unsafeCast<LogPayload>()
        log(
          source = logPayload.source,
          level = LogLevel.entries.first { it.ordinal == logPayload.level },
          format = logPayload.format,
          params = logPayload.params
        )
      }

      MessageType.GET_SYNC_STATE.ordinal -> {
        syncManager.sendSyncState()
      }
    }
  }
}

private fun registerAlarmListener() {
  onAlarm.addListener {
    logd("Alarm triggered")
    GlobalScope.launch {
      syncManager.syncFolders()
    }
  }
}

fun registerSettingsListener() {
  GlobalScope.launch {
    settingsManager.settings.collect { settings ->
      onSettingsChanged(settings)
    }
  }
}

private suspend fun onSettingsChanged(settings: Settings) {
  logd("Settings changed: $settings")
  if (settings.syncEnabled) {
    logd("Sync enabled, scheduled every $SYNC_PERIOD_MINUTES minutes")
    updateBadge(true)
    startScheduling()
    // Launch the sync in another coroutine to not make this fun blocking too long
    GlobalScope.launch {
      syncManager.syncFolders()
    }
  } else {
    logd("Sync disabled")
    updateBadge(false)
    stopScheduling()
  }
}

private fun updateBadge(enabled: Boolean) {
  if (enabled) {
    setBadgeText(BadgeText(""))
  } else {
    setBadgeText(BadgeText("OFF"))
    setBadgeBackgroundColor(BadgeBackgroundColor("#808080"))
  }
}

private fun startScheduling() {
  chrome.alarms.create(ALARM_NAME, AlarmCreateInfo(periodInMinutes = SYNC_PERIOD_MINUTES))
}

private fun stopScheduling() {
  chrome.alarms.clearAll()
}

