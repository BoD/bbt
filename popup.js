'use strict';

async function populateTable() {
    var settings = await getSettings();
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
        <td><input class="input" type="text" placeholder="Folder name" id="inputFolderName"></td>
        <td><input class="input url" type="text" placeholder="Remote bookmarks URL" id="inputUrl"></td>
        <td><button type="button" id="btnAdd">Add</button>
    </tr>
    `;

    document.getElementById("table").innerHTML = tableHtml;

    Object.keys(settings.syncItems).forEach(
        (key, i) => {
            document.getElementById("btnRemove_" + key).addEventListener("click", onRemoveClicked);
        }
    );
    document.getElementById("chkSyncEnabled").addEventListener("change", onSyncEnabledChanged);
    document.getElementById("btnAdd").addEventListener("click", onAddClicked);
}

async function onSyncEnabledChanged(event) {
    var syncEnabled = event.target.checked;
    var settings = await getSettings();
    settings.syncEnabled = syncEnabled;
    chrome.storage.sync.set(
        {
            settings: settings
        }, function() {
            onSettingsChanged();
        }
    );
}

async function onRemoveClicked(event) {
    var folderName = event.target.getAttribute("value");
    var settings = await getSettings();
    delete settings.syncItems[folderName];
    chrome.storage.sync.set(
        {
            settings: settings
        }, function() {
            populateTable();
            onSettingsChanged();
        }
    );
}

async function onAddClicked(event) {
    var folderName = document.getElementById("inputFolderName").value;
    var url = document.getElementById("inputUrl").value;
    var settings = await getSettings();
    settings.syncItems[folderName] = url;
    chrome.storage.sync.set(
        {
            settings: settings
        }, function() {
            populateTable();
            onSettingsChanged();
        }
    );
}

populateTable();
