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

@file:OptIn(ExperimentalJsExport::class)

package org.jraf.bbt.shared.bookmarks

private const val MAX_BOOKMARKS = 100


/**
 * A document containing bookmark items.
 */
// Need @JsExport because these will be jsonified / dejsonified
@JsExport
data class BookmarksDocument(
  val version: Int = FORMAT_VERSION,

  @Suppress("ArrayInDataClass")
  val bookmarks: Array<BookmarkItem>,
) {
  companion object {
    const val FORMAT_VERSION = 1
  }
}

fun BookmarksDocument.isValid(): Boolean {
  return version == BookmarksDocument.FORMAT_VERSION
}

/**
 * A bookmark item, which can either be a bookmark ([url] is not null and [bookmarks] is null) or a folder ([bookmarks] is not null and [url] is null).
 */
@JsExport
data class BookmarkItem(
  val title: String,
  val url: String?,
  @Suppress("ArrayInDataClass")
  val bookmarks: Array<BookmarkItem>?,
)

fun BookmarkItem.isFolder() = bookmarks != null
fun BookmarkItem.isBookmark() = url != null

fun BookmarksDocument.sanitize(): BookmarksDocument {
  return BookmarksDocument(
    version = BookmarksDocument.FORMAT_VERSION,
    bookmarks = bookmarks.take(MAX_BOOKMARKS).map { it.sanitize() }.toTypedArray()
  )
}

fun BookmarkItem.sanitize(): BookmarkItem {
  return BookmarkItem(
    title = title,
    url = url,
    bookmarks = bookmarks?.take(MAX_BOOKMARKS)?.map { it.sanitize() }?.toTypedArray()
  )
}
