plugins {
  kotlin("multiplatform")
}

repositories {
  mavenCentral()
}

// Generate a Version.kt file with a constant for the version name
tasks.register("generateVersionKt") {
  val outputDir = layout.buildDirectory.dir("generated/source/kotlin").get().asFile
  outputs.dir(outputDir)
  doFirst {
    val outputWithPackageDir = File(outputDir, "org/jraf/bbt/shared").apply { mkdirs() }
    File(outputWithPackageDir, "Version.kt").writeText(
      """
        package org.jraf.bbt.shared
  
        const val VERSION = "v${rootProject.version}"
      """.trimIndent()
    )
  }
}

kotlin {
  js {
    browser()
    binaries.executable()
  }
  sourceSets {
    val commonMain by getting {
      kotlin.srcDir(tasks.getByName("generateVersionKt").outputs.files)

      dependencies {
        api(KotlinX.coroutines.core)
      }
    }
  }
}
