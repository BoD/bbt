plugins {
  kotlin("multiplatform")
  kotlin("plugin.js-plain-objects")
  kotlin("plugin.serialization")
}

// Generate a Version.kt file with a constant for the version name
val generateVersionKtTask = tasks.register("generateVersionKt") {
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
    compilerOptions {
      target.set("es2015")
      optIn.addAll("kotlinx.coroutines.DelicateCoroutinesApi", "kotlinx.serialization.ExperimentalSerializationApi")
    }
  }

  sourceSets {
    commonMain {
      kotlin.srcDir(generateVersionKtTask)

      dependencies {
        api(KotlinX.coroutines.core)
        api(KotlinX.serialization.json)
      }
    }
  }
}
