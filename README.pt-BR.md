# 🦝 Banditboard

<p align="right"><a href="README.md">English</a> · <b>Português</b></p>

**Transforme um celular Android antigo, ou só o seu PC com Windows, num monitor de uso do Claude Code.**

<p align="center">
  <img src="prints/1.8.0/hero.gif" width="760" alt="Guaxinins dormindo com a sessão vazia, acordando, ficando vermelhos perto do limite, estourando em 100% e dançando quando toca música">
</p>

<p align="center">
  <a href="../../releases/latest"><img src="https://img.shields.io/github/v/release/suiciniv-dev/banditboard?label=baixar&color=d77757" alt="Baixar"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Windows-10%2B-0078D4?logo=windows&logoColor=white" alt="Windows 10+">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin e Jetpack Compose">
  <a href="../../stargazers"><img src="https://img.shields.io/github/stars/suiciniv-dev/banditboard?style=flat" alt="Estrelas no GitHub"></a>
</p>

<p align="center"><b><a href="../../releases/latest">Baixar o APK ou o instalador do Windows</a></b> · <a href="../../releases">Todas as versões</a></p>

## O que é o Banditboard?

O Banditboard é um painel sempre ligado com o seu uso do Claude, feito para aquele celular Android antigo parado na gaveta.
Ele mostra quanto você já usou da sessão de 5 horas e da semana, quando cada uma libera e se algum modelo está com incidente
aberto. Cada modelo é o Racco, um guaxinim em pixel art que dorme, sua, estoura em 100% e dança quando toca música.

Não tem celular sobrando? O app do Windows é um widget pequeno, sempre no topo, que funciona sozinho e também pode rodar junto
com o app do celular: o mesmo hook alimenta os dois.

## ✨ Recursos

- 📊 **Uso num relance**: sessão de 5 horas e semana de 7 dias, com contagem regressiva e o horário local em que cada uma libera
- 🦝 **Racco, o guaxinim**: um por modelo (Haiku, Sonnet, Opus e Fable). Eles piscam, acenam, dormem com a sessão vazia, suam a partir de 85%, ficam vermelhos a partir de 90% e estouram em 100%. O Racco clássico, mais detalhado, continua a um toque nas configurações
- 🔔 **Avisos de limite**: notificação em 80%, 90% e 100% da sessão ou da semana, e outra quando libera, mesmo com o app fechado
- 🪟 **Widget no Windows**: completo, compacto ou só o Racco num canto da tela, sempre no topo, com avisos pela notificação do próprio Windows
- 🔌 **Nenhum token no celular**: os números vêm do próprio `/usage` do Claude Code no seu PC, pela rede local
- 🎵 **Modo música**: o que estiver tocando no celular (Spotify, YouTube Music ou qualquer player), com capa, controles e volume, enquanto os guaxinins dançam em todas as telas
- 📈 **Histórico de 7 dias**: uma amostra a cada 30 minutos
- 🖥️ **Painel web**: a mesma visão no navegador do PC, na rede local, com login por PIN
- 🚦 **Status e notícias**: incidentes abertos em status.claude.com e as últimas notícias da Anthropic
- 🌙 **Preto AMOLED**: e alguns pixels de deslocamento por minuto contra marcas na tela
- 🌍 **Português e inglês**: segue o idioma do celular, ou escolha nas configurações
- 📱 **Retrato e paisagem**: um layout para cada, e zoom de acessibilidade de 90% a 150%

## 📱 Telas

O app fala português do Brasil e inglês. Ele segue o idioma do celular, e dá para escolher em Configurações → Tela → Idioma.
As telas abaixo estão em inglês.

### Telas principais

| | | |
|---|---|---|
| <img src="prints/1.9.0/01-dashboard.png" alt="Painel"> | <img src="prints/1.9.0/02-mascots.png" alt="Tela dos mascotes"> | <img src="prints/1.8.0/03-clock-portrait.png" alt="Relógio de mesa em retrato"> |
| Painel | Mascotes | Relógio de mesa |

### Avisos de limite

<img src="prints/1.9.0/03-alerts.png" width="420" alt="Notificações: semana em 80% e sessão em 90%, com o horário em que libera">

O celular mantém uma notificação discreta enquanto escuta o seu PC, para os avisos chegarem mesmo com o app fechado.
Dá para desligar em Configurações → Tela.

### Widget no Windows

<p>
  <img src="prints/1.9.0/windows-widget.png" width="480" alt="Widget do Windows com sessão, semana e os quatro guaxinins">
  <img src="prints/1.9.0/windows-compact.png" width="300" alt="Widget compacto">
  <img src="prints/1.9.0/windows-mini.png" width="96" alt="Só o Racco">
</p>

Completo, compacto ou só o Racco: escolha o formato no menu do ícone da bandeja. Um duplo clique em qualquer widget, ou "Abrir painel" na bandeja, abre uma janela maior com o relógio, o uso, os quatro guaxinins, os incidentes abertos e todas as configurações do widget. Quando toca música no Windows (Spotify, navegador, qualquer app nos controles de mídia do Windows), os guaxinins dançam. O "Só o Racco" fica num canto mostrando a
sessão, e um duplo clique abre o painel completo. Arraste para onde quiser; ele também pode ficar fora da barra de tarefas e iniciar
com o Windows. Com uma única sessão do Claude Code aberta, o "Só o Racco" e o widget compacto usam o acessório do modelo
dessa sessão (óculos no Opus, cartola no Fable); com mais de uma, o Racco volta ao normal.

Sem celular conectado, o widget sugere o app do celular mais ou menos uma vez por semana, com um QR code para baixar.
"Não mostrar de novo" desliga a sugestão.

<img src="prints/1.9.0/windows-promo.png" width="440" alt="Sugestão para instalar o app do celular, com QR code">

### Reações do Racco

As telas abaixo mostram o Racco clássico. O novo padrão é assim:

<img src="prints/1.9.0/racco.png" width="520" alt="O Racco novo com o acessório de cada modelo">

| | | |
|---|---|---|
| <img src="prints/1.8.0/04-sleeping.png" alt="Guaxinins dormindo"> | <img src="prints/1.8.0/05-sweating-88.png" alt="Guaxinins suando em 88%"> | <img src="prints/1.8.0/06-red-97.png" alt="Guaxinins vermelhos em 97%"> |
| Sessão vazia: dormindo | 88%: suando | 97%: vermelhos e tremendo |
| <img src="prints/1.8.0/07-burst-100.png" alt="Todos os guaxinins estourados em 100%"> | <img src="prints/1.8.0/08-only-fable.png" alt="Só o Fable estourado"> | <img src="prints/1.8.0/09-dancing.png" alt="Guaxinins dançando"> |
| 100%: estourados | Limite próprio do Fable em 100%: só o Fable estoura | Música tocando: dançando |

### Modo música

| | |
|---|---|
| <img src="prints/1.8.0/10-music-landscape.png" alt="Tela de música em paisagem"> | <img src="prints/1.8.0/11-music-portrait.png" width="300" alt="Tela de música em retrato"> |
| Capa, faixa, controles e volume | Retrato |

### Visuais e configurações

| | | |
|---|---|---|
| <img src="prints/1.8.0/12-rainbow.png" alt="Cores de arco-íris"> | <img src="prints/1.8.0/13-christmas.png" alt="Visual de Natal"> | <img src="prints/1.8.0/14-settings.png" alt="Configurações dos mascotes"> |
| Uma cor por modelo | Visual de Natal | Configurações, com zoom de 150% |

### Painel web, AMOLED e créditos

| | | |
|---|---|---|
| <img src="prints/1.8.0/15-web-dashboard.png" alt="Painel web num PC"> | <img src="prints/1.8.0/16-amoled.png" alt="Painel em preto AMOLED"> | <img src="prints/1.8.0/17-credits.png" alt="Créditos com o Racco"> |
| Painel web no navegador do PC | Preto AMOLED | Créditos |

## 🚀 Começo rápido

1. Baixe o APK em [Releases](../../releases/latest) e instale (permita "instalar apps desconhecidos" para o app que abrir o arquivo).
2. No PC, abra o endereço que aparece no celular e crie um PIN.
3. No cartão "Claude Code no seu PC", clique em "Copiar" e cole o comando no PowerShell.
4. Continue usando o Claude Code, no VS Code ou no terminal. Depois de uma resposta, o celular atualiza.

**Só no Windows, sem celular:**

1. Baixe o `Banditboard-<versão>.msi` em [Releases](../../releases/latest) e instale (não pede administrador). Se o antivírus ou a política da empresa bloquear apps instalados, use o `Banditboard-<versão>-windows.zip`: descompacte onde quiser e abra o `Banditboard.exe`.
2. Clique em "Conectar Claude Code" no widget.
3. Continue usando o Claude Code. Depois de uma resposta, o widget atualiza.

Usando os dois? Conecte cada um uma vez. O hook guarda uma lista de destinos em `~/.claude/clawdboard-targets.json`
e envia para todos. Quem já tinha o celular conectado não precisa refazer nada nele: ao conectar o Windows, o destino do
celular entra na lista sozinho.

> Um hook do Claude Code roda o `/usage` depois das respostas, no máximo a cada 2 minutos, sem gastar tokens. O uso do
> claude.ai ou de outros aparelhos também aparece, porque o `/usage` mostra o plano inteiro. O script do PC, por enquanto, é só para Windows.

## 🔐 Segurança

- O celular nunca guarda um token do Claude e nunca chama a API da Anthropic. O Claude Code no seu PC lê seus limites com o `/usage` e um script pequeno repassa os números. O script nunca lê as credenciais do Claude Code.
- O script envia só as porcentagens, os horários em que liberam e a família do modelo de cada sessão ativa nos últimos 10 minutos ("opus", "sonnet"...), nada das suas conversas, arquivos ou IDs de sessão.
- Cada envio leva uma chave de pareamento de 128 bits. O celular confere contra um hash SHA-256, e "Criar chave nova" invalida o comando antigo na hora.
- A chave de pareamento é criptografada com AES-256-GCM, usando uma chave derivada do seu PIN (PBKDF2, 150.000 iterações) e protegida por uma chave do Android Keystore. O PIN nunca é guardado.
- 10 PINs errados seguidos apagam a chave de pareamento, o histórico e as configurações.
- O painel web só funciona na rede local, pede o mesmo PIN e só responde quando o Host é um IP, `localhost` ou um nome `.local`, o que bloqueia DNS rebinding. Entrar no painel também desbloqueia a tela do celular.
- O instalador guarda um backup das configurações do Claude Code em `settings.json.antes-do-clawdboard`, adiciona dois hooks (`Stop` e `SessionStart`) e não mexe na sua status line.
- O app do Windows só escuta em `127.0.0.1`, então nada na sua rede alcança ele, e mesmo assim confere a chave de pareamento em cada envio.
- O modo música precisa de acesso às notificações porque o Android só mostra o player ativo para apps com esse acesso. O Banditboard usa isso para ver e controlar o player; ele não lê suas notificações.

## 🔧 Detalhes técnicos

### Como funciona

```
 Claude Code no seu PC (VS Code ou terminal)
          │  hook Stop / SessionStart, no máximo a cada 2 minutos
          ▼
 clawdboard-usage.ps1 ──► claude -p "/usage"   (comando local, sem tokens, hooks desligados)
          │
          └──► POST para cada destino em clawdboard-targets.json   (chave de pareamento, só os números)
                 ├──► http://IP-DO-CELULAR:8080/api/push
                 └──► http://127.0.0.1:47810/api/push   (app do Windows)

 ┌──────────────────┐
 │ Celular Android  │ ──► status.claude.com    incidentes abertos
 │   Banditboard    │ ──► feed RSS público     notícias da Anthropic
 └──────────────────┘
          ├──► 🦝 guaxinins na tela do celular
          └──► painel web na sua rede local (http://IP-DO-CELULAR:8080)

 O modo música lê e controla a sessão de mídia do próprio celular. Nenhuma conta envolvida.
```

### Configuração

**Primeira configuração.** O celular mostra o endereço dele, algo como `http://192.168.0.15:8080`. A porta é a primeira livre entre
8080 e 8089, e o PC e o celular precisam estar no mesmo Wi-Fi. Também dá para criar o PIN no próprio celular, em
"Prefiro criar o PIN neste celular", e conectar o Claude Code depois pelo painel. Se o roteador reiniciar, o celular pode ganhar
um IP novo; a tela e o rodapé do painel sempre mostram o atual. Reserve um IP fixo para o celular no DHCP do roteador para
evitar isso; se o IP mudar, rode o comando do painel de novo.

**Conectando o Claude Code.** O comando baixa um instalador do celular. Ele grava `~/.claude/clawdboard-usage.ps1`, coloca o
destino em `~/.claude/clawdboard-targets.json` e adiciona o script como hook `Stop` e `SessionStart` em `~/.claude/settings.json`.
O hook volta na hora e, no máximo a cada 2 minutos, dispara em segundo plano, escondido, um `claude -p "/usage" --no-session-persistence`
com os hooks desligados, lê as linhas da sessão, da semana e de cada modelo e envia. Ele usa o `claude` do PATH ou a cópia que vem
com a extensão do VS Code. Para desfazer, restaure o `settings.json.antes-do-clawdboard` ou remova os dois hooks `clawdboard-usage`.
Quando o horário de uma janela passa sem envio novo, o celular zera ela sozinho.

**No celular.** Deslize para a esquerda para a próxima tela e para a direita para voltar. Fora do carrossel, ele volta para a tela
inicial depois de 30 segundos. A engrenagem no canto de baixo abre as configurações depois de pedir o PIN. Depois de reiniciar o
celular ou o app, a tela pede o PIN de novo, e dá para desbloquear pelo painel web. O rodapé mostra quando chegou o último envio.

**Telas e modos.** Painel, mascotes (sessão e semana em cima, os quatro Raccos embaixo), gráfico de 7 dias, notícias, relógio de mesa
e música (quando ligada). Modos de tela: fixo, mascotes, carrossel ou relógio. No carrossel, a tela de música só aparece enquanto
algo está tocando.

**Barra por modelo.** Quando o `/usage` mostra um limite semanal próprio de um modelo (hoje só o Fable, em alguns planos), a barra
desse modelo fica colorida. Os outros modelos usam o limite semanal geral, então mostram esse valor em cinza.

**Visuais.** "Por modelo" (o padrão) põe uma cartola no Fable, o mais caro, óculos no Opus, fone no Sonnet e um broto no Haiku.
Também há "Clássico" (sem acessório), "Coroas" e "Natal", e cinco cores: natural (o cinza do guaxinim), arco-íris (uma por modelo),
lavanda, menta e chiclete. O painel web desenha o mesmo visual a partir da definição que o app manda em `state.look`.

**Animações.** Além de piscar, eles olham em volta, mexem as pernas, acenam, mexem as orelhas e se abaixam. Com a sessão de 5 horas
zerada eles dormem (olhos fechados e um Z). A partir de 85% suam e ficam inquietos; a partir de 90% ficam vermelhos e pulsam, e a
partir de 95% tremem. Em 100% estouram e ficam carbonizados, com olhos em X e fumaça, marcados como "esgotado". Sessão ou semana
geral em 100% estoura os quatro; o limite próprio do Fable em 100% estoura só o Fable. Um incidente aberto em status.claude.com que
cite um modelo deixa o guaxinim dele cinza, com olhos em X. As animações podem ser desligadas nas configurações.

**Música.** Configurações → Música → "Tela de música", depois "Dar acesso" e permita o Banditboard. O Banditboard não toca nada: ele
lê e controla o player do app que está tocando, pela sessão de mídia do Android. A tela mostra a capa do álbum, a faixa, o ícone do
app (um toque abre o player dele, para trocar de playlist), a barra de progresso, os botões, os botões extras do app e o volume: o
volume de mídia do celular ou, quando o app manda o som para outro aparelho, o volume desse aparelho. Enquanto a música toca, todos os
guaxinins dançam em todas as telas, painel web incluído: os sonolentos acordam, os suados dançam suando e os estourados batem o pé.
Num APK instalado pelo navegador ou pelo gerenciador de arquivos, o Android 13 ou mais novo pode dizer que é uma "configuração
restrita". Nesse caso: Configurações → Apps → Banditboard → ⋮ → Permitir configurações restritas, e tente de novo.

**Zoom e modo compacto.** 90, 100, 115, 130 ou 150% nas telas e nas configurações; as telas de bloqueio e de PIN mantêm o tamanho do
sistema. Quando o lado menor da tela fica abaixo de 380dp (zoom alto ou celular pequeno), as telas passam para um layout compacto e
escondem linhas secundárias.

**Fundo.** "Tema padrão" usa tons escuros quentes (#16130f com um brilho coral embaixo). "Preto AMOLED" economiza mais a tela.

**Feedback.** Depois de 3 dias de uso, um cartão pergunta se você está gostando do app, com um botão que abre um e-mail para
vinips00@gmail.com. Ele some sozinho depois de 30 segundos, volta a cada 10 dias e tem "Não mostrar de novo". Depois que você manda
um e-mail, ele só volta em 60 dias. O mesmo contato está nos créditos das configurações e no rodapé do painel web.

**Abrir ao ligar.** Precisa de uma permissão que só o ADB concede. Sem ela, o app funciona normalmente, só não abre sozinho depois de reiniciar:

```powershell
adb shell appops set dev.clawdboard SYSTEM_ALERT_WINDOW allow
```

**Atualizando.** Instalar um APK novo por cima do antigo mantém o PIN, as configurações e o histórico, desde que seja assinado com a
mesma chave. O app pede o PIN uma vez depois da atualização. Vindo da 1.4 ou anterior, o primeiro desbloqueio troca o token do Claude
guardado por uma chave de pareamento; depois conecte o Claude Code pelo painel.

### De onde vêm os dados

- **Uso:** a saída de `claude -p "/usage"`, um comando local do Claude Code que não chama nenhum modelo: as linhas "Current session", "Current week (all models)" e "Current week (<modelo>)", com os horários em que liberam.
- **Status:** `https://status.claude.com/api/v2/incidents/unresolved.json`
- **Notícias:** o feed RSS público `Olshansk/rss-feeds`.

### Desenvolvimento

Precisa do JDK 17 e do Android SDK com a API 35 (em `ANDROID_HOME` ou no `sdk.dir` do `local.properties`):

```powershell
.\gradlew.bat testReleaseUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

O `assembleDebug` gera a prévia `dev.clawdboard.preview`, que instala ao lado do app de verdade sem mexer nos dados dele e aceita
os dados de exemplo abaixo. O `assembleRelease` gera o app normal; localmente ele vai para `dist\`.

Na minha máquina eu uso os atalhos em `scripts\`, que apontam para o Gradle em `D:\Android\gradle-home` e para o JDK instalado pelo Visual Studio:

```powershell
.\scripts\compilar.ps1
.\scripts\instalar.ps1 -Ip 192.168.0.15   # IP do celular com ADB por Wi-Fi; sem -Ip usa o celular de teste
```

App do Windows (Compose Desktop, com o mesmo núcleo e o mesmo guaxinim do celular):

```powershell
.\gradlew.bat :desktop:run          # abre o widget
.\gradlew.bat :desktop:packageMsi   # instalador em desktop\build\compose\binaries\main\msi
.\gradlew.bat :desktop:shots        # gera os prints do widget em prints\
```

Diagnóstico pelo ADB:

```powershell
adb shell am start -n dev.clawdboard.preview/dev.clawdboard.MainActivity --ez demo true --es lang PT --es mode MASCOTS --es orient PORTRAIT
# demo: dados de exemplo, só funciona antes de criar o PIN; mode, orient e backdrop são opcionais
# também: --ei zoom 150, --es skin XMAS, --es tint RAINBOW, --ei p5 0 (sessão vazia, dormindo), --ei p7 97 (vermelho), --ez nudge true (cartão de feedback)
# --ez music true liga a tela de música com uma faixa de exemplo tocando (guaxinins dançando); --ez playing false deixa pausada
adb shell am start -n dev.clawdboard/.MainActivity --ez selftest true  # testa o cofre no aparelho
adb logcat -s ClawdSelfTest
```

### Arquitetura

- `core/`: pareamento e envio, cofre, histórico, configurações, música (sessão de mídia do Android) e o servidor do painel web
- `ui/`: telas em Jetpack Compose e o guaxinim em pixel art
- `MediaListener.kt`: o leitor de notificações que o Android exige para ver o player ativo
- `assets/panel.html`: o painel web, sem dependências externas
- `assets/pc/`: o instalador em PowerShell e o hook de uso que o celular entrega para o seu PC
- `desktop/`: o widget do Windows, o servidor local e a bandeja, feitos com os mesmos arquivos de `core/` e `ui/`

### Próximos passos

- **macOS e Linux:** uma versão em shell do hook de uso.
- **Android 16:** API 36.

## 🤝 Contribuindo

Issues e pull requests são bem-vindos. Para mudanças maiores, abra uma issue antes para a gente conversar. O código não tem comentários
de propósito: as explicações ficam neste README. Os textos do app ficam em `core/Texts.kt`, em português do Brasil e inglês. Feedback e ideias: vinips00@gmail.com.

## 📄 Licença e aviso

Ainda não há licença de código aberto, então todos os direitos são reservados. A fonte Fredoka é distribuída sob a SIL Open Font License;
o texto dela vai dentro do APK, em `assets/licenses/`.

O Banditboard é um projeto pessoal de fã, **sem vínculo, apoio ou patrocínio da Anthropic**. Claude e Claude Code são marcas da
Anthropic, PBC.

Feito com ❤️ por Vinícius Pires da Silva.
