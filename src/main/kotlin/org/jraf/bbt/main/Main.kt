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

package org.jraf.bbt.main

import chrome.alarms.AlarmOptions
import chrome.bookmarks.BookmarkTreeNode
import chrome.browserAction.BadgeBackgroundColor
import chrome.browserAction.BadgeText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jraf.bbt.VERSION
import org.jraf.bbt.model.BookmarkItem
import org.jraf.bbt.model.BookmarksDocument
import org.jraf.bbt.model.isBookmark
import org.jraf.bbt.popup.isInPopup
import org.jraf.bbt.popup.onPopupOpen
import org.jraf.bbt.settings.loadSettingsFromStorage
import org.jraf.bbt.util.FetchException
import org.jraf.bbt.util.createBookmark
import org.jraf.bbt.util.emptyFolder
import org.jraf.bbt.util.fetchJson
import org.jraf.bbt.util.findFolder
import org.jraf.bbt.util.logd
import org.jraf.bbt.util.logi
import org.jraf.bbt.util.logw

const val EXTENSION_NAME = "BoD's Bookmark Tool"

private const val SYNC_PERIOD_MINUTES = 30
private const val ALARM_NAME = EXTENSION_NAME

// Note: this is executed when the extension is installed, and
// also every time popup.html is opened.
fun main() {
    if (isInPopup()) {
        onPopupOpen()
    } else {
        chrome.runtime.onInstalled.addListener {
            logi("$EXTENSION_NAME $VERSION")
            chrome.alarms.onAlarm.addListener {
                logd("Alarm triggered")
                GlobalScope.launch {
                    syncFolders()
                }
            }
            GlobalScope.launch {
                onSettingsChanged()
            }
        }
    }
}

suspend fun onSettingsChanged() {
    val settings = loadSettingsFromStorage()
    if (settings.syncEnabled) {
        logd("Sync enabled, scheduled every $SYNC_PERIOD_MINUTES minutes")
        updateBadge(true)
        syncFolders()
        startScheduling()
    } else {
        logd("Sync disabled")
        updateBadge(false)
        stopScheduling()
    }
}

private fun updateBadge(enabled: Boolean) {
    if (enabled) {
        chrome.browserAction.setBadgeText(BadgeText(""))
    } else {
        chrome.browserAction.setBadgeText(BadgeText("OFF"))
        chrome.browserAction.setBadgeBackgroundColor(BadgeBackgroundColor("#808080"))
    }
}

private fun startScheduling() {
    chrome.alarms.create(ALARM_NAME, AlarmOptions(periodInMinutes = SYNC_PERIOD_MINUTES))
}

private fun stopScheduling() {
    chrome.alarms.clearAll()
}

private suspend fun syncFolders() {
    logd("Start syncing...")
    val settings = loadSettingsFromStorage()
    for (syncItem in settings.syncItems) {
        val ok = syncFolder(syncItem.folderName, syncItem.remoteBookmarksUrl)
        if (ok) {
            logd("Finished sync of '${syncItem.folderName}' successfully")
        } else {
            logd("Finished sync of '${syncItem.folderName}' with error")
        }
    }
    logd("Sync finished")
    logd("")
}

private suspend fun syncFolder(folderName: String, remoteBookmarksUrl: String): Boolean {
    logd("Syncing '$folderName' to $remoteBookmarksUrl")
    val folder = findFolder(folderName)
    if (folder == null) {
        logw("Could not find folder '$folderName'")
        return false
    }
    val bookmarksDocument = fetchRemoteBookmarks(remoteBookmarksUrl)
    if (bookmarksDocument == null) {
        logw("Could not fetch remote bookmarks from $remoteBookmarksUrl for folder '$folderName'")
        return false
    }
    val bookmarkItems = bookmarksDocument.bookmarks
    emptyFolder(folder)
    logd("Populating folder ${folder.title}")
    populateFolder(folder, bookmarkItems)
    return true
}

private suspend fun fetchRemoteBookmarks(remoteBookmarksUrl: String): BookmarksDocument? {
    logd("Fetching bookmarks from remote $remoteBookmarksUrl")
    return try {
        val dynamicObject: dynamic = fetchJson(remoteBookmarksUrl)
        logd("Fetched object: %O", dynamicObject.unsafeCast<Any?>())
        if (!BookmarksDocument.isValid(dynamicObject)) {
            logw("Fetched object doesn't seem to be a valid `bookmarks` format document")
            null
        } else {
            dynamicObject
        }
    } catch (e: FetchException) {
        logw("Could not fetch from remote $remoteBookmarksUrl: %O", e)
        null
    }
}

private suspend fun populateFolder(folder: BookmarkTreeNode, bookmarkItems: Array<BookmarkItem>) {
    for (bookmarkItem in bookmarkItems) {
        if (bookmarkItem.isBookmark()) {
            createBookmark(parentId = folder.id, title = bookmarkItem.title, url = bookmarkItem.url)
        } else {
            val createdFolder = createBookmark(parentId = folder.id, title = bookmarkItem.title)
            // Recurse
            populateFolder(createdFolder, bookmarkItem.bookmarks!!)
        }
    }
}
