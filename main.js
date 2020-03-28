'use strict';

chrome.runtime.onInstalled.addListener(
    function () {
        log("BoD's Bookmark Tool v1.0.2");

        chrome.alarms.onAlarm.addListener(
            function (alarm) {
                log("Alarm triggered");
                syncFolders();
            }
        );

        onSettingsChanged();
    }
);
