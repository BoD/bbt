'use strict';

async function onSyncEnabledChanged(event) {
    var syncEnabled = event.target.checked;
    if (syncEnabled) {
        chrome.browserAction.setBadgeText({text: ""});
    } else {
        chrome.browserAction.setBadgeText({text: "OFF"});
        chrome.browserAction.setBadgeBackgroundColor({color: "#808080"});
    }
    var settings = await getSettings();
    settings.syncEnabled = syncEnabled;
    chrome.storage.sync.set({settings: settings});
}

/*
function clearAlarm() {
  chrome.browserAction.setBadgeText({text: ''});
  chrome.alarms.clearAll();
  window.close();
}
*/

function getSettings() {
    return new Promise(
        resolve => {
            chrome.storage.sync.get("settings",
                function(items) {
                    var settings = items.settings;

                    // Default values
                    if (settings.syncEnabled === undefined) settings.syncEnabled = true;
                    if (settings.syncItems === undefined) settings.syncItems = {"Sample": "https://jraf.org/static/tmp/bbt/bookmarks.json"};

                    resolve(settings)
                }
            );
        }
    )
}

async function populateTable() {
    var settings = await getSettings();
    console.log(settings);

    var syncEnabledHtml = settings.syncEnabled ? "checked" : "";

    var tableHtml = `
    <tr>
        <td colspan="3">
            <input class="checkbox" ${syncEnabledHtml} type="checkbox" id="chkSyncEnabled" name="chkSyncEnabled">
            <label for="chkSyncEnabled">Enabled</label>
        </td>
    </tr>
    `;

    Object.keys(settings.syncItems).forEach(
        (key, i) => {
            var value = settings.syncItems[key];
            tableHtml += `
            <tr>
                <td><input class="input" type="text" placeholder="Folder name" value="${key}" readonly="true"></td>
                <td><input class="input url" type="text" placeholder="Remote bookmarks URL" value="${value}" readonly="true"></td>
                <td><button type="button" id="btnRemove_${key}" value="${key}">Remove</button>
            </tr>
            `;
        }
    );

    tableHtml += `
    <tr>
        <td><input class="input" type="text" placeholder="Folder name"></td>
        <td><input class="input url" type="text" placeholder="Remote bookmarks URL"></td>
        <td><button type="button" onclick="onAddClick();">Add</button>
    </tr>
    `;

    document.getElementById("table").innerHTML = tableHtml;

    Object.keys(settings.syncItems).forEach(
        (key, i) => {
            document.getElementById("btnRemove_" + key).addEventListener("click", onRemoveClicked);
        }
    );

    document.getElementById("chkSyncEnabled").addEventListener("change", onSyncEnabledChanged);
}

function onRemoveClicked(event) {
    removeSyncItem(event.target.getAttribute("value"));
}

async function removeSyncItem(folderName) {
    var settings = await getSettings();
    delete settings.syncItems[folderName];
    chrome.storage.sync.set(
        {
            settings: settings
        }, function() {
            populateTable();
        }
    );
}

populateTable();
