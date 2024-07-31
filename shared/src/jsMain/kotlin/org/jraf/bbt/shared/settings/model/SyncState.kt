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

package org.jraf.bbt.shared.settings.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.js.Date

@Serializable
data class SyncState(
  @Serializable(with = DateSerializer::class)
  val lastSync: Date?,

  val folderSyncStates: Map<String, FolderSyncState>,
) {
  val isSyncing get() = folderSyncStates.values.any { it is FolderSyncState.Syncing }

  fun asStartSyncing() = copy(folderSyncStates = mapOf())
  fun asSyncing(folderName: String) = copy(folderSyncStates = folderSyncStates + (folderName to FolderSyncState.Syncing))
  fun asError(folderName: String, message: String) =
    copy(folderSyncStates = folderSyncStates + (folderName to FolderSyncState.Error(message)))

  fun asSuccess(folderName: String) = copy(folderSyncStates = folderSyncStates + (folderName to FolderSyncState.Success))
  fun asFinishSyncing() = copy(lastSync = Date())

  companion object {
    fun initialState() = SyncState(null, mapOf())
  }
}

@Serializable
sealed class FolderSyncState {
  @Serializable
  data object Syncing : FolderSyncState()

  @Serializable
  data class Error(val message: String) : FolderSyncState()

  @Serializable
  data object Success : FolderSyncState()
}

private object DateSerializer : KSerializer<Date> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.DOUBLE)

  override fun serialize(encoder: Encoder, value: Date) {
    encoder.encodeDouble(value.getTime())
  }

  override fun deserialize(decoder: Decoder): Date {
    return Date(decoder.decodeDouble())
  }
}
