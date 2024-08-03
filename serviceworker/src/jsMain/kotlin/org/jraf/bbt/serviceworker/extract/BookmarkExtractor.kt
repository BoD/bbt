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

package org.jraf.bbt.serviceworker.extract

import chrome.offscreen.CreateParameters
import chrome.offscreen.createDocument
import chrome.runtime.ContextFilter
import kotlinx.coroutines.await
import org.jraf.bbt.shared.bookmarks.BookmarksDocument
import org.jraf.bbt.shared.bookmarks.isValid
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.logging.logw
import org.jraf.bbt.shared.messaging.Messenger.Companion.messenger

class BookmarkExtractor {
  suspend fun extractBookmarks(body: String, xPath: String?, documentUrl: String): BookmarksDocument? {
    var bookmarksDocument = extractBookmarksFromJson(body)
    if (bookmarksDocument != null) {
      return bookmarksDocument
    }

    logd("Could not parse fetched text as JSON, trying RSS/Atom")
    bookmarksDocument = extractBookmarksFromFeed(body)
    if (bookmarksDocument != null) {
      return bookmarksDocument
    }

    logd("Could not parse fetched text as RSS/Atom, trying OPML")
    bookmarksDocument = extractBookmarksFromOpml(body)
    if (bookmarksDocument != null) {
      return bookmarksDocument
    }

    logd("Could not parse fetched text as OPML, trying HTML")
    bookmarksDocument = extractBookmarksFromHtml(
      body = body,
      xPath = xPath,
      documentUrl = documentUrl
    )
    if (bookmarksDocument != null) {
      return bookmarksDocument
    }

    logd("Could not parse fetched text as HTML, give up")
    return null
  }

  /**
   * Extract bookmarks from a body that is in the JSON bookmarks format.
   */
  fun extractBookmarksFromJson(body: String): BookmarksDocument? {
    return try {
      JSON.parse<BookmarksDocument>(body).run {
        if (!isValid()) {
          logw("Invalid bookmarks document: %O", this)
          null
        } else {
          this
        }
      }
    } catch (t: Throwable) {
      logw("Text can't be parsed as JSON BookmarksDocument: ${t.message} %O", t.stackTraceToString())
      null
    }
  }

  /**
   * Extract bookmarks from a body that is an RSS or Atom feed.
   *
   * The `DomParser` API is not available in the Service Worker context, so we offload this to an offscreen document.
   * Yes, this is convoluted :(. Thanks Chrome!
   */
  suspend fun extractBookmarksFromFeed(body: String): BookmarksDocument? {
    ensureOffscreenDocumentCreated()
    return messenger.sendOffscreenExtractBookmarksFromFeedMessage(body)
  }

  /**
   * Extract bookmarks from a body that is an OPML document.
   *
   * The `DomParser` API is not available in the Service Worker context, so we offload this to an offscreen document.
   * Yes, this is convoluted :(. Thanks Chrome!
   */
  suspend fun extractBookmarksFromOpml(body: String): BookmarksDocument? {
    ensureOffscreenDocumentCreated()
    return messenger.sendOffscreenExtractBookmarksFromOpmlMessage(body)
  }

  /**
   * Extract bookmarks from a body that is an HTML document.
   * If [xPath] is not null, only the element at this XPath will be considered.
   *
   * The `DomParser` API is not available in the Service Worker context, so we offload this to an offscreen document.
   * Yes, this is convoluted :(. Thanks Chrome!
   */
  suspend fun extractBookmarksFromHtml(body: String, xPath: String?, documentUrl: String): BookmarksDocument? {
    ensureOffscreenDocumentCreated()
    return messenger.sendOffscreenExtractBookmarksFromHtmlMessage(body, xPath, documentUrl)
  }

  private suspend fun ensureOffscreenDocumentCreated() {
    val offscreenRelativePath = "offscreen.html"
    val fullyQualifiedPath = chrome.runtime.getURL(offscreenRelativePath)
    val existingContexts = chrome.runtime.getContexts(
      ContextFilter(
        contextTypes = arrayOf("OFFSCREEN_DOCUMENT"),
        documentUrls = arrayOf(fullyQualifiedPath)
      )
    ).await()
    if (existingContexts.isNotEmpty()) {
      return
    }
    createDocument(
      CreateParameters(
        justification = "Parse DOM",
        reasons = arrayOf("DOM_PARSER"),
        url = offscreenRelativePath,
      )
    ).await()
  }
}
