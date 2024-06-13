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

package org.jraf.bbt.model

import org.jraf.bbt.util.logd
import org.jraf.bbt.util.logw
import org.jraf.bbt.util.relativeTo
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.XMLDocument
import org.w3c.dom.asList
import org.w3c.dom.evaluate
import org.w3c.dom.parsing.DOMParser
import org.w3c.dom.xpath.XPathResult

private const val MAX_BOOKMARKS = 100

interface BookmarksDocument {
  val version: Int
  val bookmarks: Array<BookmarkItem>

  companion object {
    const val FORMAT_VERSION = 1

    fun parseJson(text: String): BookmarksDocument? {
      return try {
        JSON.parse<BookmarksDocument>(text).run {
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

    fun parseRssOrAtom(text: String): BookmarksDocument? {
      return try {
        val document = DOMParser().parseFromString(text, "text/xml") as XMLDocument
        val rootElementName = document.documentElement?.tagName
        if (rootElementName != "rss" && rootElementName != "feed") {
          logw("Text can't be parsed as RSS nor Atom: root element is not 'rss' nor 'feed'")
          return null
        }

        // "item" is for RSS / "entry" is for Atom
        val items = document.getElementsByTagName("item").asList()
          .ifEmpty { document.getElementsByTagName("entry").asList() }
        BookmarksDocumentImpl(
          version = FORMAT_VERSION,
          bookmarks = items.map {
            // In RSS, the link is in the text inside the <link> tage / in Atom, it's in the href attribute
            val linkElement = it.getElementsByTagName("link").item(0)
            val link = linkElement?.textContent?.ifBlank { null }
              ?: linkElement?.getAttribute("href")

            // Title is the same in RSS / Atom
            val title = it.getElementsByTagName("title").item(0)?.textContent?.ifBlank { null } ?: "Untitled"
            BookmarkItemImpl(
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

    fun parseHtml(
      text: String,
      elementXPath: String?,
      documentUrl: String,
    ): BookmarksDocument? {
      return try {
        val document = DOMParser().parseFromString(text, "text/html")
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
        BookmarksDocumentImpl(
          version = FORMAT_VERSION,
          bookmarks = aElems.mapNotNull { aElem ->
            aElem as HTMLAnchorElement
            val title = aElem.textContent?.trim()?.ifBlank { null } ?: "Untitled"
            BookmarkItemImpl(
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
}

data class BookmarksDocumentImpl(
  override val version: Int,

  @Suppress("ArrayInDataClass")
  override val bookmarks: Array<BookmarkItem>,
) : BookmarksDocument

interface BookmarkItem {
  val title: String
  val url: String?
  val bookmarks: Array<BookmarkItem>?
}

fun BookmarksDocument.isValid(): Boolean {
  return version == BookmarksDocument.FORMAT_VERSION
}

data class BookmarkItemImpl(
  override val title: String,
  override val url: String?,
  @Suppress("ArrayInDataClass")
  override val bookmarks: Array<BookmarkItem>?,
) : BookmarkItem

fun BookmarkItem.isFolder() = bookmarks != null
fun BookmarkItem.isBookmark() = url != null

fun BookmarksDocument.sanitize(): BookmarksDocument {
  return BookmarksDocumentImpl(
    version = BookmarksDocument.FORMAT_VERSION,
    bookmarks = bookmarks.take(MAX_BOOKMARKS).map { it.sanitize() }.toTypedArray()
  )
}

fun BookmarkItem.sanitize(): BookmarkItem {
  return BookmarkItemImpl(
    title = title,
    url = url,
    bookmarks = bookmarks?.take(MAX_BOOKMARKS)?.map { it.sanitize() }?.toTypedArray()
  )
}
