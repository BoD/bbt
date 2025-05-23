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

package org.jraf.bbt.shared.domparser

import org.jraf.bbt.shared.bookmarks.BookmarkItem
import org.jraf.bbt.shared.bookmarks.BookmarksDocument
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.logging.logw
import org.jraf.bbt.shared.util.relativeTo
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.Node
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
   * Extract bookmarks from a body that is an OPML document.
   */
  fun extractBookmarksFromOpml(body: String): BookmarksDocument? {
    return try {
      val document = DOMParser().parseFromString(body, "text/xml") as XMLDocument
      val rootElementName = document.documentElement?.tagName
      if (rootElementName != "opml") {
        logw("Text can't be parsed as OPML: root element is not 'opml'")
        return null
      }

      fun Element.toBookmarkItem(): BookmarkItem? {
        val title = getAttribute("text")?.ifBlank { null }
          ?: getAttribute("title")?.ifBlank { null }
          ?: "Untitled"
        val url = getAttribute("url")?.ifBlank { null }
          ?: getAttribute("htmlUrl")?.ifBlank { null }
          ?: getAttribute("xmlUrl")?.ifBlank { null }
        return if (url == null) {
          val children = getChildrenByTagName("outline")
          if (children.isEmpty()) {
            logw("Outline element has no url attribute and no outline children")
            null
          } else {
            BookmarkItem(
              title = title,
              url = null,
              bookmarks = children.mapNotNull { it.toBookmarkItem() }.toTypedArray(),
            )
          }
        } else {
          BookmarkItem(
            title = title,
            url = url,
            bookmarks = null,
          )
        }
      }

      val bodyElement = document.getElementsByTagName("body").item(0) ?: run {
        logw("No body element found in the document")
        return null
      }
      val outlineElements = bodyElement.getChildrenByTagName("outline")
      BookmarksDocument(
        bookmarks = outlineElements.mapNotNull { outlineElement ->
          outlineElement.toBookmarkItem()
        }
          .toTypedArray()
      )
    } catch (t: Throwable) {
      logw("Text can't be parsed as OPML: ${t.message} %O", t.stackTraceToString())
      null
    }
  }

  /**
   * Extract bookmarks from a body that is an HTML document.
   * If [xPath] is not null, only the element(s) at this XPath will be considered.
   *
   * The XPath expression can either point to a list of `A` elements, or to a single (non `A`) element in which case, a list of `A` elements will be searched in its children.
   */
  fun extractBookmarksFromHtml(
    body: String,
    xPath: String?,
    documentUrl: String,
  ): BookmarksDocument? {
    return try {
      val document = DOMParser().parseFromString(body, "text/html")
      val aElems: List<Element> = if (xPath != null) {
        logd("Using XPath expression: $xPath")
        document.getAElementsAtXPath(xPath) ?: return null
      } else {
        (document.body ?: run {
          logw("No body found in the document")
          return null
        }).getElementsByTagName("a").asList()
      }

      logd("Found ${aElems.size} <a> elements")
      BookmarksDocument(
        bookmarks = aElems.mapNotNull { aElem ->
          aElem as HTMLAnchorElement
          val title = aElem.innerText.trim().ifBlank { "Untitled" }
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

  private fun Document.getAElementsAtXPath(xPath: String): List<HTMLAnchorElement>? {
    return try {
      val nodesAtXpath = getNodesAtXPath(xPath)
      if (nodesAtXpath.size == 1) {
        // Single element
        val singleNode = nodesAtXpath[0]
        if (singleNode is HTMLAnchorElement) {
          // Single A
          nodesAtXpath
        } else {
          // Single non-A
          val singleElement = singleNode as? Element
          if (singleElement == null) {
            logw("Element at XPath is not an Element: $singleNode")
            null
          } else {
            singleElement.getElementsByTagName("a").asList()
          }
        }
      } else {
        // Multiple elements
        nodesAtXpath
      }?.filterIsInstance<HTMLAnchorElement>()
    } catch (t: Throwable) {
      logw("Error evaluating XPath expression '$xPath': ${t.message} %O", t.stackTraceToString())
      null
    }
  }

  private fun Document.getNodesAtXPath(xPath: String): MutableList<Node> {
    val xPathResult = evaluate(xPath, this, null, XPathResult.ANY_TYPE, null)
    logd("XPath result type: ${xPathResult.resultType}")
    val nodesAtXpath = mutableListOf<Node>()
    var node: Node?
    while (true) {
      node = xPathResult.iterateNext()
      if (node == null) {
        break
      }
      nodesAtXpath.add(node)
    }
    return nodesAtXpath
  }
}

private fun Element.getChildrenByTagName(tagName: String) = getElementsByTagName(tagName).asList().filter { it.parentElement == this }
