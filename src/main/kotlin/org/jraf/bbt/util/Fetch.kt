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

package org.jraf.bbt.util

import kotlinx.coroutines.await
import org.w3c.fetch.NO_CACHE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit
import kotlin.browser.window

suspend fun fetchJson(url: String): dynamic {
    val res = try {
        window.fetch(url, object : RequestInit {
            override var cache: RequestCache? = RequestCache.NO_CACHE
        }).await()
    } catch (t: Throwable) {
        throw FetchException("Could not fetch from $url", cause = t)
    }
    @Suppress("UNCHECKED_CAST")
    return if (res.ok) {
        try {
            res.json().await()
        } catch (t: Throwable) {
            throw FetchException("Could not convert to JSON", res.status, t)
        }
    } else {
        throw FetchException(res.statusText, res.status)
    }
}

class FetchException(message: String, val status: Short? = null, cause: Throwable? = null) : Exception(message, cause)