plugins {
  kotlin("multiplatform")
  kotlin("plugin.js-plain-objects")
}

repositories {
  mavenCentral()
}

// Replace the version in the manifest with the version defined in gradle
tasks.register("replaceVersionInManifest") {
  val manifestFile = layout.projectDirectory.dir("src/jsMain/resources/manifest.json").asFile
  outputs.file(manifestFile)
  doFirst {
    var contents = manifestFile.readText()
    contents = contents.replace(Regex(""""version": "(.*)""""), """"version": "${rootProject.version}"""")
    manifestFile.writeText(contents)
  }
}

// Make jsProcessResources depend on it
project.afterEvaluate {
  tasks.getByName("jsProcessResources").dependsOn("replaceVersionInManifest")
}

kotlin {
  js {
    browser()
    binaries.executable()
  }
  sourceSets {

    val commonMain by getting {
      dependencies {
        implementation(project(":shared"))
      }
    }
  }
}
