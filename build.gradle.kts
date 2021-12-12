import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("js")
}

group = "org.jraf"
version = "1.4.0"

repositories {
    mavenCentral()
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = "7.3.1"
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
    val manifestFile = layout.projectDirectory.dir("src/main/resources/manifest.json").asFile
    outputs.file(manifestFile)
    doFirst {
        var contents = manifestFile.readText()
        contents = contents.replace(Regex(""""version": "(.*)""""), """"version": "${project.version}"""")
        manifestFile.writeText(contents)
    }
}
// Make browserDevelopmentWebpack and browserProductionWebpack depend on it
project.afterEvaluate {
    tasks.getByName("browserDevelopmentWebpack").dependsOn("replaceVersionInManifest")
    tasks.getByName("browserProductionWebpack").dependsOn("replaceVersionInManifest")
}


tasks.withType<Kotlin2JsCompile>().all {
//    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    dependsOn("generateVersionKt")
}

dependencies {
    implementation(KotlinX.coroutines.coreJs)
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
        }
    }
    sourceSets["main"].kotlin.srcDir(tasks.getByName("generateVersionKt").outputs.files)
}

tasks.register<Zip>("dist") {
    dependsOn(":browserProductionWebpack")
    from(layout.buildDirectory.dir("distributions"))
    include("*", "*/*")
    exclude("*.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Run `./gradlew refreshVersions` to update dependencies
// Run `./gradlew browserDevelopmentWebpack` for tests and `./gradlew dist` to release
