plugins {
  kotlin("multiplatform").apply(false)
  kotlin("plugin.js-plain-objects").apply(false)
  id("org.jetbrains.compose").apply(false)
  kotlin("plugin.compose").apply(false)
}

group = "org.jraf"
version = "1.6.2"

tasks.register<Sync>("devDist") {
  listOf(
    ":serviceworker",
    ":popup",
    ":offscreen",
  )
    .map {
      project(it)
    }
    .forEach {
      dependsOn("${it.name}:jsBrowserDevelopmentExecutableDistribution")
      from(it.layout.buildDirectory.dir("dist/js/developmentExecutable"))
    }
  into(layout.buildDirectory.dir("devDist"))
}

tasks.register<Zip>("dist") {
  listOf(
    ":serviceworker",
    ":popup",
    ":offscreen",
  )
    .map {
      project(it)
    }
    .forEach {
      dependsOn("${it.name}:jsBrowserDistribution")
      from(it.layout.buildDirectory.dir("dist/js/productionExecutable"))
    }
  destinationDirectory.set(layout.buildDirectory.dir("dist"))
}


// Run `./gradlew refreshVersions` to update dependencies
// Run `./gradlew devDist` for tests (result is in build/devDist)
// Run `./gradlew dist` to release (result is in build/dist/bbt-x.y.z.zip)
