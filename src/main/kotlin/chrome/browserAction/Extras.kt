package chrome.browserAction

import org.jraf.bbt.util.jsObject

@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun BadgeText(text: String) = jsObject<BadgeText> { this.text = text }

@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun BadgeBackgroundColor(color: String) = jsObject<BadgeBackgroundColor> { this.color = color }