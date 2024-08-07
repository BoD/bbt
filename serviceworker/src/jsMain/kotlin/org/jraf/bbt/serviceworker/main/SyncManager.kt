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

package org.jraf.bbt.serviceworker.main

import chrome.bookmarks.BookmarkTreeNode
import kotlinx.coroutines.flow.first
import org.jraf.bbt.serviceworker.extract.BookmarkExtractor
import org.jraf.bbt.serviceworker.fetch.FetchException
import org.jraf.bbt.serviceworker.fetch.fetchText
import org.jraf.bbt.shared.bookmarks.BookmarkItem
import org.jraf.bbt.shared.bookmarks.BookmarkManager.Companion.bookmarkManager
import org.jraf.bbt.shared.bookmarks.BookmarksDocument
import org.jraf.bbt.shared.bookmarks.isBookmark
import org.jraf.bbt.shared.bookmarks.sanitize
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.logging.logw
import org.jraf.bbt.shared.settings.SettingsManager.Companion.settingsManager
import org.jraf.bbt.shared.settings.model.SyncState
import org.jraf.bbt.shared.util.decodeURIComponent
import org.jraf.bbt.shared.util.transitiveMessage

class SyncManager {
  private val bookmarkExtractor = BookmarkExtractor()

  suspend fun syncFolders() {
    logd("Start syncing...")
    saveSyncState { asStartSyncing() }
    val settings = settingsManager.settings.first()
    for (syncItem in settings.syncItems) {
      saveSyncState { asSyncing(folderName = syncItem.folderName) }
      try {
        syncFolder(syncItem.folderName, syncItem.remoteBookmarksUrl)
        logd("Finished sync of '${syncItem.folderName}' successfully")
        saveSyncState { asSuccess(folderName = syncItem.folderName) }
      } catch (e: Exception) {
        logw("Finished sync of '${syncItem.folderName}' with error: %O", e.stackTraceToString())
        saveSyncState { asError(folderName = syncItem.folderName, message = e.transitiveMessage) }
      }
    }
    saveSyncState { asFinishSyncing() }
    logd("Sync finished")
    logd("")
  }

  private suspend fun syncFolder(folderName: String, remoteBookmarksUrl: String) {
    logd("Syncing '$folderName' to $remoteBookmarksUrl")
    val folder = bookmarkManager.findFolder(folderName) ?: throw RuntimeException("Could not find folder '$folderName'")
    val bookmarksDocument = try {
      fetchRemoteBookmarks(remoteBookmarksUrl)
    } catch (e: Exception) {
      throw RuntimeException("Could not fetch remote bookmarks from $remoteBookmarksUrl for folder '$folderName'", e)
    }
    val bookmarkItems = bookmarksDocument.bookmarks
    bookmarkManager.emptyFolder(folder)
    logd("Populating folder ${folder.title}")
    populateFolder(folder, bookmarkItems)
  }

  private suspend fun fetchRemoteBookmarks(remoteBookmarksUrl: String): BookmarksDocument {
    logd("Fetching bookmarks from remote $remoteBookmarksUrl")
    return try {
      val body = fetchText(remoteBookmarksUrl)
      bookmarkExtractor.extractBookmarks(
        body = body,
        xPath = remoteBookmarksUrl.extractXPathFragment(),
        documentUrl = remoteBookmarksUrl.withoutXPathFragment()
      )
        ?: run {
          throw RuntimeException("Fetched object doesn't seem to be either valid `bookmarks` JSON format document, RSS/Atom feed, or HTML")
        }
    } catch (e: FetchException) {
      throw RuntimeException("Could not fetch from remote $remoteBookmarksUrl", e)
    }.sanitize()
  }

  private suspend fun populateFolder(folder: BookmarkTreeNode, bookmarkItems: Array<BookmarkItem>) {
    for (bookmarkItem in bookmarkItems) {
      if (bookmarkItem.isBookmark()) {
        bookmarkManager.createBookmark(parentId = folder.id, title = bookmarkItem.title, url = bookmarkItem.url)
      } else {
        val createdFolder = bookmarkManager.createBookmark(parentId = folder.id, title = bookmarkItem.title)
        // Recurse
        populateFolder(createdFolder, bookmarkItem.bookmarks!!)
      }
    }
  }

  private suspend fun saveSyncState(transform: SyncState.() -> SyncState) {
    val settings = settingsManager.settings.first()
    settingsManager.saveSettingsToStorage(settings.copy(syncState = transform(settings.syncState)))
  }

  private fun String.extractXPathFragment(): String? {
    return this.substringAfterLast("#__xpath=", "").ifBlank { null }?.let { decodeURIComponent(it) }
  }

  private fun String.withoutXPathFragment(): String {
    return this.substringBeforeLast("#__xpath=", "")
  }
}
