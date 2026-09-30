# Contributing to Banditboard

<p align="right"><b>English</b> · <a href="#contribuindo-com-o-banditboard">Português</a></p>

Thanks for wanting to help! Bug reports, ideas, translations and pull requests are all welcome.

## Before you start

- **Bugs** go to [Issues](../../issues). On Windows, attach `%APPDATA%\Banditboard\conectar.log` if the problem is connecting Claude Code (it never contains the pairing key).
- **Questions and ideas** go to [Discussions](../../discussions).
- **Bigger changes**: open an issue first so we can agree on the approach before you spend time on it. The issues are written in Portuguese, but English is fine.

## Where the explanations live

The code has no comments, on purpose. Instead of comments that drift away from the code, the explanations live in a few
places that are easy to find and keep up to date:

- [docs/how-it-works.md](docs/how-it-works.md): the data flow, every setting and every screen, in plain language
- [docs/development.md](docs/development.md): building, testing, the ADB diagnostics and how the code is organized
- the [README](README.md): what the app does, security and troubleshooting
- commit messages and pull request descriptions: why a change was made

If your change needs explaining, update one of these docs in the same pull request (in English and Portuguese if you can;
if not, write it in one of them and I'll translate). Please don't add code comments.

## Making a change

1. Build and run the tests: `.\gradlew.bat testReleaseUnitTest assembleDebug` (see [docs/development.md](docs/development.md)).
2. Try it on a phone, on Windows or on a Mac. The Android preview build (`dev.clawdboard.preview`) installs next to the real app and accepts sample data, so you don't need a real Claude Code session to test the screens. The iPhone and Apple Watch apps take `--demo` in the simulator for the same purpose.
3. The app's text lives in `core/Texts.kt`, in English and Brazilian Portuguese. Add both when you add a string.
4. Keep the existing internal names (`dev.clawdboard`, `clawdboard-usage.ps1`, `X-Clawdboard`): changing them would break existing installs.
5. Don't add Anthropic logos or artwork. The mascot is Racco, Banditboard's own raccoon.

By contributing, you agree that your contribution is licensed under the [MIT License](LICENSE).

---

# Contribuindo com o Banditboard

Valeu por querer ajudar! Relatos de bug, ideias, traduções e pull requests são bem-vindos.

## Antes de começar

- **Problemas** vão para as [Issues](../../issues). No Windows, se o problema for conectar o Claude Code, mande junto o `%APPDATA%\Banditboard\conectar.log` (ele nunca traz a chave de pareamento).
- **Dúvidas e ideias** vão para as [Discussions](../../discussions).
- **Mudanças maiores**: abra uma issue antes, para a gente combinar o caminho antes de você gastar tempo.

## Onde ficam as explicações

O código não tem comentários, de propósito. Em vez de comentários que vão ficando desatualizados, as explicações ficam em
poucos lugares, fáceis de achar e de manter em dia:

- [docs/como-funciona.md](docs/como-funciona.md): o caminho dos dados, cada configuração e cada tela, em linguagem simples
- [docs/desenvolvimento.md](docs/desenvolvimento.md): como compilar, testar, o diagnóstico pelo ADB e a organização do código
- o [README](README.pt-BR.md): o que o app faz, segurança e problemas comuns
- as mensagens de commit e as descrições de pull request: o porquê de cada mudança

Se a sua mudança precisa de explicação, atualize um desses documentos no mesmo pull request (em português e em inglês, se
puder; se não, escreva num deles que eu traduzo). Por favor, não coloque comentários no código.

## Fazendo uma mudança

1. Compile e rode os testes: `.\gradlew.bat testReleaseUnitTest assembleDebug` (veja [docs/desenvolvimento.md](docs/desenvolvimento.md)).
2. Teste no celular, no Windows ou no Mac. A versão de teste do Android (`dev.clawdboard.preview`) fica ao lado do app de verdade e aceita dados de exemplo, então não precisa de uma sessão real do Claude Code para testar as telas. Os apps do iPhone e do Apple Watch aceitam o `--demo` no simulador para a mesma coisa.
3. Os textos do app ficam em `core/Texts.kt`, em português e em inglês. Ao criar um texto, coloque os dois.
4. Mantenha os nomes internos (`dev.clawdboard`, `clawdboard-usage.ps1`, `X-Clawdboard`): mudar quebraria a instalação de quem já usa.
5. Não use logos nem artes da Anthropic. O mascote é o Racco, o guaxinim do próprio Banditboard.

Ao contribuir, você concorda que a sua contribuição fica sob a [licença MIT](LICENSE).
