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

@file:OptIn(ExperimentalComposeUiApi::class, DelicateCoroutinesApi::class)

package org.jraf.bbt.popup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.skiko.wasm.onWasmReady
import org.jraf.bbt.popup.theme.BbtTheme
import org.jraf.bbt.shared.bookmarks.BookmarkManager.Companion.bookmarkManager
import org.jraf.bbt.shared.logging.initLogs
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.settings.SettingsManager.Companion.settingsManager
import org.jraf.bbt.shared.settings.model.SyncItem
import org.jraf.bbt.shared.util.equalsIgnoreCase
import org.jraf.bbt.shared.util.isValidUrl

// This is executed every time popup.html is opened.
fun main() {
  initLogs(logWithMessages = true, sourceName = "Popup")
  logd("Popup open")

  onWasmReady {
    @OptIn(ExperimentalComposeUiApi::class)
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
      val settings by settingsManager.settings.collectAsState(null)
      BbtTheme {
        Surface(Modifier.fillMaxSize()) {
          if (settings != null) {
            Popup(
              settings = settings!!,
              onSyncEnabledCheckedChange = ::onSyncEnabledCheckedChange,
              onAddItem = ::onAddItem,
              onRemoveItem = ::onRemoveItem,
            )
          }
        }
      }
    }
  }
}

private fun onSyncEnabledCheckedChange(checked: Boolean) {
  GlobalScope.launch {
    settingsManager.saveSettingsToStorage(settingsManager.settings.first().copy(syncEnabled = checked))
  }
}

class AddItemResult(
  val folderNameErrorText: String?,
  val remoteBookmarksUrlErrorText: String?,
)

private suspend fun onAddItem(folderName: String, remoteBookmarksUrl: String): AddItemResult {
  val settings = settingsManager.settings.first()
  val folderNameErrorText = if (!bookmarkManager.isExistingFolder(folderName)) {
    "Folder doesn't exist"
  } else if (settings.syncItems.any { it.folderName.equalsIgnoreCase(folderName) }) {
    "Folder is already handled"
  } else {
    null
  }
  val remoteBookmarksUrlErrorText = if (!isValidUrl(remoteBookmarksUrl)) {
    "Invalid URL"
  } else {
    null
  }
  if (folderNameErrorText == null && remoteBookmarksUrlErrorText == null) {
    val syncItem = SyncItem(folderName = folderName, remoteBookmarksUrl = remoteBookmarksUrl)
    settingsManager.saveSettingsToStorage(settings.copy(syncItems = settings.syncItems + syncItem))
  }
  return AddItemResult(folderNameErrorText, remoteBookmarksUrlErrorText)
}

private fun onRemoveItem(syncItem: SyncItem) {
  GlobalScope.launch {
    val settings = settingsManager.settings.first()
    settingsManager.saveSettingsToStorage(settings.copy(syncItems = settings.syncItems - syncItem))
  }
}

