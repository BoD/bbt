plugins {
  kotlin("multiplatform")
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
        implementation(project(":shared"))
      }
    }
  }
}
