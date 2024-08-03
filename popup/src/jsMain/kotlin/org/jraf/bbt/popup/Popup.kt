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

package org.jraf.bbt.popup

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import bbt.popup.generated.resources.Res
import bbt.popup.generated.resources.check_24px
import bbt.popup.generated.resources.delete_24px
import bbt.popup.generated.resources.logo
import bbt.popup.generated.resources.sync_24px
import bbt.popup.generated.resources.warning_24px
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jraf.bbt.popup.components.DenseOutlinedTextField
import org.jraf.bbt.popup.components.Rotate
import org.jraf.bbt.popup.components.SwitchWithLabel
import org.jraf.bbt.popup.theme.successColor
import org.jraf.bbt.popup.theme.warningColor
import org.jraf.bbt.shared.VERSION
import org.jraf.bbt.shared.settings.model.FolderSyncState
import org.jraf.bbt.shared.settings.model.Settings
import org.jraf.bbt.shared.settings.model.SyncItem
import org.jraf.bbt.shared.settings.model.SyncState

@Composable
fun Popup(
  settings: Settings,
  onSyncEnabledCheckedChange: (Boolean) -> Unit,
  onAddItem: suspend (folderName: String, remoteBookmarksUrl: String) -> AddItemResult,
  onRemoveItem: (SyncItem) -> Unit,
) {
  Column(
    Modifier
      .padding(8.dp)
      .fillMaxSize()
  ) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
      )

      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
          modifier = Modifier.alpha(.75F),
          text = "BoD's Bookmark Tool $VERSION\n" + syncStatusText(syncState = settings.syncState),
          style = MaterialTheme.typography.labelSmall,
        )

        Spacer(Modifier.weight(1f))

        SwitchWithLabel(
          label = "Enabled",
          checked = settings.syncEnabled,
          onCheckedChange = onSyncEnabledCheckedChange
        )
      }
    }

    Spacer(Modifier.height(8.dp))

    SyncItemList(
      settings = settings,
      onAddItem = onAddItem,
      onRemoveItem = onRemoveItem,
    )
  }
}

private fun syncStatusText(syncState: SyncState) = if (syncState.isSyncing) {
  "Sync ongoing..."
} else {
  val lastSync = syncState.lastSync
  if (lastSync == null) {
    "Never synced"
  } else
    "Last sync: ${lastSync.toLocaleDateString()} ${
      lastSync.toLocaleTimeString(locales = emptyArray(), options = dateLocaleOptions {
        hour = "2-digit"
        minute = "2-digit"
      })
    }"
}

@Composable
private fun ColumnScope.SyncItemList(
  settings: Settings,
  onAddItem: suspend (folderName: String, remoteBookmarksUrl: String) -> AddItemResult,
  onRemoveItem: (SyncItem) -> Unit,
) {
  Box(
    modifier = Modifier
      .weight(1f)
      .fillMaxWidth()
  ) {

    val lazyListState = rememberLazyListState()
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      state = lazyListState,
    ) {
      item {
        AddItemRow(onAddItem = onAddItem)
      }

      items(settings.syncItems) { syncItem ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Folder name
          DenseOutlinedTextField(
            modifier = Modifier.width(192.dp),
            value = syncItem.folderName,
            readOnly = true,
            singleLine = true,
            onValueChange = { }
          )

          Spacer(Modifier.width(8.dp))

          // Remote bookmarks URL
          DenseOutlinedTextField(
            modifier = Modifier.width(320.dp),
            value = syncItem.remoteBookmarksUrl,
            readOnly = true,
            singleLine = true,
            onValueChange = { }
          )

          Spacer(Modifier.width(8.dp))

          // Sync indication
          Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center,
          ) {
            AnimatedContent(settings.syncState.folderSyncStates[syncItem.folderName]) { folderSyncState ->
              when (folderSyncState) {
                FolderSyncState.Syncing -> {
                  Rotate {
                    Icon(
                      modifier = Modifier.padding(horizontal = 8.dp),
                      painter = painterResource(Res.drawable.sync_24px),
                      tint = MaterialTheme.colorScheme.primary,
                      contentDescription = null
                    )
                  }
                }

                FolderSyncState.Success -> {
                  Icon(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(Res.drawable.check_24px),
                    tint = successColor,
                    contentDescription = null
                  )
                }

                is FolderSyncState.Error -> {
                  Icon(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(Res.drawable.warning_24px),
                    tint = warningColor,
                    contentDescription = null
                  )
                }

                else -> {}
              }
            }
          }

          // Should 'logically' be here, but it looks better without it ¯\_(ツ)_/¯ IANAD
//          Spacer(Modifier.width(8.dp))

          // Remove button
          IconButton(
            onClick = {
              onRemoveItem(syncItem)
            },
          ) {
            Icon(
              painter = painterResource(Res.drawable.delete_24px),
              contentDescription = "Remove",
            )
          }
        }
      }
    }

    VerticalScrollbar(
      modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
      adapter = rememberScrollbarAdapter(
        scrollState = lazyListState
      )
    )
  }
}

@Composable
private fun AddItemRow(onAddItem: suspend (folderName: String, remoteBookmarksUrl: String) -> AddItemResult) {
  Row(
    verticalAlignment = Alignment.Top,
  ) {
    // Folder name
    var folderName by remember { mutableStateOf("") }
    var folderNameErrorText: String? by remember { mutableStateOf(null) }
    DenseOutlinedTextField(
      modifier = Modifier.width(192.dp),
      placeholder = { Text("Folder name") },
      value = folderName,
      singleLine = true,
      onValueChange = {
        folderName = it
        folderNameErrorText = null
      },
      isError = folderNameErrorText != null,
      supportingText = {
        folderNameErrorText?.let { Text(it) }
      },
    )

    Spacer(Modifier.width(8.dp))

    // Bookmark URL
    var remoteBookmarksUrl by remember { mutableStateOf("") }
    var remoteBookmarksUrlErrorText: String? by remember { mutableStateOf(null) }
    DenseOutlinedTextField(
      modifier = Modifier.width(320.dp),
      placeholder = { Text("URL (RSS, Atom, JSON, HTML)") },
      value = remoteBookmarksUrl,
      singleLine = true,
      onValueChange = {
        remoteBookmarksUrl = it
        remoteBookmarksUrlErrorText = null
      },
      isError = remoteBookmarksUrlErrorText != null,
      supportingText = {
        remoteBookmarksUrlErrorText?.let { Text(it) }
      },
    )

    Spacer(Modifier.width(8.dp))

    // Add button
    val coroutineScope = rememberCoroutineScope()
    val isAddButtonEnabled = folderName.isNotBlank() && folderNameErrorText == null &&
      remoteBookmarksUrl.isNotBlank() && remoteBookmarksUrlErrorText == null
    OutlinedButton(
      modifier = Modifier
        .height(48.dp)
        .width(88.dp),
      contentPadding = PaddingValues(0.dp),
      enabled = isAddButtonEnabled,
      onClick = {
        coroutineScope.launch {
          val result = onAddItem(folderName, remoteBookmarksUrl)
          folderNameErrorText = result.folderNameErrorText
          remoteBookmarksUrlErrorText = result.remoteBookmarksUrlErrorText
          if (result.folderNameErrorText == null && result.remoteBookmarksUrlErrorText == null) {
            folderName = ""
            remoteBookmarksUrl = ""
          }
        }
      },
    ) {
      Text("Add")
    }
  }
}
