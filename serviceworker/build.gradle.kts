plugins {
  kotlin("multiplatform")
  kotlin("plugin.js-plain-objects")
}

// Replace the version in the manifest with the project's version
val replaceVersionInManifestTask = tasks.register("replaceVersionInManifest") {
  val manifestFile = layout.projectDirectory.dir("src/manifest.json").asFile
  val outputDir = layout.buildDirectory.dir("generated/resources").get().asFile
  outputs.dir(outputDir)
  doFirst {
    var contents = manifestFile.readText()
      .replace("{VERSION}", rootProject.version.toString())
    File(outputDir, "manifest.json").writeText(contents)
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
    compilerOptions {
      target.set("es2015")
      optIn.addAll("kotlinx.coroutines.DelicateCoroutinesApi", "kotlinx.serialization.ExperimentalSerializationApi")
    }
  }

  sourceSets {
    commonMain {
      resources.srcDir(replaceVersionInManifestTask)

      dependencies {
        implementation(project(":shared"))
      }
    }
  }
}
