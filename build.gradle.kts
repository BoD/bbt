import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("org.jetbrains.kotlin.js") version "1.3.71"
    id("com.github.ben-manes.versions") version "0.28.0"
}

group = "org.jraf"
version = "1.1.0"

repositories {
    mavenCentral()
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = "6.3"
    }
}

tasks.withType<Kotlin2JsCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.5")

}

kotlin.target.browser {}

tasks.register<Zip>("dist") {
    dependsOn(":browserProductionWebpack")
    from(layout.buildDirectory.dir("distributions"))
    include("*", "*/*")
    exclude("*.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Run `./gradlew browserDevelopmentWebpack` for tests and `./gradlew dist` to release