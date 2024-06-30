plugins {
  kotlin("multiplatform").apply(false)
  kotlin("plugin.js-plain-objects").apply(false)
}

group = "org.jraf"
version = "1.5.0"

//tasks.register<Zip>("devDist") {
//  subprojects.forEach {
//    dependsOn("${it.name}:jsBrowserDevelopmentExecutableDistribution")
//    from(it.layout.buildDirectory.dir("dist/js/productionExecutable"))
//  }
//  include("*", "*/*")
//  exclude("*.zip")
//  destinationDirectory.set(layout.buildDirectory.dir("devDist"))
//}


tasks.register<Sync>("devDist") {
  listOf(":serviceworker", ":offscreen")
    .map {
      project(it)
    }
    .forEach {
      dependsOn("${it.name}:jsBrowserDevelopmentExecutableDistribution")
      from(it.layout.buildDirectory.dir("dist/js/developmentExecutable"))

    }
  into(layout.buildDirectory.dir("devDist"))
}


// Run `./gradlew refreshVersions` to update dependencies
// Run `./gradlew jsBrowserDevelopmentExecutableDistribution` for tests (result is in build/dist/js/developmentExecutable)
// Run `./gradlew dist` to release (result is in build/js/packages/bbt/kotlin/bbt-x.y.z.zip)
