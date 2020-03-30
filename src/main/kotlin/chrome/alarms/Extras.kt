package chrome.alarms

import org.jraf.bbt.util.jsObject

@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun AlarmOptions(periodInMinutes: Int) = jsObject<AlarmOptions> { this.periodInMinutes = periodInMinutes }

