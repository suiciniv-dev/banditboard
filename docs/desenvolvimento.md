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

O mesmo módulo `desktop/` gera o app do Mac. Num Mac com o JDK 17:

```sh
./gradlew :desktop:run             # abre o app na barra de menus
./gradlew :desktop:packageDmg      # app em desktop/build/compose/binaries/main/app, com o DMG do lado
```

O DMG do `packageDmg` precisa do Finder para desenhar o atalho da pasta Aplicativos, e isso falha pelo SSH. Nas versões eu
monto o DMG com o `hdiutil` a partir do `.app`, com um link para `/Applications` do lado:

```sh
mkdir dmg && cp -R desktop/build/compose/binaries/main/app/Banditboard.app dmg/ && ln -s /Applications dmg/Applications
hdiutil create -volname Banditboard -srcfolder dmg -ov -format UDZO Banditboard-<versão>.dmg
```

Os avisos do Android com o app fechado precisam do `app/google-services.json` do projeto no Firebase. Esse arquivo não vai
para o git; sem ele, o build pula o Firebase e o resto do app funciona igual.

### iPhone e Apple Watch

O projeto do Xcode é gerado pelo [XcodeGen](https://github.com/yonaskolb/XcodeGen) a partir do `ios/project.yml`:

```sh
cd ios
xcodegen generate
xcodebuild -project Banditboard.xcodeproj -scheme Banditboard -destination "id=<UDID do iPhone>" DEVELOPMENT_TEAM=<time> -allowProvisioningUpdates -allowProvisioningDeviceRegistration build
xcodebuild -project Banditboard.xcodeproj -scheme Banditboard -configuration Release -destination generic/platform=iOS BUNDLE_BASE=dev.clawdboard.banditboard.sideload CODE_SIGNING_ALLOWED=NO build   # o IPA sem assinatura das versões
```

O `BUNDLE_BASE` define os identificadores do app, do widget, do app do relógio e das complicações. O grupo compartilhado do
Keychain é `<time>.dev.clawdboard.banditboard.shared`, e o app descobre o time quando abre, então o mesmo código funciona quando o
AltStore ou o Sideloadly assinam de novo com o Apple ID de outra pessoa. Para montar o IPA, copie o `Banditboard.app` de
`build/Build/Products/Release-iphoneos` para uma pasta `Payload` e compacte em ZIP.

Argumentos para tirar prints no simulador: `--demo` (dados de exemplo), `--lang=pt` ou `--lang=en`, `--settings` (abre os
ajustes), `--scroll-end` (rola até as notícias) e, no relógio, `--page=0`, `--page=1` ou `--page=2`.

### Servidor

A pasta `worker/` é o Worker da Cloudflare por trás do "conectar de qualquer lugar", com um banco D1:

```sh
cd worker
npx wrangler d1 execute banditboard --remote --file schema.sql
npx wrangler secret put FCM_SERVICE_ACCOUNT    # o JSON da conta de serviço do Firebase, para os avisos do Android
npx wrangler deploy
```

Ele também entrega os instaladores `/pc/box.ps1` e `/pc/box.sh`, que os comandos do /conectar usam.

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

Os apps do iPhone e do Apple Watch são em SwiftUI, porque os widgets e as complicações da Apple só existem ali. Eles seguem
as mesmas regras do código em Kotlin: o mesmo desenho do Racco, os mesmos níveis de aviso e os mesmos textos.

## Organização do código

- `core/`: pareamento e envio, cofre, histórico, configurações, música (player do Android) e o servidor do painel web
- `ui/`: as telas em Jetpack Compose e o guaxinim em pixel art
- `MediaListener.kt`: o leitor de notificações que o Android exige para enxergar o player
- `assets/panel.html`: o painel web, sem nenhuma dependência externa
- `assets/pc/`: os instaladores e os hooks de uso do Windows (PowerShell) e do macOS (sh), entregues pelo celular, pelo app do computador e pelo servidor
- `desktop/`: o widget e o ícone da bandeja do Windows e o app da barra de menus do Mac, com o servidor local e o compartilhamento na rede de casa, feitos com os mesmos arquivos de `core/` e `ui/`
- `ios/`: o app do iPhone, os widgets, o app do Apple Watch e as complicações, em SwiftUI (`Shared/` é o código que eles têm em comum)
- `worker/`: o servidor por trás do "conectar de qualquer lugar" e dos avisos pelo Firebase
- `site/`: o site banditboard.pages.dev, uma página estática que o Cloudflare Pages publica a cada push na `main`, com a página /conectar

Alguns nomes internos ainda dizem `clawdboard` (o pacote `dev.clawdboard`, o `clawdboard-usage.ps1`, o cabeçalho
`X-Clawdboard`). Eles ficaram assim de propósito, para não quebrar a instalação e o pareamento de quem já usa.
