# Como o Banditboard funciona

<p align="right"><a href="how-it-works.md">English</a> · <b>Português</b></p>

[← Voltar para o README](../README.pt-BR.md)

## O caminho dos dados

```
 Claude Code no seu PC (VS Code ou terminal)
          │  hook Stop / SessionStart, no máximo a cada 2 minutos
          ▼
 clawdboard-usage.ps1 ──► claude -p "/usage"   (comando local, sem tokens, com os hooks desligados)
          │
          └──► POST para cada destino do clawdboard-targets.json   (chave de pareamento, só os números)
                 ├──► http://IP-DO-CELULAR:8080/api/push
                 └──► http://127.0.0.1:47810/api/push   (app do Windows)

 ┌──────────────────┐
 │ Celular Android  │ ──► status.claude.com    problemas em aberto
 │   Banditboard    │ ──► feed RSS público     notícias da Anthropic
 └──────────────────┘
          ├──► 🦝 guaxinins na tela do celular
          └──► painel web na sua rede (http://IP-DO-CELULAR:8080)

 O modo música só lê e controla o player do próprio celular. Não precisa de conta nenhuma.
```

## Configuração

**Primeira vez.** O celular mostra o endereço dele, algo como `http://192.168.0.15:8080`. A porta é a primeira livre entre
8080 e 8089, e o PC e o celular precisam estar no mesmo Wi-Fi. Se preferir, crie o PIN no próprio celular, em "Prefiro criar
o PIN neste celular", e conecte o Claude Code depois pelo painel. Quando o roteador reinicia, o celular pode mudar de IP; a
tela e o rodapé do painel sempre mostram o endereço atual. Para não ter esse problema, reserve um IP fixo para o celular no
roteador. Se o IP mudar, é só rodar o comando do painel de novo.

**Conectando o Claude Code.** O comando baixa um instalador do celular. Ele salva o `~/.claude/clawdboard-usage.ps1`, coloca
o celular na lista `~/.claude/clawdboard-targets.json` e registra o script como hook `Stop` e `SessionStart` no
`~/.claude/settings.json`. O hook termina na hora e, no máximo a cada 2 minutos, roda escondido um
`claude -p "/usage" --no-session-persistence` com os hooks desligados, pega as linhas da sessão, da semana e de cada modelo e
envia. Ele usa o `claude` que estiver no PATH ou o que vem com a extensão do VS Code. Para desfazer, volte o
`settings.json.antes-do-clawdboard` ou apague os dois hooks `clawdboard-usage`. Se a hora de liberar passa e nenhum envio
novo chega, o celular zera aquela janela sozinho. Os scripts ficam em [`app/src/main/assets/pc/`](../app/src/main/assets/pc/).

**No celular.** Arraste para a esquerda para ver a próxima tela e para a direita para voltar. Fora do carrossel, ele volta
para a tela inicial depois de 30 segundos. A engrenagem no canto de baixo abre as configurações, depois de pedir o PIN.
Quando o celular ou o app reinicia, a tela pede o PIN de novo, e dá para desbloquear pelo painel web. O rodapé mostra quando
chegou o último envio.

**Telas e modos.** Painel, mascotes (sessão e semana em cima, os quatro Raccos embaixo), gráfico de 7 dias, notícias, relógio
de mesa e música, se estiver ligada. A tela pode ficar fixa, nos mascotes, no carrossel ou no relógio. No carrossel, a tela de
música só aparece quando algo está tocando.

**Barra de cada modelo.** Quando o `/usage` mostra um limite semanal só de um modelo (hoje só o Fable, em alguns planos), a
barra dele fica colorida. Os outros modelos usam o limite semanal geral, por isso aparecem em cinza.

**Visuais.** No "Por modelo", que é o padrão, o Fable, o mais caro, ganha uma cartola, o Opus usa óculos, o Sonnet usa fone e
o Haiku tem um broto na cabeça. Também tem o "Clássico" (sem acessório), "Coroas" e "Natal", e cinco cores: natural (o cinza
do guaxinim), arco-íris (uma cor para cada modelo), lavanda, menta e chiclete. O painel web desenha o mesmo visual com a
definição que o app manda em `state.look`.

**Animações.** Além de piscar, eles olham para os lados, mexem as pernas, acenam, mexem as orelhas e se abaixam. Com a sessão
de 5 horas zerada, eles dormem (olhos fechados e um Z). Em 85% começam a suar e ficam agitados; em 90% ficam vermelhos e
pulsam; em 95% tremem. Em 100% estouram e ficam queimados, com olhos em X e fumaça, e aparece "esgotado". Se a sessão ou a
semana geral chegam em 100%, os quatro estouram; se só o limite do Fable chega em 100%, só ele estoura. Quando o
status.claude.com tem um problema que cita um modelo, o guaxinim dele fica cinza, com olhos em X. Dá para desligar as
animações nas configurações.

**Música.** Vá em Configurações → Música → "Tela de música", toque em "Dar acesso" e permita o Banditboard. Ele não toca nada
sozinho: só lê e controla o player do app que está tocando. A tela mostra a capa, a música, o ícone do app (um toque abre o
player, para trocar de playlist), a barra de progresso, os botões, os botões extras do app e o volume, que é o volume de mídia
do celular ou, se o som estiver saindo em outro aparelho, o volume desse aparelho. Enquanto a música toca, todos os guaxinins
dançam em todas as telas, inclusive no painel web: os que estavam dormindo acordam, os suados dançam suando e os estourados
batem o pé. Se você instalou o APK pelo navegador ou por um gerenciador de arquivos, o Android 13 ou mais novo pode avisar que
é uma "configuração restrita". Nesse caso, vá em Configurações → Apps → Banditboard → ⋮ → Permitir configurações restritas e
tente de novo.

**Zoom e modo compacto.** 90, 100, 115, 130 ou 150% nas telas e nas configurações; as telas de bloqueio e de PIN ficam no
tamanho do sistema. Quando o lado menor da tela passa a ter menos de 380dp (zoom alto ou celular pequeno), as telas mudam para
um layout compacto e escondem as linhas menos importantes.

**Fundo.** O "Tema padrão" usa tons escuros e quentes (#16130f, com um brilho coral embaixo). O "Preto AMOLED" economiza mais
a tela.

**Feedback.** Depois de 3 dias de uso, aparece um cartão perguntando se você está gostando, com um botão que abre um e-mail
para o autor. Ele some sozinho depois de 30 segundos, volta a cada 10 dias e tem "Não mostrar de novo". Se você mandar um
e-mail, ele só volta depois de 60 dias.

**Abrir quando o celular ligar.** Precisa de uma permissão que só o ADB consegue dar. Sem ela, o app funciona normal, só não
abre sozinho depois de reiniciar:

```powershell
adb shell appops set dev.clawdboard SYSTEM_ALERT_WINDOW allow
```

**Atualizando.** É só instalar o APK novo por cima do antigo: o PIN, as configurações e o histórico continuam, desde que o APK
seja assinado com a mesma chave. Depois de atualizar, o app pede o PIN uma vez. Quem vem da 1.4 ou de antes troca o token do
Claude salvo por uma chave de pareamento no primeiro desbloqueio; depois disso, conecte o Claude Code pelo painel.

## De onde vêm os dados

- **Uso:** a saída do `claude -p "/usage"`, um comando local do Claude Code que não chama nenhum modelo. Ele lê as linhas "Current session", "Current week (all models)" e "Current week (<modelo>)", com a hora em que cada uma libera.
- **Status:** `https://status.claude.com/api/v2/incidents/unresolved.json`
- **Notícias:** o feed RSS público `Olshansk/rss-feeds`.
