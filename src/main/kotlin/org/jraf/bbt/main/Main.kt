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
import org.jraf.bbt.model.SyncState
import org.jraf.bbt.model.isBookmark
import org.jraf.bbt.popup.isInPopup
import org.jraf.bbt.popup.onPopupOpen
import org.jraf.bbt.settings.loadSettingsFromStorage
import org.jraf.bbt.util.*
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.browser.window

const val EXTENSION_NAME = "BoD's Bookmark Tool"

private const val SYNC_PERIOD_MINUTES = 30
private const val ALARM_NAME = EXTENSION_NAME

private val backgroundPage = chrome.extension.getBackgroundPage()

val syncStatePublisher: CachedPublisher<SyncState> by backgroundPage {
    CachedPublisher(SyncState.initialState())
}

// Note: this is executed when the extension starts, and
// also every time popup.html is opened.
fun main() {
    if (isInPopup()) {
        onPopupOpen()
    } else {
        logi("$EXTENSION_NAME $VERSION")

        window.addEventListener("message", ::onMessage)

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

private fun onMessage(event: Event) {
    event as MessageEvent
    logd("onMessage data=${event.data}")
    GlobalScope.launch {
        onSettingsChanged()
    }
}

private suspend fun onSettingsChanged() {
    val settings = loadSettingsFromStorage()
    if (settings.syncEnabled) {
        logd("Sync enabled, scheduled every $SYNC_PERIOD_MINUTES minutes")
        updateBadge(true)
        startScheduling()
        // Launch the sync in another coroutine to not make this fun blocking too long
        GlobalScope.launch {
            syncFolders()
        }
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
    publishSyncState { asStartSyncing() }
    val settings = loadSettingsFromStorage()
    for (syncItem in settings.syncItems) {
        publishSyncState { asSyncing(folderName = syncItem.folderName) }
        try {
            syncFolder(syncItem.folderName, syncItem.remoteBookmarksUrl)
            logd("Finished sync of '${syncItem.folderName}' successfully")
            publishSyncState { asSuccess(folderName = syncItem.folderName) }
        } catch (e: Exception) {
            logd("Finished sync of '${syncItem.folderName}' with error")
            publishSyncState { asError(folderName = syncItem.folderName, cause = e) }
        }
    }
    publishSyncState { asFinishSyncing() }
    logd("Sync finished")
    logd("")
}

private suspend fun syncFolder(folderName: String, remoteBookmarksUrl: String) {
    logd("Syncing '$folderName' to $remoteBookmarksUrl")
    val folder = findFolder(folderName) ?: throw RuntimeException("Could not find folder '$folderName'")
    val bookmarksDocument = try {
        fetchRemoteBookmarks(remoteBookmarksUrl)
    } catch (e: Exception) {
        throw RuntimeException("Could not fetch remote bookmarks from $remoteBookmarksUrl for folder '$folderName'", e)
    }
    val bookmarkItems = bookmarksDocument.bookmarks
    emptyFolder(folder)
    logd("Populating folder ${folder.title}")
    populateFolder(folder, bookmarkItems)
}

private suspend fun fetchRemoteBookmarks(remoteBookmarksUrl: String): BookmarksDocument {
    logd("Fetching bookmarks from remote $remoteBookmarksUrl")
    return try {
        val dynamicObject: dynamic = fetchJson(remoteBookmarksUrl)
        logd("Fetched object: %O", dynamicObject.unsafeCast<Any?>())
        if (!BookmarksDocument.isValid(dynamicObject)) {
            throw RuntimeException("Fetched object doesn't seem to be a valid `bookmarks` format document")
        } else {
            dynamicObject
        }
    } catch (e: FetchException) {
        throw RuntimeException("Could not fetch from remote $remoteBookmarksUrl", e)
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

private fun publishSyncState(syncState: SyncState.() -> SyncState) {
    syncStatePublisher.publish(syncStatePublisher.value!!.syncState())
}
