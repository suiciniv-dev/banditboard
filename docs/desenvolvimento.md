# Desenvolvendo o Banditboard

<p align="right"><a href="development.md">English</a> · <b>Português</b></p>

[← Voltar para o README](../README.pt-BR.md) · [Como contribuir](../CONTRIBUTING.md)

## Compilando

Você vai precisar do JDK 17 e do Android SDK com a API 35 (no `ANDROID_HOME` ou no `sdk.dir` do `local.properties`):

```powershell
.\gradlew.bat testReleaseUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

O `assembleDebug` gera a versão de teste `dev.clawdboard.preview`, que fica instalada ao lado do app de verdade sem mexer
nos dados dele e aceita os dados de exemplo mais abaixo. O `assembleRelease` gera o app normal, que vai para a pasta `dist\`.

Na minha máquina eu uso os atalhos da pasta `scripts\`, que já apontam para o Gradle em `D:\Android\gradle-home` e para o JDK
que veio com o Visual Studio:

```powershell
.\scripts\compilar.ps1
.\scripts\instalar.ps1 -Ip 192.168.0.15   # IP do celular com ADB por Wi-Fi; sem -Ip, usa o celular de teste
```

App do Windows (Compose Desktop, com o mesmo núcleo e o mesmo guaxinim do celular):

```powershell
.\gradlew.bat :desktop:run                                   # abre o widget
.\gradlew.bat :desktop:packageMsi                            # gera o instalador em desktop\build\compose\binaries\main\msi
.\gradlew.bat :desktop:shots --args="prints\<versão>\pt pt"  # gera os prints do widget e do painel
```

## Diagnóstico pelo ADB

```powershell
adb shell am start -n dev.clawdboard.preview/dev.clawdboard.MainActivity --ez demo true --es lang PT --es mode MASCOTS --es orient PORTRAIT
# demo: dados de exemplo, só funciona antes de criar o PIN; mode, orient e backdrop são opcionais
# também: --ei zoom 150, --es skin XMAS, --es tint RAINBOW, --ei p5 0 (sessão vazia, dormindo), --ei p7 97 (vermelho), --ei pf 100 (limite do Fable), --ez nudge true (cartão de feedback)
# --ez music true liga a tela de música com uma música de exemplo tocando (guaxinins dançando); --ez playing false deixa pausada
adb shell am start -n dev.clawdboard/.MainActivity --ez selftest true  # testa o cofre no aparelho
adb logcat -s ClawdSelfTest
```

## Por que Kotlin

O app do celular e o do Windows são o mesmo código em Kotlin. O Jetpack Compose desenha as telas do Android e o Compose
Multiplatform desenha o widget do Windows. Por isso o Racco, as animações, as regras dos avisos, a leitura do uso e os textos
em português e inglês ficam num lugar só: mexeu no guaxinim uma vez, os dois apps já recebem.

## Organização do código

- `core/`: pareamento e envio, cofre, histórico, configurações, música (player do Android) e o servidor do painel web
- `ui/`: as telas em Jetpack Compose e o guaxinim em pixel art
- `MediaListener.kt`: o leitor de notificações que o Android exige para enxergar o player
- `assets/panel.html`: o painel web, sem nenhuma dependência externa
- `assets/pc/`: o instalador em PowerShell e o hook de uso que o celular entrega para o seu PC
- `desktop/`: o widget do Windows, o servidor local e o ícone da bandeja, feitos com os mesmos arquivos de `core/` e `ui/`
- `site/`: o site banditboard.pages.dev, uma página estática que o Cloudflare Pages publica a cada push na `main`

Alguns nomes internos ainda dizem `clawdboard` (o pacote `dev.clawdboard`, o `clawdboard-usage.ps1`, o cabeçalho
`X-Clawdboard`). Eles ficaram assim de propósito, para não quebrar a instalação e o pareamento de quem já usa.
