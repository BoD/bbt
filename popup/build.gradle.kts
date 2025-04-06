plugins {
  kotlin("multiplatform")
  id("org.jetbrains.compose")
  kotlin("plugin.compose")
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
      dependencies {
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(compose.components.resources)

        implementation(project(":shared"))
      }
    }
  }
}
