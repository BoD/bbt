import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("js") version "1.3.71"
    id("com.github.ben-manes.versions") version "0.28.0"
}

group = "org.jraf"
version = "1.2.1"

repositories {
    mavenCentral()
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = "6.3"
    }
}

// Generate a Version.kt file with a constant for the version name
tasks.register("generateVersionKt") {
    val outputDir = layout.buildDirectory.dir("generated/source/kotlin").get().asFile
    outputs.dir(outputDir)
    doFirst {
        val outputWithPackageDir = File(outputDir, "org/jraf/bbt").apply { mkdirs() }
        File(outputWithPackageDir, "Version.kt").writeText(
            """
                package org.jraf.bbt

                const val VERSION = "v${project.version}"
            """.trimIndent()
        )
    }
}

// Replace the version in the manifest with the version defined in gradle
tasks.register("replaceVersionInManifest") {
    doFirst {
        val manifestFile = layout.projectDirectory.dir("src/main/resources/manifest.json").asFile
        var contents = manifestFile.readText()
        contents = contents.replace(Regex(""""version": "(.*)""""), """"version": "${project.version}"""")
        manifestFile.writeText(contents)
    }
}

tasks.withType<Kotlin2JsCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    dependsOn(":generateVersionKt")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.5")

}

kotlin {
    target.browser {}
    sourceSets["main"].kotlin.srcDir(tasks.getByName("generateVersionKt").outputs.files)
}

tasks.register<Zip>("dist") {
    dependsOn(":browserProductionWebpack")
    dependsOn(":replaceVersionInManifest")
    from(layout.buildDirectory.dir("distributions"))
    include("*", "*/*")
    exclude("*.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Run `./gradlew browserDevelopmentWebpack` for tests and `./gradlew dist` to release