plugins {
    kotlin("multiplatform")
}

group = "org.jraf"
version = "1.5.0"

repositories {
    mavenCentral()
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
    val manifestFile = layout.projectDirectory.dir("src/jsMain/resources/manifest.json").asFile
    outputs.file(manifestFile)
    doFirst {
        var contents = manifestFile.readText()
        contents = contents.replace(Regex(""""version": "(.*)""""), """"version": "${project.version}"""")
        manifestFile.writeText(contents)
    }
}

// Make jsProcessResources depend on it
project.afterEvaluate {
    tasks.getByName("jsProcessResources").dependsOn("replaceVersionInManifest")
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
                implementation(KotlinX.coroutines.core)
            }
        }
    }
}

tasks.register<Zip>("dist") {
    dependsOn("jsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/js/productionExecutable"))
    include("*", "*/*")
    exclude("*.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist/js/productionExecutable"))
}

// Run `./gradlew refreshVersions` to update dependencies
// Run `./gradlew jsBrowserDevelopmentExecutableDistribution` for tests (result is in build/dist/js/developmentExecutable)
// Run `./gradlew dist` to release (result is in build/js/packages/bbt/kotlin/bbt-x.y.z.zip)
