package org.jraf.bbt.util

import kotlinx.coroutines.suspendCancellableCoroutine

class Settings(
    val syncEnabled: Boolean,
    val syncItems: Array<SyncItem>
)

data class SyncItem(
    val folderName: String,
    val remoteBookmarksUrl: String
)


suspend fun retrieveSettingsFromStorage(): Settings {
    return suspendCancellableCoroutine { cont ->
        chrome.storage.sync.get("settings") { items ->
            val obj = items.settings
            val res = if (obj == undefined) {
                Settings(
                    syncEnabled = true,
                    syncItems = arrayOf(SyncItem("Sample", "https://jraf.org/static/tmp/bbt/bookmarks.json"))
                )
            } else {
                Settings(
                    syncEnabled = obj.syncEnabled as Boolean,
                    syncItems = obj.syncItems as Array<SyncItem>
                )
            }
            cont.resume(res) {}
        }
    }
}