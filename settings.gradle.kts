plugins {
  // See https://splitties.github.io/refreshVersions/
  id("de.fayard.refreshVersions") version "0.60.5"
}

rootProject.name = "bbt"

include(
  "shared",
  "popup",
  "offscreen",
  "serviceworker",
)
