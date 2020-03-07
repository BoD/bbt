'use strict';

chrome.runtime.onInstalled.addListener(
    async function() {
        console.log("Hello, World!");
        var bbtFolder = await findFolder("bbt");
        if (bbtFolder == null) {
            console.log("Could not find bbt folder")
            return;
        }
        await emptyFolder(bbtFolder);
        var bookmarks = await fetchRemoteBookmarks();
        if (bookmarks == null) {
            console.log("Could not fetch remote bookmarks")
            return;
        }
        console.log("Remote bookmarks: %O", bookmarks);
        populateBookmarks(bbtFolder, bookmarks.bookmarks);
    }
);

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

function fetchRemoteBookmarks() {
    console.log("Fetching bookmarks from remote");
    return fetch(
        "https://jraf.org/static/tmp/bbt/bookmarks.json", {
            "cache": "no-cache"
        }
    ).then(
        (response) => {
            return response.json();
        }
    );
}

function populateBookmarks(folder, bookmarks) {
    bookmarks.forEach(
        (bookmark, i) => {
            if (bookmark.url != null) {
                // Bookmark
                chrome.bookmarks.create(
                    {
                        "parentId": folder.id,
                        "title": bookmark.title,
                        "url": bookmark.url
                    }
                );
            } else {
                // Folder
                chrome.bookmarks.create(
                    {
                        "parentId": folder.id,
                        "title": bookmark.title,
                    },
                    function(createdFolder) {
                        // Recurse
                        populateBookmarks(createdFolder, bookmark.bookmarks);
                    }
                );
            }
        }
    );
}
