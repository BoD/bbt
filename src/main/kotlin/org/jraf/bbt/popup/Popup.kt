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
import org.jraf.bbt.main.syncingPublisher
import org.jraf.bbt.settings.SyncItem
import org.jraf.bbt.settings.loadSettingsFromStorage
import org.jraf.bbt.settings.saveSettingsToStorage
import org.jraf.bbt.util.isExistingFolder
import org.jraf.bbt.util.isValidUrl
import org.jraf.bbt.util.logd
import org.jraf.bbt.util.postMessageToBackgroundPage
import org.w3c.dom.HTMLButtonElement
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
            <td colspan="2">
                <div class="onoffswitch"><input $syncEnabledCheckboxCheckedHtml type="checkbox" id="chkSyncEnabled" name="chkSyncEnabled" class="onoffswitch-checkbox"><label class="onoffswitch-label" for="chkSyncEnabled"><span class="onoffswitch-inner"></span><span class="onoffswitch-switch"></span></label></div>
                $syncEnabledLabelHtml
            </td>
            <td>
                <center><img id="imgSyncing" src="icons/loading.gif" width="20" height="20" hidden></center>
            </td>
        </tr>
    """.trimIndent()

    // Sync items
    for (syncItem in settings.syncItems) {
        tableHtml += """
            <tr>
                <td><input class="input" type="text" placeholder="Folder name" value="${syncItem.folderName}" readonly="true"></td>
                <td><input class="input url" type="text" placeholder="Remote bookmarks URL" value="${syncItem.remoteBookmarksUrl}" readonly="true"></td>
                <td><button type="button" id="btnRemove_${syncItem.folderName}" value="${syncItem.folderName}">Remove</button>
            </tr>
        """.trimIndent()
    }

    // Add item and validation message
    tableHtml += """
            <tr>
                <td><input class="input" type="text" placeholder="Folder name" id="inputFolderName"></td>
                <td><input class="input url" type="text" placeholder="Remote bookmarks URL" id="inputUrl"></td>
                <td><button type="button" id="btnAdd" disabled>Add</button>
            </tr>
            <tr>
                <td id="tdFolderNameError" class="validationError"></td>
                <td id="tdUrlError" class="validationError"></td>
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

    // Observe sync
    val onSyncChanged: (Boolean) -> Unit = { syncing ->
        logd("Syncing=$syncing")
        val imgSyncing = document.getElementById("imgSyncing")!!
        if (syncing) {
            imgSyncing.removeAttribute("hidden")
        } else {
            imgSyncing.setAttribute("hidden", "hidden")
        }
    }
    syncingPublisher.addObserver(onSyncChanged)
    window.onblur = { syncingPublisher.removeObserver(onSyncChanged) }
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
        val tdFolderNameError = document.getElementById("tdFolderNameError")!!
        tdFolderNameError.innerHTML = if (folderName.isNotBlank() && !isExistingFolder) {
            "Bookmark folder doesn't exist"
        } else {
            ""
        }

        // Url
        val isValidUrl = isValidUrl(url)
        val tdUrlError = document.getElementById("tdUrlError")!!
        tdUrlError.innerHTML = if (url.isNotBlank() && !isValidUrl) {
            "Invalid URL"
        } else {
            ""
        }

        val areInputsValid = isExistingFolder && isValidUrl
        btnAdd.disabled = !areInputsValid
    }
}

private fun postOnSettingsChanged() = postMessageToBackgroundPage("onSettingsChanged")
