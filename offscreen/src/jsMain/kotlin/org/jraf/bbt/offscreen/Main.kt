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

package org.jraf.bbt.offscreen

import org.jraf.bbt.offscreen.domparser.parseFeed
import org.jraf.bbt.offscreen.domparser.parseHtml
import org.jraf.bbt.shared.logging.initLogs
import org.jraf.bbt.shared.logging.logd
import org.jraf.bbt.shared.messaging.Message
import org.jraf.bbt.shared.messaging.MessageType
import org.jraf.bbt.shared.messaging.OffscreenParseFeedPayload
import org.jraf.bbt.shared.messaging.OffscreenParseHtmlPayload

fun main() {
  initLogs(logWithMessages = true, sourceName = "Offscreen")
  logd("main")
  chrome.runtime.onMessage.addListener { msg, sender, sendResponse ->
    val message = msg.unsafeCast<Message>()
    when (message.type) {
      MessageType.OFFSCREEN_PARSE_FEED.ordinal -> {
        val logMessage = message.payload.unsafeCast<OffscreenParseFeedPayload>()
        sendResponse(parseFeed(logMessage.body))
      }

      MessageType.OFFSCREEN_PARSE_HTML.ordinal -> {
        val logMessage = message.payload.unsafeCast<OffscreenParseHtmlPayload>()
        sendResponse(
          parseHtml(
            body = logMessage.body,
            elementXPath = logMessage.elementXPath,
            documentUrl = logMessage.documentUrl,
          )
        )
      }
    }
  }
}
