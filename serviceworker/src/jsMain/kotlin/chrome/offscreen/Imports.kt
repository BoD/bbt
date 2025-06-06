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

// XXX This API is not available in Firefox, and using @JsQualifier generates a reference in an 'imports' section, resulting in
// `chrome.offscreen is undefined`.
// Using @JsName instead as a workaround - see https://slack-chats.kotlinlang.org/t/27344934/x
//@file:JsQualifier("chrome.offscreen")

package chrome.offscreen

import kotlin.js.Promise

@JsName("chrome.offscreen.createDocument")
external fun createDocument(parameters: CreateParameters): Promise<Unit>
