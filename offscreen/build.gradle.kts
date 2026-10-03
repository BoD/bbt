plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
  js {
    browser {
      commonWebpackConfig {
        // Extension CSP forbids eval-based source maps.
        // See https://stackoverflow.com/questions/48047150/chrome-extension-compiled-by-webpack-throws-unsafe-eval-error
        devtool = "cheap-module-source-map"
      }
    }
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
