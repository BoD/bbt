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

@file:OptIn(ExperimentalMaterial3Api::class)

package org.jraf.bbt.popup.components

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenseOutlinedTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = LocalTextStyle.current,
  label: @Composable (() -> Unit)? = null,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  prefix: @Composable (() -> Unit)? = null,
  suffix: @Composable (() -> Unit)? = null,
  supportingText: @Composable (() -> Unit)? = null,
  isError: Boolean = false,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = false,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  minLines: Int = 1,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  shape: Shape = OutlinedTextFieldDefaults.shape,
  colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
  // If color is not provided via the text style, use content color as a default
  val textColor = textStyle.color.takeOrElse {
    colors.textColor(enabled, isError, interactionSource).value
  }
  val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

  Column {
    BasicTextField(
      value = value,
      modifier = if (label != null) {
        modifier
          // Merge semantics at the beginning of the modifier chain to ensure padding is
          // considered part of the text field.
          .semantics(mergeDescendants = true) {}
          .padding(top = OutlinedTextFieldTopPadding)
      } else {
        modifier
      }
        .defaultErrorSemantics(isError, "Error")
        .height(48.dp),
      onValueChange = onValueChange,
      enabled = enabled,
      readOnly = readOnly,
      textStyle = mergedTextStyle,
      cursorBrush = SolidColor(colors.cursorColor(isError).value),
      visualTransformation = visualTransformation,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      interactionSource = interactionSource,
      singleLine = singleLine,
      maxLines = maxLines,
      minLines = minLines,
      decorationBox = @Composable { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
          value = value,
          visualTransformation = visualTransformation,
          innerTextField = innerTextField,
          placeholder = placeholder,
          label = label,
          leadingIcon = leadingIcon,
          trailingIcon = trailingIcon,
          prefix = prefix,
          suffix = suffix,
          // We handle the supporting text ourselves
          supportingText = null,
          singleLine = singleLine,
          enabled = enabled,
          isError = isError,
          interactionSource = interactionSource,
          colors = colors,
          contentPadding = OutlinedTextFieldDefaults.contentPadding(
            start = 8.dp,
            top = 0.dp,
            end = 8.dp,
            bottom = 0.dp,
          ),
          container = {
            OutlinedTextFieldDefaults.ContainerBox(
              enabled,
              isError,
              interactionSource,
              colors,
              shape
            )
          }
        )
      }
    )

    if (supportingText != null) {
      Box(
        Modifier
          .height(16.dp)
          .padding(horizontal = 8.dp)
      ) {
        val mergedStyle = LocalTextStyle.current.merge(MaterialTheme.typography.labelSmall)
        CompositionLocalProvider(
          LocalContentColor provides colors.supportingTextColor(enabled, isError, interactionSource).value,
          LocalTextStyle provides mergedStyle,
        ) {
          supportingText()
        }
      }
    }
  }
}

@Composable
private fun TextFieldColors.textColor(
  enabled: Boolean,
  isError: Boolean,
  interactionSource: InteractionSource,
): State<Color> {
  val focused by interactionSource.collectIsFocusedAsState()

  val targetValue = when {
    !enabled -> disabledTextColor
    isError -> errorTextColor
    focused -> focusedTextColor
    else -> unfocusedTextColor
  }
  return rememberUpdatedState(targetValue)
}

@Composable
private fun TextFieldColors.cursorColor(isError: Boolean): State<Color> {
  return rememberUpdatedState(if (isError) errorCursorColor else cursorColor)
}

@Composable
private fun TextFieldColors.supportingTextColor(
  enabled: Boolean,
  isError: Boolean,
  interactionSource: InteractionSource,
): State<Color> {
  val focused by interactionSource.collectIsFocusedAsState()

  return rememberUpdatedState(
    when {
      !enabled -> disabledSupportingTextColor
      isError -> errorSupportingTextColor
      focused -> focusedSupportingTextColor
      else -> unfocusedSupportingTextColor
    }
  )
}

private fun Modifier.defaultErrorSemantics(
  isError: Boolean,
  defaultErrorMessage: String,
): Modifier = if (isError) semantics { error(defaultErrorMessage) } else this

private val OutlinedTextFieldTopPadding = 8.dp
