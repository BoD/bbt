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

import kotlin.js.Date

data class SyncState(
    val lastSync: Date?,
    val folderSyncStates: Map<String, FolderSyncState>
) {
    val isSyncing get() = folderSyncStates.values.any { it is Syncing }

    fun asStartSyncing() = copy(folderSyncStates = mapOf())
    fun asSyncing(folderName: String) = copy(folderSyncStates = folderSyncStates + (folderName to Syncing))
    fun asError(folderName: String, cause: Throwable) = copy(folderSyncStates = folderSyncStates + (folderName to Error(cause)))
    fun asSuccess(folderName: String) = copy(folderSyncStates = folderSyncStates + (folderName to Success))
    fun asFinishSyncing() = copy(lastSync = Date())

    companion object {
        fun initialState() = SyncState(null, mapOf())
    }
}

sealed class FolderSyncState {
    abstract val isSyncing: Boolean
    abstract val isError: Boolean
    abstract val isSuccess: Boolean
}

object Syncing : FolderSyncState() {
    override val isSyncing = true
    override val isError = false
    override val isSuccess = false
}

data class Error(val cause: Throwable) : FolderSyncState() {
    override val isSyncing = false
    override val isError = true
    override val isSuccess = false
}

object Success : FolderSyncState() {
    override val isSyncing = false
    override val isError = false
    override val isSuccess = true
}
