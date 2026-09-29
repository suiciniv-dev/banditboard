# Developing Banditboard

<p align="right"><b>English</b> · <a href="desenvolvimento.md">Português</a></p>

[← Back to the README](../README.md) · [Contributing](../CONTRIBUTING.md)

## Building

Needs JDK 17 and the Android SDK with API 35 (in `ANDROID_HOME` or in `sdk.dir` of `local.properties`):

```powershell
.\gradlew.bat testReleaseUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

`assembleDebug` builds the preview `dev.clawdboard.preview`, which installs next to the real app without touching its data and
accepts the sample data below. `assembleRelease` builds the regular app; locally it goes to `dist\`.

On my machine I use the shortcuts in `scripts\`, which point to Gradle in `D:\Android\gradle-home` and to the JDK installed by
Visual Studio:

```powershell
.\scripts\compilar.ps1
.\scripts\instalar.ps1 -Ip 192.168.0.15   # phone IP with ADB over Wi-Fi; without -Ip it uses the test phone
```

Windows app (Compose Desktop, sharing the core and the raccoon with the phone):

```powershell
.\gradlew.bat :desktop:run                                   # opens the widget
.\gradlew.bat :desktop:packageMsi                            # installer in desktop\build\compose\binaries\main\msi
.\gradlew.bat :desktop:shots --args="prints\<version>\en en" # renders the widget and dashboard screenshots
```

## Diagnostics over ADB

```powershell
adb shell am start -n dev.clawdboard.preview/dev.clawdboard.MainActivity --ez demo true --es lang EN --es mode MASCOTS --es orient PORTRAIT
# demo: sample data, only works before a PIN is created; mode, orient and backdrop are optional
# also: --ei zoom 150, --es skin XMAS, --es tint RAINBOW, --ei p5 0 (empty session, asleep), --ei p7 97 (red), --ei pf 100 (Fable's own limit), --ez nudge true (feedback card)
# --ez music true turns on the music screen with a sample track playing (raccoons dancing); --ez playing false leaves it paused
adb shell am start -n dev.clawdboard/.MainActivity --ez selftest true  # tests the vault on the device
adb logcat -s ClawdSelfTest
```

## Why Kotlin

The phone app and the Windows app are the same Kotlin code. Jetpack Compose draws the Android screens and Compose
Multiplatform draws the Windows widget, so Racco, his animations, the alert rules, the usage parsing and the texts in both
languages live in one place: change the raccoon once and both apps get it.

## Architecture

- `core/`: pairing and push, vault, history, settings, music (Android media session) and the web dashboard server
- `ui/`: Jetpack Compose screens and the pixel-art raccoon
- `MediaListener.kt`: the notification listener Android requires to see the active player
- `assets/panel.html`: the web dashboard, with no external dependencies
- `assets/pc/`: the PowerShell installer and the usage hook the phone serves to your PC
- `desktop/`: the Windows widget, local server and tray, built from the same `core/` and `ui/` sources
- `site/`: the website at banditboard.pages.dev, a static page published by Cloudflare Pages on every push to `main`

Some internal names still say `clawdboard` (the `dev.clawdboard` package, `clawdboard-usage.ps1`, the `X-Clawdboard` header).
They stay that way on purpose so existing installs and pairings keep working.
