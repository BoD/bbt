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

package org.jraf.bbt.offscreen.domparser

import org.jraf.bbt.shared.bookmarks.BookmarkItem
import org.jraf.bbt.shared.bookmarks.BookmarksDocument
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.logging.logw
import org.jraf.bbt.shared.util.relativeTo
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.XMLDocument
import org.w3c.dom.asList
import org.w3c.dom.evaluate
import org.w3c.dom.parsing.DOMParser
import org.w3c.dom.xpath.XPathResult

class DomParserBookmarkExtractor {
  /**
   * Extract bookmarks from a body that is an RSS or Atom feed.
   */
  fun extractBookmarksFromFeed(body: String): BookmarksDocument? {
    return try {
      val document = DOMParser().parseFromString(body, "text/xml") as XMLDocument
      val rootElementName = document.documentElement?.tagName
      if (rootElementName != "rss" && rootElementName != "feed") {
        logw("Text can't be parsed as RSS nor Atom: root element is not 'rss' nor 'feed'")
        return null
      }

      // `item` is for RSS / `entry` is for Atom
      val items = document.getElementsByTagName("item").asList()
        .ifEmpty { document.getElementsByTagName("entry").asList() }
      BookmarksDocument(
        bookmarks = items.map {
          // In RSS, the link is in the text inside the <link> tage / in Atom, it's in the href attribute
          val linkElement = it.getElementsByTagName("link").item(0)
          val link = linkElement?.textContent?.ifBlank { null }
            ?: linkElement?.getAttribute("href")

          // Title is the same in RSS / Atom
          val title = it.getElementsByTagName("title").item(0)?.textContent?.ifBlank { null } ?: "Untitled"
          BookmarkItem(
            title = title,
            url = link,
            bookmarks = null,
          )
        }
          .filterNot { it.url.isNullOrBlank() }
          .toTypedArray()
      )
    } catch (t: Throwable) {
      logw("Text can't be parsed as RSS nor Atom: ${t.message} %O", t.stackTraceToString())
      null
    }
  }

  /**
   * Extract bookmarks from a body that is an HTML document.
   * If [elementXPath] is not null, only the element at this XPath will be considered.
   */
  fun extractBookmarksFromHtml(
    body: String,
    elementXPath: String?,
    documentUrl: String,
  ): BookmarksDocument? {
    return try {
      val document = DOMParser().parseFromString(body, "text/html")
      val root = if (elementXPath != null) {
        logd("Using XPath expression: $elementXPath")
        val element = try {
          val xPathResult = document.evaluate(elementXPath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
          xPathResult.singleNodeValue as? Element
        } catch (t: Throwable) {
          logw("Error evaluating XPath expression '$elementXPath': ${t.message} %O", t.stackTraceToString())
          return null
        }
        if (element == null) {
          logw("Node at XPath expression '$elementXPath' not found or not an Element")
          return null
        } else {
          element
        }
      } else {
        document.body
      } ?: run {
        logw("No body found in the document")
        return null
      }
      val aElems = root.getElementsByTagName("a").asList()
      logd("Found ${aElems.size} <a> elements")
      BookmarksDocument(
        bookmarks = aElems.mapNotNull { aElem ->
          aElem as HTMLAnchorElement
          val title = aElem.textContent?.trim()?.ifBlank { null } ?: "Untitled"
          BookmarkItem(
            title = title,
            url = aElem.getAttribute("href")
              ?.ifBlank { null }?.relativeTo(documentUrl)
              ?: return@mapNotNull null,
            bookmarks = null,
          )
        }
          .toTypedArray()
      )
    } catch (t: Throwable) {
      logw("Text can't be parsed as HTML: ${t.message} %O", t.stackTraceToString())
      null
    }
  }
}
