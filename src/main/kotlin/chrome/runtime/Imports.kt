@file:JsQualifier("chrome.runtime")

package chrome.runtime

external val onInstalled: OnInstalled

external interface OnInstalled {
    fun addListener(block: () -> Unit)
}