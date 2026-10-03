plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.kotlin.compose)
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
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(libs.compose.components.resources)

        implementation(project(":shared"))
      }
    }
  }
}
