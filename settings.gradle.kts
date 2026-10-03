pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    google()
    mavenCentral()
  }
}

plugins {
  // See https://splitties.github.io/refreshVersions/
  id("de.fayard.refreshVersions").version("0.60.6")
}

rootProject.name = "bbt"

include(
  "shared",
  "popup",
  "offscreen",
  "serviceworker",
)
