# BoD's Bookmark Tool

This very small and simple extension allows you to synchronize any bookmark folder with a remote json file.  Ideal to share a bunch of bookmarks with your team at work, or your loved ones!

## Setup

The extension is available at the Chrome Web Store (publication pending!).

## Remote file format

The remote file is a very simple json file of this form:

```json
{
	"version": 1,
	"bookmarks": [
		{
			"title": "Bookmark 1",
			"url": "https://JRAF.org"
		},
		{
			"title": "Folder 1",
			"bookmarks": [
				{
					"title": "Bookmark 1 in folder 1",
					"url": "https://google.com"
				},
				{
                    "title": "Bookmark 2 in folder 1",
                    "url": "https://microsoft.com"
                }
			]
		}
	]
}
```

## License

Copyright 2020-present Benoit "BoD" Lubek <BoD@JRAF.org>

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.
