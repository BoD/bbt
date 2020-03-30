@file:JsQualifier("chrome.browserAction")

package chrome.browserAction

external fun setBadgeText(badgeText: BadgeText)

external interface BadgeText {
    var text: String
}

external fun setBadgeBackgroundColor(badgeBackgroundColor: BadgeBackgroundColor)

external interface BadgeBackgroundColor {
    var color: String
}