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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jraf.bbt.shared.EXTENSION_NAME
import org.jraf.bbt.shared.VERSION
import org.jraf.bbt.shared.logging.LogLevel
import org.jraf.bbt.shared.logging.initLogs
import org.jraf.bbt.shared.logging.log
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.logging.logi
import org.jraf.bbt.shared.messaging.LogMessage
import org.jraf.bbt.shared.messaging.asMessage
import org.jraf.bbt.shared.settings.SettingsManager.Companion.settingsManager

private const val SYNC_PERIOD_MINUTES = 30

private const val ALARM_NAME = EXTENSION_NAME

private val syncManager = SyncManager()

// This is executed once when the extension starts
fun main() {
  initLogs(logWithMessages = false, sourceName = "Core")
  logi("$EXTENSION_NAME $VERSION")
  registerMessageListener()
  registerSettingsListener()
}

private fun registerMessageListener() {
  chrome.runtime.onMessage.addListener { msg, _, _ ->
    when (val message = msg.asMessage()) {
      is LogMessage -> {
        log(
          source = message.source,
          level = LogLevel.entries.first { it.ordinal == message.level },
          format = message.format,
          params = message.params
        )
      }

      else -> {
        // Ignore
      }
    }
  }
}

private val onAlarmTriggered: () -> Unit = {
  logd("Alarm triggered")
  GlobalScope.launch {
    syncManager.syncFolders()
  }
}

fun registerSettingsListener() {
  GlobalScope.launch {
    settingsManager.settings
      .map { it.syncEnabled }
      .distinctUntilChanged()
      .collect { syncEnabled ->
        onSyncEnabledChanged(syncEnabled)
      }
  }
}

private var lastSyncEnabled: Boolean? = null

private fun onSyncEnabledChanged(syncEnabled: Boolean) {
  logd("syncEnabled changed: $syncEnabled")
  if (syncEnabled) {
    logd("Sync enabled, scheduled every $SYNC_PERIOD_MINUTES minutes")
    updateBadge(true)
    stopScheduling()
    startScheduling()

    // Keep a memory cache of the last sync enabled state, so we don't sync every single time the extension comes alive.
    // We should only sync when going from disabled to enabled.
    if (lastSyncEnabled == false) {
      GlobalScope.launch {
        syncManager.syncFolders()
      }
    }
  } else {
    logd("Sync disabled")
    updateBadge(false)
    stopScheduling()
  }

  lastSyncEnabled = syncEnabled
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
  onAlarm.addListener(onAlarmTriggered)
  chrome.alarms.create(
    ALARM_NAME,
    AlarmCreateInfo(
      periodInMinutes = SYNC_PERIOD_MINUTES,
      delayInMinutes = SYNC_PERIOD_MINUTES,
    ),
  )
}

private fun stopScheduling() {
  onAlarm.removeListener(onAlarmTriggered)
  chrome.alarms.clearAll()
}
