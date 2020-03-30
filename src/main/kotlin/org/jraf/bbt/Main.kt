package org.jraf.bbt

import chrome.alarms.AlarmOptions
import chrome.browserAction.BadgeBackgroundColor
import chrome.browserAction.BadgeText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
//import org.jraf.bbt.util.findFolder
import org.jraf.bbt.util.log
import org.jraf.bbt.util.retrieveSettingsFromStorage

private const val SYNC_PERIOD_MINUTES = 1
private const val ALARM_NAME = "BoD's Bookmark Tool"

fun main() {
    chrome.runtime.onInstalled.addListener {
        log("BoD's Bookmark Tool v1.1.0")
        chrome.alarms.onAlarm.addListener {
            log("Alarm triggered")
            GlobalScope.launch {
                syncFolders()
            }
        }
        GlobalScope.launch {
            onSettingsChanged()
        }
    }
}

private suspend fun onSettingsChanged() {
    val settings = retrieveSettingsFromStorage()
    if (settings.syncEnabled) {
        log("Sync enabled, scheduled every $SYNC_PERIOD_MINUTES minutes")
        updateBadge(true)
        syncFolders()
        startScheduling()
    } else {
        log("Sync disabled")
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
    log("Start syncing...")
    val settings = retrieveSettingsFromStorage()
    for (syncItem in settings.syncItems) {
        val ok = syncFolder(syncItem.folderName, syncItem.remoteBookmarksUrl)
        if (ok) {
            log("Finished sync of '${syncItem.folderName}' successfully")
        } else {
            log("Finished sync of '${syncItem.folderName}' with error")
        }
    }
    log("Sync finished")
    log("")
}

private suspend fun syncFolder(folderName: String, remoteBookmarksUrl: String): Boolean {
    log("Syncing '$folderName' to $remoteBookmarksUrl")
//    val folder = findFolder(folderName)
//    if (folder == null) {
//        log("Could not find folder '$folderName'")
//        return false
//    }
//    val bookmarks = fetchRemoteBookmarks(remoteBookmarksUrl)
//    if (bookmarks == null) {
//        log("Could not fetch remote bookmarks from $remoteBookmarksUrl for folder '$folderName'")
//        return false
//    }
//    log("Fetched object: %O", bookmarks)
//    val bookmarkObject = bookmarks.bookmarks
//    if (bookmarkObject == null) {
//        log("Fetched object doesn't seem to be in a compatible `bookmarks` format")
//        return false
//    }
//    emptyFolder(folder)
//    log("Populating folder ${folder.title}")
//    populateFolder(folder, bookmarkObject)
    return true
}
