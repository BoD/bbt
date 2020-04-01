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
import org.jraf.bbt.main.EXTENSION_NAME
import org.jraf.bbt.main.VERSION
import org.jraf.bbt.main.onSettingsChanged
import org.jraf.bbt.settings.loadSettingsFromStorage
import org.jraf.bbt.settings.saveSettingsToStorage
import org.jraf.bbt.util.logd
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import kotlin.browser.document

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
    val syncEnabledHtml = if (settings.syncEnabled) "checked" else ""
    var tableHtml = """
        <tr>
            <td colspan="3">
                <input class="checkbox" $syncEnabledHtml type="checkbox" id="chkSyncEnabled" name="chkSyncEnabled">
                <label for="chkSyncEnabled">Enabled</label>
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

    // Footer
    tableHtml += """
            <tr>
                <td><input class="input" type="text" placeholder="Folder name" id="inputFolderName"></td>
                <td><input class="input url" type="text" placeholder="Remote bookmarks URL" id="inputUrl"></td>
                <td><button type="button" id="btnAdd">Add</button>
            </tr>
    """.trimIndent()

    document.getElementById("table")!!.innerHTML = tableHtml

    // Enabled checkbox
    document.getElementById("chkSyncEnabled")!!.addEventListener("change", ::onSyncEnabledChanged)

    // Sync items remove button
    for (syncItem in settings.syncItems) {
        document.getElementById("btnRemove_${syncItem.folderName}")!!.addEventListener("click", ::onRemoveClicked)
    }
}

private fun onSyncEnabledChanged(event: Event) {
    val syncEnabled = (event.target as HTMLInputElement).checked
    GlobalScope.launch {
        val settings = loadSettingsFromStorage()
        saveSettingsToStorage(settings.copy(syncEnabled = syncEnabled))

        onSettingsChanged()
    }
}

private fun onRemoveClicked(event: Event) {
    val folderName = (event.target as HTMLButtonElement).value
    logd("onRemoveClicked folderName=$folderName")
    GlobalScope.launch {
        val settings = loadSettingsFromStorage()
        saveSettingsToStorage(settings.copy(syncItems = settings.syncItems.filterNot { it.folderName == folderName }))

        populateTable()
        onSettingsChanged()
    }
}