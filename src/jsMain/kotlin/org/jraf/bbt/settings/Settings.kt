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

package org.jraf.bbt.settings

import org.jraf.bbt.util.logd
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private class StorageSettings(
    val syncEnabled: Boolean,
    val syncItems: Array<SyncItem>
)

data class Settings(
    val syncEnabled: Boolean,
    val syncItems: List<SyncItem>
)

data class SyncItem(
    val folderName: String,
    val remoteBookmarksUrl: String
)

private fun Settings.toStorageSettings() = StorageSettings(
    syncEnabled = syncEnabled,
    syncItems = syncItems.toTypedArray()
)


suspend fun loadSettingsFromStorage(): Settings {
    return suspendCoroutine { cont ->
        chrome.storage.sync.get("settings") { items ->
            val obj = items.settings
            val res = if (obj == undefined) {
                Settings(
                    syncEnabled = true,
                    syncItems = listOf(SyncItem("Sample", "https://jraf.org/static/tmp/bbt/bookmarks.json"))
                )
            } else {
                Settings(
                    syncEnabled = obj.syncEnabled as Boolean,
                    syncItems = (obj.syncItems as Array<SyncItem>).toList()
                )
            }
            cont.resume(res)
        }
    }
}

suspend fun saveSettingsToStorage(settings: Settings) {
    logd("Save settings to storage: %O", settings)
    suspendCoroutine<Unit> { cont ->
        val obj = js("{}")
        obj.settings = settings.toStorageSettings()
        chrome.storage.sync.set(obj) { cont.resume(Unit) }
    }
}
