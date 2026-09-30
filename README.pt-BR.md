# 🦝 Banditboard

<p align="right"><a href="README.md">English</a> · <b>Português</b></p>

**Acompanhe o limite do Claude Code num celular Android parado, no iPhone e no Apple Watch, ou num widget no Windows e no Mac.**

<p align="center">
  <img src="prints/1.8.0/hero.gif" width="760" alt="Guaxinins dormindo com a sessão vazia, acordando, ficando vermelhos perto do limite, estourando em 100% e dançando com música">
</p>

<p align="center">
  <a href="https://banditboard.pages.dev/video/banditboard-pt.mp4"><img src="prints/1.14.0/pt/video.webp" width="560" alt="Veja o vídeo: o Banditboard no Android, iPhone, Apple Watch, Windows e Mac"></a><br>
  <sub>▶️ <a href="https://banditboard.pages.dev/video/banditboard-pt.mp4">Veja em vídeo</a></sub>
</p>

<p align="center">
  <a href="../../releases/latest"><img src="https://img.shields.io/github/v/release/suiciniv-dev/banditboard?label=baixar&color=d77757" alt="Última versão"></a>
  <a href="../../releases"><img src="https://img.shields.io/github/downloads/suiciniv-dev/banditboard/total?label=downloads&color=d77757" alt="Total de downloads"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/suiciniv-dev/banditboard?label=licen%C3%A7a" alt="Licença MIT"></a>
  <a href="../../commits/main"><img src="https://img.shields.io/github/last-commit/suiciniv-dev/banditboard?label=%C3%BAltimo%20commit" alt="Último commit"></a>
  <a href="../../stargazers"><img src="https://img.shields.io/github/stars/suiciniv-dev/banditboard?style=flat&label=estrelas" alt="Estrelas no GitHub"></a>
  <br>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/iPhone-iOS%2017%2B-555555" alt="iPhone, iOS 17+">
  <img src="https://img.shields.io/badge/Apple%20Watch-watchOS%2010%2B-555555" alt="Apple Watch, watchOS 10+">
  <img src="https://img.shields.io/badge/Windows-10%2B-0078D4" alt="Windows 10+">
  <img src="https://img.shields.io/badge/macOS-11%2B-555555" alt="macOS 11+">
</p>

<p align="center"><b><a href="#-para-começar">Para começar</a></b> · <a href="https://banditboard.pages.dev/">Site</a> · <a href="https://banditboard.pages.dev/conectar/">Conectar de qualquer lugar</a> · <a href="../../releases">Todas as versões</a></p>

## O que é o Banditboard?

É um painel que fica sempre à vista mostrando o seu uso do Claude. Ele mostra quanto você já gastou da sessão de 5 horas e
da semana, quando cada uma libera e se algum modelo está com problema. Cada modelo é o Racco, um guaxinim em pixel art que
dorme, sua, estoura quando chega em 100% e dança quando toca música.

Começou naquele celular Android esquecido na gaveta e hoje vai para onde você olha: no iPhone, com widgets na Tela de Início
e no StandBy; no Apple Watch, com complicações no mostrador; no Windows, num widget pequeno sempre por cima das janelas; e no
Mac, com o Racco e a porcentagem da sessão na barra de menus. Use um só ou todos juntos: o mesmo hook do Claude Code manda o
uso para todos os aparelhos.

### Por que o Banditboard?

A gente só confere o limite quando lembra de rodar o `/usage`, e a sessão costuma acabar bem no meio da tarefa. O
Banditboard deixa os números sempre à vista, no celular do lado do monitor, no pulso ou num canto da tela, e avisa quando
chega em 80%, 90% e 100%. Não precisa deixar terminal aberto nem ficar atualizando aba. Os números vêm do próprio Claude Code,
então você não entrega token nem login para ninguém.

## ✨ O que ele faz

- 📊 **Uso num piscar de olhos**: sessão de 5 horas e semana, com contagem regressiva e a hora em que cada uma libera
- 🦝 **Racco, o guaxinim**: um para cada modelo (Haiku, Sonnet, Opus e Fable). Eles piscam, acenam, dormem quando a sessão está vazia, começam a suar em 85%, ficam vermelhos em 90% e estouram em 100%. Skins, cores e o Racco clássico ficam nas configurações
- 🔔 **Avisos de limite**: notificação quando a sessão ou a semana chegam em 80%, 90% e 100%, e outra quando liberam. No Android, chegam mesmo com o app fechado
- 📱 **iPhone**: painel, widgets na Tela de Início e na Tela Bloqueada, StandBy no carregador e avisos
- ⌚ **Apple Watch**: app com sessão, semana e modelos, e complicações para o mostrador. Depois de parear pelo iPhone, o relógio busca o uso sozinho
- 🍎 **Barra de menus do Mac**: o Racco e a porcentagem da sessão do lado do relógio, um painelzinho ao clicar e, se quiser, um widget na mesa
- 🪟 **Widget no Windows**: completo, compacto ou só o Racco no canto da tela, sempre por cima das janelas, com avisos do próprio Windows
- 🌐 **Conectar de qualquer lugar**: pareie em [banditboard.pages.dev/conectar](https://banditboard.pages.dev/conectar/) e o uso chega no celular mesmo fora da rede de casa, cifrado de ponta a ponta
- 📷 **Pareamento por QR code**: leia um código do site ou do painel do Windows ou do Mac, sem PIN
- 🔌 **Sem token no celular**: os números vêm do `/usage` do próprio Claude Code, no seu computador
- 🎵 **Modo música**: mostra o que está tocando no celular (Spotify, YouTube Music ou qualquer outro), com capa, controles e volume, e os guaxinins dançam em todas as telas
- 📈 **Histórico de 7 dias**: uma medição a cada 30 minutos
- 🖥️ **Painel web**: a tela do app do Android no navegador do PC, dentro da sua rede, protegida por PIN
- 🚦 **Status e notícias**: problemas em aberto no status.claude.com e as últimas notícias da Anthropic
- 🌙 **Preto AMOLED**: e a tela se mexe alguns pixels por minuto para não marcar
- 🌍 **Português e inglês**: segue o idioma do aparelho, ou você escolhe
- 📱 **Em pé ou deitado**: no Android, um layout para cada, e zoom de 90% a 150%

## 🚀 Para começar

| Onde | Arquivo | |
|---|---|---|
| Android 8.0 ou mais novo | `Banditboard-<versão>.apk` | [Baixar](../../releases/latest) |
| iPhone (iOS 17 ou mais novo) e Apple Watch (watchOS 10 ou mais novo), beta | `Banditboard-<versão>-iphone.ipa` | [Baixar](../../releases/latest) |
| Windows 10 ou mais novo, instalador | `Banditboard-<versão>.msi` | [Baixar](../../releases/latest) |
| Windows 10 ou mais novo, portátil | `Banditboard-<versão>-windows.zip` | [Baixar](../../releases/latest) |
| macOS 11 ou mais novo, Intel ou Apple Silicon | `Banditboard-<versão>.dmg` | [Baixar](../../releases/latest) |

### No computador

Os apps do Windows e do Mac já funcionam sozinhos como painel, e também conectam o Claude Code para os seus outros aparelhos.

**Windows**

1. Instale o MSI. Não precisa ser administrador. Se preferir não instalar nada, descompacte o ZIP onde quiser e abra o `Banditboard.exe`.
2. Clique em "Conectar Claude Code" no widget. O uso já aparece em seguida. Se não der para conectar, o widget diz o motivo e o "O que fazer" abre o painel com o próximo passo.
3. Use o Claude Code normalmente. A cada resposta, o widget atualiza. Para buscar na hora, use "Atualizar agora" no painel ou no menu da bandeja.

**Mac**

1. Abra o DMG e arraste o Banditboard para a pasta Aplicativos.
2. O app ainda não tem a notarização da Apple, então na primeira vez o macOS diz que não conseguiu verificar. Vá em Ajustes do Sistema → Privacidade e Segurança e clique em "Abrir Mesmo Assim". Nos Macs com Apple Silicon, o macOS pode oferecer para instalar o Rosetta antes.
3. O Racco aparece na barra de menus. Clique nele e em "Conectar Claude Code". O uso aparece em seguida, e a barra passa a mostrar a porcentagem da sessão.

### No celular e no relógio

Escolha um dos três jeitos de levar os números até lá:

- **De qualquer lugar (Android e iPhone).** Abra [banditboard.pages.dev/conectar](https://banditboard.pages.dev/conectar/) em qualquer navegador. Leia o QR code com o celular, copie o comando do Windows ou do Mac e rode no computador onde está o Claude Code. O uso passa pelo servidor do Banditboard cifrado de ponta a ponta, então o celular não precisa estar no seu Wi-Fi. No Android, os avisos chegam mesmo com o app fechado.
- **QR code na rede de casa (Android e iPhone).** No painel do Windows ou do Mac, ligue "Compartilhar o uso com o iPhone na rede de casa" e leia o código. Funciona para o Android também.
- **PIN na rede de casa (Android).** Abra no navegador do PC o endereço que aparece no celular, crie um PIN, clique em "Copiar" no cartão "Claude Code no seu PC" e cole o comando no PowerShell.

No Android, o leitor fica em "Ler QR code". No iPhone, aponte a Câmera para o código e toque no aviso.

**Instalando no iPhone (beta).** O Banditboard ainda não está na App Store. O IPA da versão vem sem assinatura: você instala
com o seu próprio Apple ID pelo [Sideloadly](https://sideloadly.io/) (Windows ou Mac) ou pelo [AltStore](https://altstore.io/).
Com um Apple ID gratuito, o app instalado assim para de abrir depois de 7 dias, então renove ou instale de novo uma vez por
semana; o pareamento continua. Depois, adicione os widgets: toque e segure a Tela de Início → Editar → Adicionar Widget →
Banditboard. Para o StandBy, deixe o iPhone carregando deitado e bloqueado, e adicione o Banditboard nas pilhas de widgets.

**Apple Watch.** O app do relógio vem dentro do app do iPhone. Se ele não instalar sozinho, abra o app Watch no iPhone e toque
em "Instalar" do lado do Banditboard. Abra o Banditboard no iPhone uma vez: ele passa o pareamento para o relógio, e dali em
diante o relógio busca o uso sozinho. Para colocar uma complicação, toque e segure o mostrador → Editar → Complicações →
Banditboard.

### Usando vários aparelhos

Conecte cada um uma vez. O hook guarda a lista de destinos em `~/.claude/clawdboard-targets.json` e manda para todos, então
conectar um aparelho novo nunca desconecta os outros.

> Depois das respostas, um hook do Claude Code roda o `/usage`, no máximo a cada 2 minutos, e isso não gasta tokens.
> O uso do claude.ai e de outros aparelhos também entra na conta, porque o `/usage` mostra o plano inteiro. O hook roda no
> Windows (PowerShell) e no macOS (sh); [o Linux está a caminho](../../issues/1).

## 🔐 Segurança

- Nenhum aparelho guarda token do Claude nem fala com a API da Anthropic. Quem lê os limites é o próprio Claude Code, no seu computador, com o `/usage`, e um script pequeno só repassa os números. O script não mexe nas credenciais do Claude Code.
- O script manda apenas as porcentagens, a hora em que cada limite libera e qual modelo cada sessão ativa nos últimos 10 minutos está usando ("opus", "sonnet"...). Nada das suas conversas, dos seus arquivos ou dos IDs das sessões.
- Cada envio leva uma chave de pareamento de 128 bits. O app que recebe compara com um hash SHA-256, e "Criar chave nova" invalida o comando antigo na hora.
- **Conectar de qualquer lugar.** A página /conectar cria as chaves no seu navegador: o código da caixa, uma chave de escrita, uma de leitura e uma chave mestra de 256 bits. O servidor do Banditboard (um Worker da Cloudflare) só recebe o hash SHA-256 das chaves de escrita e de leitura. O script cifra cada envio com AES-256-CBC e HMAC-SHA256, com chaves tiradas da chave mestra, que só o QR code e o comando carregam. O servidor guarda o último envio cifrado, a hora dele e um nível aproximado (0, 80, 90 ou 100 para a sessão e a semana) para saber quando acordar o app do Android, e não consegue ler os números. Os avisos do Android passam pelo Firebase Cloud Messaging com o mesmo conteúdo cifrado. Uma caixa que fica 45 dias sem receber nada é apagada.
- **QR code na rede de casa.** O compartilhamento fica desligado até você ligar no painel do Windows ou do Mac. Aí o app responde, só para leitura, na sua rede (portas 47830 a 47839), e só para quem manda a chave do QR code.
- No Android, a chave de pareamento fica criptografada com AES-256-GCM, com uma chave tirada do seu PIN (PBKDF2, 150.000 iterações) e protegida pelo Android Keystore. O PIN não fica salvo em lugar nenhum, e 10 erros seguidos apagam a chave de pareamento, o histórico e as configurações. O pareamento por QR code fica no armazenamento privado do app e não precisa de PIN.
- No iPhone e no Apple Watch, o pareamento fica no Keychain, compartilhado só com os widgets do próprio Banditboard.
- O painel web do Android só funciona dentro da sua rede, pede o mesmo PIN e só responde quando o endereço é um IP, `localhost` ou um nome `.local`, o que impede ataques de DNS rebinding. Entrar no painel também desbloqueia a tela do celular.
- O instalador faz um backup das configurações do Claude Code em `settings.json.antes-do-clawdboard`, adiciona dois hooks (`Stop` e `SessionStart`) e não mexe na sua status line.
- Os apps do Windows e do Mac só recebem envios em `127.0.0.1`, e mesmo assim conferem a chave de pareamento a cada envio.
- O modo música precisa de acesso às notificações porque é só assim que o Android mostra o player que está tocando. O Banditboard usa isso para ver e controlar o player; ele não lê as suas notificações.

**Leia os scripts antes de rodar.** Tudo o que roda no seu computador é curto, dá para conferir linha por linha:
o [`install.ps1`](app/src/main/assets/pc/install.ps1) e o [`install.sh`](app/src/main/assets/pc/install.sh) (gravam o hook e
mexem no `settings.json`), o [`usage.ps1`](app/src/main/assets/pc/usage.ps1) e o [`usage.sh`](app/src/main/assets/pc/usage.sh)
(o hook em si) e o [`connect.ps1`](desktop/src/main/resources/connect.ps1) (o que o app do Windows usa para rodar o
instalador). O servidor é o [`worker/src/index.js`](worker/src/index.js).

**Confira o download.** Cada versão traz o SHA-256 de cada arquivo. Compare com o do arquivo que você baixou:

```powershell
Get-FileHash .\Banditboard-1.14.0.msi -Algorithm SHA256
```

```sh
shasum -a 256 Banditboard-1.14.0.dmg
```

## 📱 Como ele é

### Android

| | | |
|---|---|---|
| <img src="prints/1.13.2/pt/01-dashboard.webp" alt="Painel"> | <img src="prints/1.13.2/pt/02-mascots.webp" alt="Tela dos mascotes"> | <img src="prints/1.13.2/pt/10-history.webp" alt="Histórico de 7 dias"> |
| Painel | Mascotes | Histórico de 7 dias |

<p>
  <img src="prints/1.13.2/pt/03-clock-portrait.webp" width="200" alt="Relógio de mesa com o celular em pé">
  <img src="prints/1.9.0/03-alerts.png" width="420" alt="Notificações: semana em 80% e sessão em 90%, com a hora em que liberam">
</p>

O relógio de mesa com o celular em pé e os avisos de limite (esse print está em inglês). Para os avisos chegarem com o app
fechado, o celular deixa uma notificação discreta enquanto espera o PC. Se não quiser os avisos, desligue em
Configurações → Tela.

### iPhone

<p>
  <img src="prints/1.14.0/pt/ios-dashboard.webp" width="200" alt="Painel do iPhone com sessão, semana e os quatro guaxinins">
  <img src="prints/1.14.0/pt/ios-dashboard-news.webp" width="200" alt="Painel do iPhone rolado até o status e as notícias">
  <img src="prints/1.14.0/pt/ios-settings.webp" width="200" alt="Ajustes do iPhone com guaxinim, skin, cor, idioma, avisos e pareamento">
</p>

<p>
  <img src="prints/1.14.0/pt/ios-home-widgets.webp" width="340" alt="Widgets médio e pequeno na Tela de Início com sessão, semana e o Racco">
  <img src="prints/1.14.0/pt/ios-notifications.webp" width="340" alt="Avisos de limite no iPhone">
</p>

O painel, os ajustes, os widgets da Tela de Início e os avisos. Os widgets também vêm nos tamanhos da Tela Bloqueada e aparecem
no StandBy. O iOS decide com que frequência os apps atualizam em segundo plano, então no iPhone um aviso pode chegar alguns
minutos depois.

### Apple Watch

<p>
  <img src="prints/1.14.0/pt/watch-session.webp" width="200" alt="Apple Watch com a sessão em 37% e a hora em que libera">
  <img src="prints/1.14.0/pt/watch-week.webp" width="200" alt="Apple Watch com a semana em 64%">
  <img src="prints/1.14.0/pt/watch-models.webp" width="200" alt="Apple Watch com os quatro guaxinins e o uso de cada modelo">
</p>

Gire a Coroa Digital para ir da sessão para a semana e os modelos. As complicações vêm nos formatos redondo, retangular, de
canto e em linha.

### Mac

<p>
  <img src="prints/1.14.0/pt/mac-menubar.webp" width="480" alt="O Racco e 37% na barra de menus do Mac, com o painelzinho aberto logo abaixo">
  <img src="prints/1.14.0/pt/mac-widget.webp" width="360" alt="Widget do Mac na mesa">
</p>

<img src="prints/1.14.0/pt/mac-dashboard.webp" width="600" alt="Painel do Mac com sessão, semana, modelos, status, notícias, ajustes do widget e o código para parear o iPhone">

Clique no Racco da barra de menus para abrir o painelzinho. Dali você liga ou desliga o widget na mesa, abre o painel ou
atualiza na hora. O painel tem todos os ajustes, o "Abrir ao iniciar sessão" e o código para parear o iPhone.

### Widget no Windows

<p>
  <img src="prints/1.13.2/pt/windows-widget.webp" width="480" alt="Widget do Windows com sessão, semana e os quatro guaxinins">
  <img src="prints/1.13.2/pt/windows-compact.webp" width="300" alt="Widget compacto">
  <img src="prints/1.13.2/pt/windows-mini.webp" width="96" alt="Só o Racco">
</p>

<img src="prints/1.13.2/pt/windows-dashboard.webp" width="600" alt="Painel do Windows com sessão, semana, modelos, status, notícias, gráfico de 7 dias, configurações do widget e créditos">

O widget tem três formatos, que você troca pelo ícone perto do relógio do Windows: completo, compacto ou só o Racco.
Dê dois cliques em qualquer widget, ou escolha "Painel e ajustes" no ícone, para abrir o painel: são os mesmos cartões do
painel web do celular, mais as configurações do widget e os créditos, com um QR code para baixar o app do celular. Dá para
arrastar o widget para onde quiser, e ele lembra o lugar. Também dá para tirar ele da barra de tarefas e fazer ele abrir junto
com o Windows. Quando toca música no PC, os guaxinins dançam. Se só uma sessão do Claude Code estiver aberta, o "Só o Racco" e
o widget compacto usam o acessório do modelo dela: óculos no Opus, cartola no Fable.

### Reações do Racco

| | | |
|---|---|---|
| <img src="prints/1.13.2/pt/04-sleeping.webp" alt="Guaxinins dormindo"> | <img src="prints/1.13.2/pt/05-sweating-88.webp" alt="Guaxinins suando em 88%"> | <img src="prints/1.13.2/pt/06-red-97.webp" alt="Guaxinins vermelhos em 97%"> |
| Sessão vazia: dormindo | 88%: suando | 97%: vermelhos e tremendo |
| <img src="prints/1.13.2/pt/07-burst-100.webp" alt="Guaxinins estourados em 100%"> | <img src="prints/1.13.2/pt/08-only-fable.webp" alt="Só o Fable estourado"> | <img src="prints/1.13.2/pt/09-dancing.webp" alt="Guaxinins dançando"> |
| 100%: estourados | Limite do Fable em 100%: só ele estoura | Tocando música: dançando |

Nesses prints o Opus aparece cinza, com olhos em X, porque os dados de exemplo trazem um problema em aberto no
status.claude.com.

### Modo música

<img src="prints/1.13.2/pt/11-music.webp" width="600" alt="Tela de música com capa, música, controles, volume e guaxinins dançando">

## 🛠️ Problemas comuns

- **O Windows diz "O Windows protegeu o computador".** O instalador ainda não tem assinatura digital. Clique em "Mais informações" e depois em "Executar assim mesmo". Se quiser ter certeza de que é o arquivo certo, compare o [SHA-256](#-segurança) com o da versão.
- **O macOS diz que não conseguiu verificar o Banditboard.** O app ainda não tem a notarização da Apple. Vá em Ajustes do Sistema → Privacidade e Segurança e clique em "Abrir Mesmo Assim". Só precisa fazer isso uma vez.
- **O Android não deixa instalar o APK.** Ele vem do GitHub, e não da Play Store, então o Android pede para permitir "instalar apps desconhecidos" no navegador ou no gerenciador de arquivos que abriu o arquivo.
- **O app do iPhone parou de abrir.** Com um Apple ID gratuito, o app instalado por fora da App Store dura 7 dias. Renove no AltStore ou instale o IPA de novo pelo Sideloadly; o pareamento continua.
- **"Configuração restrita" ao ligar o modo música.** No Android 13 ou mais novo, um APK instalado pelo navegador ou por um gerenciador de arquivos não recebe acesso às notificações de cara. Vá em Configurações → Apps → Banditboard → ⋮ → Permitir configurações restritas e tente de novo.
- **O celular parou de atualizar depois que o roteador reiniciou.** No pareamento por PIN, provavelmente o celular mudou de IP. A tela e o rodapé do painel sempre mostram o endereço atual: abra esse endereço no PC e rode o comando de novo. Para não ter esse problema, reserve um IP fixo para o celular no roteador, ou conecte de qualquer lugar pelo site.
- **O Windows não consegue conectar o Claude Code.** O widget mostra o motivo, e o "O que fazer" abre o painel com o próximo passo e o erro exato do PowerShell. Os detalhes ficam em `%APPDATA%\Banditboard\conectar.log` (sem a chave de pareamento), e dá para mandar esse arquivo numa [issue](../../issues).
- **Os números não mudam.** O hook roda depois das respostas do Claude Code, no máximo a cada 2 minutos. Para buscar na hora, use "Atualizar agora" no painel do Windows ou do Mac, no menu da bandeja ou no painelzinho da barra do Mac.

## 🧭 Próximos passos

- [Linux: uma versão do hook em shell](../../issues/1)
- iPhone e Apple Watch no TestFlight e na App Store, com avisos por push
- App para Wear OS, no Galaxy Watch e nos outros relógios com Android
- [Android 16: passar para a API 36](../../issues/4)

## 📚 Documentação

- [Como funciona](docs/como-funciona.md): o caminho dos dados, cada configuração e de onde vêm os números
- [Desenvolvimento](docs/desenvolvimento.md): como compilar, testar, diagnosticar pelo ADB e como o código está organizado
- [Como contribuir](CONTRIBUTING.md)

## 🤝 Quer ajudar?

Issues e pull requests são bem-vindos. Para mudanças maiores, abra uma issue antes para a gente conversar. O
[CONTRIBUTING.md](CONTRIBUTING.md) explica como o projeto está organizado e onde ficam as explicações.

Dúvidas e ideias vão para as [Discussions](../../discussions); problemas, para as [Issues](../../issues).

## 📄 Licença e aviso

Licenciado sob a [licença MIT](LICENSE). A fonte Fredoka é distribuída pela SIL Open Font License, e o texto da licença vai
dentro do APK, em `assets/licenses/`.

O Banditboard é um projeto pessoal de fã, **sem vínculo, apoio ou patrocínio da Anthropic**. Claude e Claude Code são marcas da
Anthropic, PBC. Android é marca do Google LLC; iPhone, Apple Watch, macOS e StandBy são marcas da Apple Inc.; Windows é marca da
Microsoft Corporation.

Feito com ❤️ por [Vinícius Pires da Silva](https://www.linkedin.com/in/viniciuspiresdasilva/).

**Gostou? Deixe uma ⭐**
