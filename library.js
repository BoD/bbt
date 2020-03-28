'use strict';

function log() {
    if (typeof arguments[0] === 'string') {
        arguments[0] = new Date().toISOString() + " - " + arguments[0];
    }
    chrome.extension.getBackgroundPage().console.log.apply(this, arguments);
}

async function onSettingsChanged() {
    var settings = await getSettings();
    if (settings.syncEnabled) {
        log("Enable sync");
        syncFolders();
        startScheduling();
        chrome.browserAction.setBadgeText({ text: "" });
    } else {
        log("Disable sync");
        stopScheduling();
        chrome.browserAction.setBadgeText({ text: "OFF" });
        chrome.browserAction.setBadgeBackgroundColor({ color: "#808080" });
    }
}

function getSettings() {
    return new Promise(
        resolve => {
            chrome.storage.sync.get("settings",
                function (items) {
                    var settings = items.settings;

                    // Default values
                    if (settings === undefined) settings = {};
                    if (settings.syncEnabled === undefined) settings.syncEnabled = true;
                    if (settings.syncItems === undefined) settings.syncItems = { "Sample": "https://jraf.org/static/tmp/bbt/bookmarks.json" };

                    resolve(settings);
                }
            );
        }
    )
}

function startScheduling() {
    chrome.alarms.create(
        "BoD's Bookmark Tool", {
        "periodInMinutes": 1
    });
}


function stopScheduling() {
    chrome.alarms.clearAll();
}

async function syncFolders() {
    log("Start syncing...");
    var settings = await getSettings();
    for (const folderName of Object.keys(settings.syncItems)) {
        var remoteBookmarksUrl = settings.syncItems[folderName];
        const ok = await syncFolder(folderName, remoteBookmarksUrl);
        if (ok) {
            log("Finished sync of '%s' successfully", folderName);
        } else {
            log("Finished sync of '%s' with error", folderName);
        }
    }
    log("Sync finished");
    log("");
}

async function syncFolder(folderName, remoteBookmarksUrl) {
    log("Syncing '%s' to %s", folderName, remoteBookmarksUrl);
    var folder = await findFolder(folderName);
    if (folder == null) {
        log("Could not find folder '%s'", folderName);
        return false;
    }
    var bookmarks = await fetchRemoteBookmarks(remoteBookmarksUrl);
    if (bookmarks == null) {
        log("Could not fetch remote bookmarks from %s for folder '%s'", remoteBookmarksUrl, folderName);
        return false;
    }
    log("Fetched object: %O", bookmarks);
    const bookmarkObject = bookmarks.bookmarks;
    if (bookmarkObject == null) {
        log("Fetched object doesn't seem to be in a compatible `bookmarks` format");
        return false;
    }
    await emptyFolder(folder);
    log("Populating folder %s", folder.title);
    await populateFolder(folder, bookmarkObject);
    return true;
}

function findFolder(folderName) {
    return new Promise(
        resolve => {
            chrome.bookmarks.search({}, function (bookmarkTreeNodes) {
                bookmarkTreeNodes.forEach(bookmarkTreeNode => {
                    if (bookmarkTreeNode.title.toUpperCase() === folderName.toUpperCase() && bookmarkTreeNode.url == null) {
                        resolve(bookmarkTreeNode);
                        return;
                    }
                });
                resolve(null);
            });
        }
    );
}

function emptyFolder(folder) {
    log("Emptying folder %s", folder.title);
    return new Promise(
        resolve => {
            chrome.bookmarks.getChildren(folder.id,
                function (children) {
                    var childCount = children.length;
                    if (childCount == 0) resolve();
                    children.forEach(
                        (child, i) => {
                            chrome.bookmarks.removeTree(child.id,
                                function () {
                                    if (i == childCount - 1) {
                                        resolve();
                                    }
                                }
                            );
                        }
                    );
                }
            );
        }
    );
}

function fetchRemoteBookmarks(remoteBookmarksUrl) {
    log("Fetching bookmarks from remote %s", remoteBookmarksUrl);
    return fetch(
        remoteBookmarksUrl, {
        "cache": "no-cache"
    }).then((response) => {
        return response.json();
    }).catch(
        function (error) {
            log("Could not fetch from remote %s: %O", remoteBookmarksUrl, error);
            return null;
        }
    );
}

function populateFolder(folder, bookmarks) {
    return new Promise(
        resolve => {
            var childCount = bookmarks.length;
            if (childCount == 0) resolve();
            bookmarks.forEach(
                (bookmark, i) => {
                    if (bookmark.url != null) {
                        // Bookmark
                        chrome.bookmarks.create(
                            {
                                "parentId": folder.id,
                                "title": bookmark.title,
                                "url": bookmark.url
                            },
                            function () {
                                if (i == childCount - 1) {
                                    resolve();
                                }
                            }
                        );
                    } else {
                        // Folder
                        chrome.bookmarks.create(
                            {
                                "parentId": folder.id,
                                "title": bookmark.title,
                            },
                            async function (createdFolder) {
                                // Recurse
                                await populateFolder(createdFolder, bookmark.bookmarks);
                                if (i == childCount - 1) {
                                    resolve();
                                }
                            }
                        );
                    }
                }
            );
        }
    );
}
