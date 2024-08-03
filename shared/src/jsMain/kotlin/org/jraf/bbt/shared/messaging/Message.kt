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

package org.jraf.bbt.shared.messaging

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
sealed class Message

@Serializable
class LogMessage(
  val source: String,
  val level: Int,
  val format: String,
  val params: Array<out @Serializable(with = JsonStringAnySerializer::class) Any?>,
) : Message()

@Serializable
data object SettingsChangedMessage : Message()

@Serializable
class OffscreenExtractBookmarksFromFeedMessage(
  val body: String,
) : Message()

@Serializable
class OffscreenExtractBookmarksFromHtmlMessage(
  val body: String,
  val xPath: String?,
  val documentUrl: String,
) : Message()


private object JsonStringAnySerializer : KSerializer<Any> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Any", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Any) {
    encoder.encodeString(JSON.stringify(value))
  }

  override fun deserialize(decoder: Decoder): Any {
    return JSON.parse(decoder.decodeString())
  }
}
