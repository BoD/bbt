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

package org.jraf.bbt.util

import chrome.bookmarks.BookmarkTreeNode
import chrome.bookmarks.CreateDetails
import chrome.bookmarks.SearchQuery
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun findFolder(folderName: String): BookmarkTreeNode? {
    return suspendCoroutine { cont ->
        chrome.bookmarks.search(SearchQuery()) { bookmarkTreeNodes ->
            for (bookmarkTreeNode in bookmarkTreeNodes) {
                if (bookmarkTreeNode.title.equalsIgnoreCase(folderName.uppercase()) && bookmarkTreeNode.url == null) {
                    cont.resume(bookmarkTreeNode)
                    return@search
                }
            }
            cont.resume(null)
        }
    }
}

suspend fun isExistingFolder(folderName: String) = findFolder(folderName) != null

suspend fun getFolderChildren(folderId: String): Array<BookmarkTreeNode> {
    return suspendCoroutine { cont ->
        chrome.bookmarks.getChildren(folderId) {
            cont.resume(it)
        }
    }
}

suspend fun removeBookmarkTree(folderId: String) {
    suspendCoroutine<Unit> { cont ->
        chrome.bookmarks.removeTree(folderId) {
            cont.resume(Unit)
        }
    }
}

suspend fun emptyFolder(folder: BookmarkTreeNode) {
    logd("Emptying folder ${folder.title}")
    val children = getFolderChildren(folder.id)
    for (child in children) {
        removeBookmarkTree(child.id)
    }
}

suspend fun createBookmark(parentId: String, title: String, url: String? = null): BookmarkTreeNode {
    return suspendCoroutine { cont ->
        chrome.bookmarks.create(
            CreateDetails(
                parentId = parentId,
                title = title,
                url = url
            )
        ) {
            cont.resume(it)
        }
    }
}
