@file:JsQualifier("chrome.alarms")

package chrome.alarms

external fun create(alarmName: String, alarmOptions: AlarmOptions)

external interface AlarmOptions {
    var periodInMinutes: Int
}

external fun clearAll()

external val onAlarm: OnAlarm

external interface OnAlarm {
    fun addListener(block: () -> Unit)
}