import java.net.URL
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.ipvault"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ipvault"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}


// Fetch the open-source Monocraft typeface and package it as an Android font resource.
val monocraftResDir = layout.buildDirectory.dir("generated/monocraft/res")
val monocraftFontFile = monocraftResDir.map { it.file("font/monocraft.ttf") }
val downloadMonocraftFont by tasks.registering {
    outputs.file(monocraftFontFile)
    doLast {
        val output = monocraftFontFile.get().asFile
        if (!output.exists()) {
            output.parentFile.mkdirs()
            val archive = layout.buildDirectory.file("downloads/Monocraft-ttf.zip").get().asFile
            archive.parentFile.mkdirs()
            URL("https://github.com/IdreesInc/Monocraft/releases/download/v4.2.1/Monocraft-ttf.zip")
                .openConnection().apply { setRequestProperty("User-Agent", "IP-VAULT-Android-Build") }
                .getInputStream().use { input -> archive.outputStream().use { input.copyTo(it) } }
            var copied = false
            ZipInputStream(archive.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name.substringAfterLast('/').lowercase()
                    if (!entry.isDirectory && name.endsWith(".ttf") &&
                        !name.contains("bold") && !name.contains("italic") &&
                        !name.contains("thin") && !name.contains("light")) {
                        output.outputStream().use { zip.copyTo(it) }
                        copied = true
                        break
                    }
                }
            }
            check(copied && output.length() > 1000L) { "Could not find Monocraft regular TTF in downloaded archive" }
        }
    }
}
// Package the exact generated launcher artwork (not a vector redraw).
val launcherIconResDir = layout.buildDirectory.dir("generated/launcher-icon/res")
val installLauncherIcon by tasks.registering {
    val source = layout.projectDirectory.file("src/main/assets/launcher_icon.png.b64")
    val output = launcherIconResDir.map { it.file("mipmap-nodpi/ic_launcher.png") }
    inputs.file(source)
    outputs.file(output)
    doLast {
        val target = output.get().asFile
        target.parentFile.mkdirs()
        target.writeBytes(java.util.Base64.getDecoder().decode(source.asFile.readText().trim()))
    }
}
android.sourceSets.getByName("main").res.srcDir(monocraftResDir)
android.sourceSets.getByName("main").res.srcDir(launcherIconResDir)
tasks.named("preBuild").configure {
    dependsOn(downloadMonocraftFont, installLauncherIcon)
}
