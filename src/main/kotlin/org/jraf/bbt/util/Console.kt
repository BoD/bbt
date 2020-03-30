package org.jraf.bbt.util

import chrome.extension.getBackgroundPage
import kotlin.js.Date

fun log(format:String, vararg params:Any) {
    val date = Date()
    getBackgroundPage().console.log("${date.toLocaleDateString()} ${date.toLocaleTimeString()} - $format", *params)
}