package org.jraf.bbt.util

@Suppress("NOTHING_TO_INLINE")
inline fun <T> jsObject(noinline init: T.() -> Unit): T = (js("{}") as T).apply(init)