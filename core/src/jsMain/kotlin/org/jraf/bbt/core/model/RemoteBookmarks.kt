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

package org.jraf.bbt.core.model

import chrome.offscreen.CreateParameters
import chrome.offscreen.createDocument
import chrome.runtime.ContextFilter
import kotlinx.coroutines.await
import org.jraf.bbt.shared.logging.logw
import org.jraf.bbt.shared.messaging.sendOffscreenParseFeedMessage
import org.jraf.bbt.shared.messaging.sendOffscreenParseHtmlMessage
import org.jraf.bbt.shared.remote.model.BookmarksDocument
import org.jraf.bbt.shared.remote.model.isValid

/**
 * Parse the given text as a JSON bookmarks document.
 */
fun parseJson(body: String): BookmarksDocument? {
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
 * Parse the given text as an RSS or Atom feed.
 *
 * The `DomParser` API is not available in the Service Worker context, so we offload this to an offscreen document.
 * Yes, this is convoluted :(. Thanks Chrome!
 */
suspend fun parseFeed(body: String): BookmarksDocument? {
  ensureOffscreenDocumentCreated()
  return sendOffscreenParseFeedMessage(body)
}

/**
 * Parse the given text as an HTML document.
 *
 * The `DomParser` API is not available in the Service Worker context, so we offload this to an offscreen document.
 * Yes, this is convoluted :(. Thanks Chrome!
 */
suspend fun parseHtml(body: String, elementXPath: String?, documentUrl: String): BookmarksDocument? {
  ensureOffscreenDocumentCreated()
  return sendOffscreenParseHtmlMessage(body, elementXPath, documentUrl)
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
