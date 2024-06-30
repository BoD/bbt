plugins {
  kotlin("multiplatform")
}

repositories {
  mavenCentral()
}

kotlin {
  js {
    browser()
    binaries.executable()
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(KotlinX.coroutines.core)
      }
    }
  }
}
