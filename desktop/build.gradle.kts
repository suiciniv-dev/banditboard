import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val shared = listOf(
    "core/Alerts.kt", "core/Http.kt", "core/Look.kt", "core/StatusApi.kt", "core/Models.kt", "core/Push.kt", "core/Settings.kt", "core/Texts.kt",
    "ui/Colors.kt", "ui/Format.kt", "ui/Mascot.kt",
)

sourceSets.main {
    kotlin.srcDir("../app/src/main/java")
    kotlin.include(shared.map { "dev/clawdboard/$it" } + "dev/clawdboard/desktop/**")
    resources.srcDir("../app/src/main/assets")
    resources.srcDir("../app/src/main/res/font")
    resources.include("pc/**", "fredoka.ttf", "music.ps1", "connect.ps1")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    implementation("org.json:json:20240303")
    implementation("io.nayuki:qrcodegen:1.8.0")
    implementation("net.java.dev.jna:jna:5.14.0")
}

tasks.register<JavaExec>("shots") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "dev.clawdboard.desktop.ShotsKt"
    args(rootProject.file("prints/1.9.0").absolutePath)
}

compose.desktop {
    application {
        mainClass = "dev.clawdboard.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            modules("java.instrument", "jdk.httpserver", "jdk.unsupported")
            packageName = "Banditboard"
            packageVersion = "1.13.2"
            vendor = "suiciniv-dev"
            windows {
                perUserInstall = true
                menu = true
                shortcut = true
                iconFile.set(project.file("icons/banditboard.ico"))
                upgradeUuid = "6f1d3c2a-8b4e-4f7a-9c1d-2e5b7a9c0d13"
            }
        }
    }
}
