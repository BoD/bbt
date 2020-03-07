'use strict';

async function onSettingsChanged() {
    var settings = await getSettings();
    if (settings.syncEnabled) {
        syncFolders();
        startScheduling();
        chrome.browserAction.setBadgeText({text: ""});
    } else {
        stopScheduling();
        chrome.browserAction.setBadgeText({text: "OFF"});
        chrome.browserAction.setBadgeBackgroundColor({color: "#808080"});
    }
}

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

function startScheduling() {
    chrome.alarms.create(
        "BoD's Bookmark Tool", {
            "periodInMinutes": 5
        }
    );
}


function stopScheduling() {
    chrome.alarms.clearAll();
    window.close();
}

async function syncFolders() {
    var settings = await getSettings();
    Object.keys(settings.syncItems).forEach(
        (folderName, i) => {
            var remoteBookmarksUrl = settings.syncItems[folderName];
            syncFolder(folderName, remoteBookmarksUrl);
        }
    )
}

async function syncFolder(folderName, remoteBookmarksUrl) {
    console.log("Syncing " + folderName + " to remoteBookmarksUrl...")
    var folder = await findFolder(folderName);
    if (folder == null) {
        console.log("Could not find folder " + folderName)
        return;
    }
    await emptyFolder(folder);
    var bookmarks = await fetchRemoteBookmarks(remoteBookmarksUrl);
    if (bookmarks == null) {
        console.log("Could not fetch remote bookmarks")
        return;
    }
    console.log("Remote bookmarks: %O", bookmarks);
    await populateBookmarks(folder, bookmarks.bookmarks);
    console.log("Sync finished")
}

function findFolder(folderName) {
    return new Promise(
        resolve => {
            chrome.bookmarks.search({
                    "title": folderName,
                    "url": null
                },
                function(bookmarkTreeNodes) {
                    bookmarkTreeNodes.forEach(
                        (bookmarkTreeNode, i) => {
                            if (bookmarkTreeNode.url == null) {
                                resolve(bookmarkTreeNode);
                                return;
                            }
                        }
                    );
                    resolve(null);
                }
            );
        }
    );
}

function emptyFolder(folder) {
    console.log("Emptying folder %O", folder);
    return new Promise(
        resolve => {
            chrome.bookmarks.getChildren(folder.id,
                function (children) {
                    var childCount = children.length
                    if (childCount == 0) resolve();
                    children.forEach(
                        (child, i) => {
                            chrome.bookmarks.removeTree(child.id,
                                function() {
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
    console.log("Fetching bookmarks from remote");
    return fetch(
        remoteBookmarksUrl, {
            "cache": "no-cache"
        }
    ).then(
        (response) => {
            return response.json();
        }
    );
}

function populateBookmarks(folder, bookmarks) {
    return new Promise(
        resolve => {
            var childCount = bookmarks.length
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
                            function() {
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
                            async function(createdFolder) {
                                // Recurse
                                await populateBookmarks(createdFolder, bookmark.bookmarks);
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
