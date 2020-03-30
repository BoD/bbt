@file:JsQualifier("chrome.extension")

package chrome.extension

import kotlin.js.Console

external fun getBackgroundPage(): BackgroundPage

external interface BackgroundPage {
    val console: Console
}