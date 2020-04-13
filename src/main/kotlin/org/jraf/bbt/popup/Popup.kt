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

package org.jraf.bbt.popup

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jraf.bbt.VERSION
import org.jraf.bbt.main.EXTENSION_NAME
import org.jraf.bbt.main.syncStatePublisher
import org.jraf.bbt.model.FolderSyncState
import org.jraf.bbt.model.SyncState
import org.jraf.bbt.settings.SyncItem
import org.jraf.bbt.settings.loadSettingsFromStorage
import org.jraf.bbt.settings.saveSettingsToStorage
import org.jraf.bbt.util.equalsIgnoreCase
import org.jraf.bbt.util.isExistingFolder
import org.jraf.bbt.util.isValidUrl
import org.jraf.bbt.util.logd
import org.jraf.bbt.util.postMessageToBackgroundPage
import org.jraf.bbt.util.transitiveMessage
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import kotlin.browser.document
import kotlin.browser.window

fun isInPopup() = document.getElementById("bbt") != null

fun onPopupOpen() {
    logd("Popup open")
    document.getElementById("nameAndVersion")!!.innerHTML = "$EXTENSION_NAME $VERSION"
    GlobalScope.launch {
        populateTable()
    }
}

private suspend fun populateTable() {
    val settings = loadSettingsFromStorage()

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
                <td><input class="input folderName" type="text" placeholder="Folder name" value="${syncItem.folderName}" readonly="true"></td>
                <td class="url"><input class="input url" type="text" placeholder="Remote bookmarks URL" value="${syncItem.remoteBookmarksUrl}" readonly="true"></td>
                <td><img id="imgSyncState_${syncItem.folderName}" src="icons/empty.png" width="20" height="20"></td>
                <td><button type="button" id="btnRemove_${syncItem.folderName}" value="${syncItem.folderName}">Remove</button>
            </tr>
        """.trimIndent()
    }

    // Add item section, validation message, and last sync info
    tableHtml += """
            <tr>
                <td><input class="input folderName" type="text" placeholder="Folder name" id="inputFolderName"></td>
                <td class="url"><input class="input url" type="text" placeholder="Remote bookmarks URL" id="inputUrl"></td>
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

    // Observe sync state
    syncStatePublisher.addObserver(onSyncStateChanged)
    window.onblur = { syncStatePublisher.removeObserver(onSyncStateChanged) }
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
    val settings = loadSettingsFromStorage()
    saveSettingsToStorage(settings.copy(syncEnabled = syncEnabled))

    updateSyncEnabledText(syncEnabled)
    postOnSettingsChanged()
}

private fun onRemoveClick(event: Event) {
    val folderName = (event.target as HTMLButtonElement).value
    logd("onRemoveClick folderName=$folderName")
    GlobalScope.launch {
        val settings = loadSettingsFromStorage()
        val settingsToSave = settings.copy(syncItems = settings.syncItems.filterNot { it.folderName == folderName })
        saveSettingsToStorage(settingsToSave)

        populateTable()
        postOnSettingsChanged()
    }
}

private fun onAddClick(@Suppress("UNUSED_PARAMETER") event: Event) {
    val folderName = (document.getElementById("inputFolderName") as HTMLInputElement).value
    val url = (document.getElementById("inputUrl") as HTMLInputElement).value
    logd("onAddClick folderName=$folderName url=$url")
    GlobalScope.launch {
        val settings = loadSettingsFromStorage()
        val syncItemsToSave = settings.syncItems + SyncItem(folderName, url)
        val settingsToSave = settings.copy(syncItems = syncItemsToSave)
        saveSettingsToStorage(settingsToSave)

        populateTable()
        postOnSettingsChanged()
    }
}

private fun onAddItemInputChange(@Suppress("UNUSED_PARAMETER") event: Event) {
    val folderName = (document.getElementById("inputFolderName") as HTMLInputElement).value
    val url = (document.getElementById("inputUrl") as HTMLInputElement).value
    val btnAdd = document.getElementById("btnAdd") as HTMLButtonElement

    GlobalScope.launch {
        // Folder
        val isExistingFolder = isExistingFolder(folderName)
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
    val settings = loadSettingsFromStorage()
    return settings.syncItems.any { it.folderName.equalsIgnoreCase(folderName) }
}

private fun postOnSettingsChanged() = postMessageToBackgroundPage("onSettingsChanged")

private val onSyncStateChanged: (SyncState) -> Unit = { syncState ->
    logd("syncState=$syncState")
    val tdLastSync = document.getElementById("tdLastSync")!!
    if (syncState.isSyncing) {
        tdLastSync.innerHTML = "Sync ongoing…"
    } else {
        tdLastSync.innerHTML = if (syncState.lastSync == null) {
            ""
        } else {
            "Last sync: ${syncState.lastSync.toLocaleDateString()} ${syncState.lastSync.toLocaleTimeString(locales = emptyArray(), options = dateLocaleOptions {
                hour = "2-digit"
                minute = "2-digit"
            })}"
        }
    }

    GlobalScope.launch {
        val settings = loadSettingsFromStorage()
        for (syncItem in settings.syncItems) {
            val folderName = syncItem.folderName
            val imgSyncState = document.getElementById("imgSyncState_$folderName") as HTMLImageElement
            val folderSyncState = syncState.folderSyncStates[folderName]
            when {
                folderSyncState == null -> {
                    imgSyncState.src = "icons/empty.png"
                    imgSyncState.title = ""
                }
                folderSyncState.isSyncing -> {
                    imgSyncState.src = "icons/loading.gif"
                    imgSyncState.title = "Sync ongoing…"
                }
                folderSyncState.isError -> {
                    imgSyncState.src = "icons/warning.png"
                    imgSyncState.title = "Could not sync: ${(folderSyncState.unsafeCast<FolderSyncState.Error>()).cause.transitiveMessage}"
                }
                folderSyncState.isSuccess -> {
                    imgSyncState.src = "icons/success.png"
                    imgSyncState.title = "Sync successful"
                }
            }
        }
    }
}
