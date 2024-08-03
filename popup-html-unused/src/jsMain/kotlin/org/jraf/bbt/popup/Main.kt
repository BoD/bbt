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

package org.jraf.bbt.popup

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jraf.bbt.shared.EXTENSION_NAME
import org.jraf.bbt.shared.VERSION
import org.jraf.bbt.shared.bookmarks.BookmarkManager.Companion.bookmarkManager
import org.jraf.bbt.shared.logging.initLogs
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.messaging.Message
import org.jraf.bbt.shared.messaging.MessageType
import org.jraf.bbt.shared.messaging.Messenger.Companion.messenger
import org.jraf.bbt.shared.messaging.SyncStateChangedPayload
import org.jraf.bbt.shared.messaging.toSyncState
import org.jraf.bbt.shared.settings.SettingsManager.Companion.settingsManager
import org.jraf.bbt.shared.settings.model.SyncItem
import org.jraf.bbt.shared.syncstate.FolderSyncState
import org.jraf.bbt.shared.syncstate.SyncState
import org.jraf.bbt.shared.util.equalsIgnoreCase
import org.jraf.bbt.shared.util.isValidUrl
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

// This is executed every time popup.html is opened.
fun main() {
  initLogs(logWithMessages = true, sourceName = "Popup")
  logd("Popup open")
  document.getElementById("nameAndVersion")!!.innerHTML = "$EXTENSION_NAME $VERSION"
  observeSyncState()
  messenger.sendGetSyncStateMessage()
  GlobalScope.launch {
    populateTable()
  }
}

private fun observeSyncState() {
  val callback: (message: Any, sender: Any, sendResponse: (Any?) -> Unit) -> Unit = { msg, _, _ ->
    val message = msg.unsafeCast<Message>()
    when (message.type) {
      MessageType.SYNC_STATE_CHANGED.ordinal -> {
        val syncStateChangedPayload = message.payload.unsafeCast<SyncStateChangedPayload>()
        onSyncStateChanged(syncStateChangedPayload.syncState.toSyncState())
      }
    }
  }
  chrome.runtime.onMessage.addListener(callback)
  window.onblur = { chrome.runtime.onMessage.removeListener(callback) }
}

private suspend fun populateTable() {
  val settings = settingsManager.loadSettingsFromStorage()

  // Enabled checkbox
  val syncEnabledCheckboxCheckedHtml = if (settings.syncEnabled) "checked" else ""
  val syncEnabledLabelHtml = if (settings.syncEnabled) {
    """<div id="txtEnabledDisabled" class="enabledDisabled"">Enabled</div>"""
  } else {
    """<div id="txtEnabledDisabled" class="enabledDisabled disabled"">Disabled</div>"""
  }
  var tableHtml = """
        <tr>
            <td colspan="4">
                <div class="onoffswitch"><input $syncEnabledCheckboxCheckedHtml type="checkbox" id="chkSyncEnabled" name="chkSyncEnabled" class="onoffswitch-checkbox"><label class="onoffswitch-label" for="chkSyncEnabled"><span class="onoffswitch-inner"></span><span class="onoffswitch-switch"></span></label></div>
                $syncEnabledLabelHtml
            </td>
        </tr>
    """.trimIndent()

  // Sync items
  for (syncItem in settings.syncItems) {
    tableHtml += """
            <tr>
                <td><input class="input folderName" type="text" value="${syncItem.folderName}" readonly="true"></td>
                <td class="url"><input class="input url" type="text" value="${syncItem.remoteBookmarksUrl}" readonly="true"></td>
                <td><img id="imgSyncState_${syncItem.folderName}" src="icons/empty.png" width="20" height="20"></td>
                <td><button type="button" id="btnRemove_${syncItem.folderName}" value="${syncItem.folderName}">Remove</button>
            </tr>
        """.trimIndent()
  }

  // Add item section, validation message, and last sync info
  tableHtml += """
            <tr>
                <td><input class="input folderName" type="text" placeholder="Folder name" id="inputFolderName"></td>
                <td class="url"><input class="input url" type="text" placeholder="Remote bookmarks URL (RSS, Atom, JSON)" id="inputUrl"></td>
                <td><img src="icons/empty.png" width="20" height="20"></td>
                <td><button type="button" id="btnAdd" disabled>Add</button>
            </tr>
            <tr>
                <td id="tdFolderNameError" class="validationError">&nbsp;</td>
                <td id="tdUrlError" class="validationError">&nbsp;</td>
            </tr>
            <tr>
                <td colspan="4" align="right" id="tdLastSync" class="lastSync"></td>
            </tr>
    """.trimIndent()

  document.getElementById("table")!!.innerHTML = tableHtml

  // Enabled checkbox
  document.getElementById("chkSyncEnabled")!!.addEventListener("change", ::onSyncEnabledChange)
  document.getElementById("txtEnabledDisabled")!!.addEventListener("click", ::onSyncEnabledClick)

  // Items remove buttons
  for (syncItem in settings.syncItems) {
    document.getElementById("btnRemove_${syncItem.folderName}")!!.addEventListener("click", ::onRemoveClick)
  }

  // Add item validation and button
  document.getElementById("inputFolderName")!!.addEventListener("input", ::onAddItemInputChange)
  document.getElementById("inputUrl")!!.addEventListener("input", ::onAddItemInputChange)
  document.getElementById("btnAdd")!!.addEventListener("click", ::onAddClick)
}

private fun updateSyncEnabledText(syncEnabled: Boolean) {
  val divElement = document.getElementById("txtEnabledDisabled")!!
  divElement.innerHTML = if (syncEnabled) "Enabled" else "Disabled"
  if (syncEnabled) divElement.classList.remove("disabled") else divElement.classList.add("disabled")
}

private fun onSyncEnabledClick(@Suppress("UNUSED_PARAMETER") event: Event) {
  val chkSyncEnabled = document.getElementById("chkSyncEnabled") as HTMLInputElement
  val newSyncEnabled = !chkSyncEnabled.checked
  chkSyncEnabled.checked = newSyncEnabled
  updateSyncEnabled(newSyncEnabled)
}

private fun onSyncEnabledChange(event: Event) {
  val syncEnabled = (event.target as HTMLInputElement).checked
  updateSyncEnabled(syncEnabled)
}

private fun updateSyncEnabled(syncEnabled: Boolean) = GlobalScope.launch {
  val settings = settingsManager.loadSettingsFromStorage()
  settingsManager.saveSettingsToStorage(settings.copy(syncEnabled = syncEnabled))

  updateSyncEnabledText(syncEnabled)
  messenger.sendSettingsChangedMessage()
}

private fun onRemoveClick(event: Event) {
  val folderName = (event.target as HTMLButtonElement).value
  logd("onRemoveClick folderName=$folderName")
  GlobalScope.launch {
    val settings = settingsManager.loadSettingsFromStorage()
    val settingsToSave = settings.copy(syncItems = settings.syncItems.filterNot { it.folderName == folderName })
    settingsManager.saveSettingsToStorage(settingsToSave)

    populateTable()
    messenger.sendSettingsChangedMessage()
  }
}

private fun onAddClick(@Suppress("UNUSED_PARAMETER") event: Event) {
  val folderName = (document.getElementById("inputFolderName") as HTMLInputElement).value
  val url = (document.getElementById("inputUrl") as HTMLInputElement).value
  logd("onAddClick folderName=$folderName url=$url")
  GlobalScope.launch {
    val settings = settingsManager.loadSettingsFromStorage()
    val syncItemsToSave = settings.syncItems + SyncItem(folderName, url)
    val settingsToSave = settings.copy(syncItems = syncItemsToSave)
    settingsManager.saveSettingsToStorage(settingsToSave)

    populateTable()
    messenger.sendSettingsChangedMessage()
  }
}

private fun onAddItemInputChange(@Suppress("UNUSED_PARAMETER") event: Event) {
  val folderName = (document.getElementById("inputFolderName") as HTMLInputElement).value
  val url = (document.getElementById("inputUrl") as HTMLInputElement).value
  val btnAdd = document.getElementById("btnAdd") as HTMLButtonElement

  GlobalScope.launch {
    // Folder
    val isExistingFolder = bookmarkManager.isExistingFolder(folderName)
    val isAlreadySyncedFolder = isAlreadySyncedFolder(folderName)
    val tdFolderNameError = document.getElementById("tdFolderNameError")!!
    tdFolderNameError.innerHTML = when {
      folderName.isBlank() -> "&nbsp;"
      !isExistingFolder -> "Bookmark folder doesn't exist"
      isAlreadySyncedFolder -> "Folder is already added"
      else -> "&nbsp;"
    }

    // Url
    val isValidUrl = isValidUrl(url)
    val tdUrlError = document.getElementById("tdUrlError")!!
    tdUrlError.innerHTML = when {
      url.isBlank() -> "&nbsp;"
      !isValidUrl -> "Invalid URL"
      else -> "&nbsp;"
    }

    val areInputsValid = isExistingFolder && !isAlreadySyncedFolder && isValidUrl
    btnAdd.disabled = !areInputsValid
  }
}

private suspend fun isAlreadySyncedFolder(folderName: String): Boolean {
  val settings = settingsManager.loadSettingsFromStorage()
  return settings.syncItems.any { it.folderName.equalsIgnoreCase(folderName) }
}

private val onSyncStateChanged: (SyncState) -> Unit = { syncState ->
  val tdLastSync = document.getElementById("tdLastSync")!!
  if (syncState.isSyncing) {
    tdLastSync.innerHTML = "Sync ongoing..."
  } else {
    tdLastSync.innerHTML = if (syncState.lastSync == null) {
      ""
    } else {
      "Last sync: ${syncState.lastSync!!.toLocaleDateString()} ${
        syncState.lastSync!!.toLocaleTimeString(locales = emptyArray(), options = dateLocaleOptions {
          hour = "2-digit"
          minute = "2-digit"
        })
      }"
    }
  }

  GlobalScope.launch {
    val settings = settingsManager.loadSettingsFromStorage()
    for (syncItem in settings.syncItems) {
      val folderName = syncItem.folderName
      val imgSyncState = document.getElementById("imgSyncState_$folderName") as HTMLImageElement
      when (val folderSyncState = syncState.folderSyncStates[folderName]) {
        null -> {
          imgSyncState.src = "icons/empty.png"
          imgSyncState.title = ""
        }

        is FolderSyncState.Syncing -> {
          imgSyncState.src = "icons/syncing.png"
          imgSyncState.title = "Sync ongoing..."
        }

        is FolderSyncState.Error -> {
          imgSyncState.src = "icons/warning.png"
          imgSyncState.title = "Could not sync: ${folderSyncState.message}"
        }

        is FolderSyncState.Success -> {
          imgSyncState.src = "icons/success.png"
          imgSyncState.title = "Sync successful"
        }
      }
    }
  }
}
