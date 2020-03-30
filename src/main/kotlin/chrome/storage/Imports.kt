@file:JsQualifier("chrome.storage")

package chrome.storage

external val sync: Sync

external interface Sync {
    fun get(item: String, onResult: (dynamic) -> Unit)
}