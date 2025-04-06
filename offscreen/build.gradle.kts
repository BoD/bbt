plugins {
  kotlin("multiplatform")
}

kotlin {
  js {
    browser()
    binaries.executable()
    compilerOptions {
      target.set("es2015")
    }
  }
  sourceSets.commonMain {
    dependencies {
      implementation(project(":shared"))
    }
  }
}
