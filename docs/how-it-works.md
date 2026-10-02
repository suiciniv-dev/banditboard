# How Banditboard works

<p align="right"><b>English</b> · <a href="como-funciona.md">Português</a></p>

[← Back to the README](../README.md)

## Data flow

```
 Claude Code on your PC or Mac (VS Code or terminal)
          │  Stop / SessionStart hook, at most every 2 minutes
          ▼
 clawdboard-usage.ps1 (Windows) or clawdboard-usage.sh (macOS)
          │  claude -p "/usage"   (local command, no tokens, hooks off)
          │
          └──► POST to each destination in clawdboard-targets.json   (pairing key, only the numbers)
                 ├──► http://PHONE-IP:8080/api/push                  (Android, PIN pairing)
                 ├──► http://127.0.0.1:47810/api/push                (Windows or Mac app)
                 └──► https://banditboard-api.vinips00.workers.dev/v1/box/ID/push
                        (encrypted end to end; Firebase wakes the Android app)

 Windows or Mac app ──► shares read-only on your home network, when you turn it on (ports 47830-47839)

 Antigravity open on the same PC ──► the Windows app reads its quota on 127.0.0.1 every 15 s
          └──► goes along with the usage, to the same destinations

 Readers:
   Android phone   ── PIN pairing, QR code from the Windows/Mac app, or QR code from /conectar
   iPhone          ── QR code from the Windows/Mac app, or QR code from /conectar
   Apple Watch     ── gets the pairing from the iPhone, then fetches by itself
   Windows / Mac   ── receive the hook directly

 Every app also reads:
   status.claude.com     open incidents
   public RSS feed       Anthropic news

 Music mode reads and controls the phone's own media session. No account involved.
```

## Configuration

**First setup.** The phone shows its address, something like `http://192.168.0.15:8080`. The port is the first free one between
8080 and 8089, and the PC and the phone must be on the same Wi-Fi. You can also create the PIN on the phone itself through
"I'd rather create the PIN on this phone" and connect Claude Code later from the dashboard. If the router restarts the phone may get a
new IP; the screen and the dashboard footer always show the current one. Reserve a fixed IP for the phone in the router's DHCP
settings to avoid that; if the IP changes, run the command from the dashboard again.

**Connecting Claude Code.** The command downloads an installer from the phone. It writes `~/.claude/clawdboard-usage.ps1`,
puts the destination in `~/.claude/clawdboard-targets.json` and adds it as a `Stop` and `SessionStart` hook in
`~/.claude/settings.json`. The hook returns right away and, at most every 2 minutes, starts a hidden background run of
`claude -p "/usage" --no-session-persistence` with hooks turned off, reads the session, weekly and per-model lines and sends
them. It uses `claude` from your PATH or the copy bundled with the VS Code extension. To undo it, restore
`settings.json.antes-do-clawdboard` or remove the two `clawdboard-usage` hooks. When a window's reset time passes without a
new push, the phone drops it to 0% on its own. The scripts are in [`app/src/main/assets/pc/`](../app/src/main/assets/pc/).

**On the phone.** Swipe left for the next screen and right to go back. Outside the carousel, it returns to the home screen after
30 seconds. The gear in the bottom right corner opens the settings after asking for the PIN. After a reboot or an app restart
the screen asks for the PIN again, and you can unlock it from the web dashboard. The footer shows when the last push arrived.

**Screens and modes.** Dashboard, mascots (session and week on top, the four Raccos below), 7-day chart, news, desk clock and
music (when enabled). Screen modes: static, mascots, carousel or clock. In the carousel the music screen only shows up while
something is playing.

**Per-model bar.** When `/usage` reports a model's own weekly limit (today only Fable, on some plans), that model's bar is colored.
The other models draw from the general weekly limit, so they show that value in gray.

**Skins.** "Per model" (the default) puts a top hat on Fable, the most expensive one, glasses on Opus, headphones on Sonnet and
a sprout on Haiku. There are also "Classic" (no accessory), "Crowns" and "Christmas", and five colors: natural (the raccoon's
gray), rainbow (one per model), lavender, mint and bubblegum. The web dashboard draws the same skin from the definition the app
sends in `state.look`.

**Animations.** Besides blinking, they look around, move their legs, wave, twitch their ears and crouch. With the 5-hour session
at zero they sleep (eyes closed and a Z). From 85% they sweat and get restless; from 90% they turn red and throb, and from 95%
they shake. At 100% they burst and stay charred, with X eyes and smoke, marked "maxed out". Session or general week at 100%
bursts all four; Fable's own limit at 100% bursts only Fable. An open incident on status.claude.com that names a model turns its
raccoon gray with X eyes. Animations can be turned off in the settings.

**Music.** Settings → Music → "Music screen", then "Grant access" and allow Banditboard. Banditboard plays nothing itself: it reads
and controls the player of the app that is playing, through Android's media session. The screen shows the album cover, the track,
the icon of the app (one tap opens its player, to change playlists), the progress bar, the buttons, the app's extra buttons and the
volume: the phone's media volume or, when the app sends the sound to another device, that device's volume. While music plays, every
raccoon dances on every screen, the web dashboard included: sleepy ones wake up, sweaty ones dance sweating and burst ones tap a foot.
On an APK installed through a browser or file manager, Android 13 and later may say it is a "restricted setting". In that case:
Settings → Apps → Banditboard → ⋮ → Allow restricted settings, and try again.

**Zoom and compact mode.** 90, 100, 115, 130 or 150% on the screens and in the settings; the lock and PIN screens keep the system
size. When the shorter side of the screen drops below 380dp (high zoom or a small phone), the screens switch to a compact layout
and hide secondary lines.

**Background.** "Default theme" uses warm dark tones (#16130f with a coral glow at the bottom). "AMOLED black" saves more screen.

**Feedback.** After 3 days of use a card asks whether you are enjoying the app, with a button that opens an email to the author.
It hides itself after 30 seconds, comes back every 10 days and has "Don't show again". After you send an email it only returns
in 60 days.

**Open on boot.** It needs a permission only ADB can grant. Without it the app works normally, it just does not open by itself
after a reboot:

```powershell
adb shell appops set dev.clawdboard SYSTEM_ALERT_WINDOW allow
```

**Updating.** Installing a new APK over the old one keeps the PIN, settings and history, as long as it is signed with the same
key. The app asks for the PIN once after the update. Coming from 1.4 or older, the first unlock replaces the stored Claude token
with a pairing key; then connect Claude Code from the dashboard.

## Mac

The Mac app lives in the menu bar: Racco and the session percentage next to the clock, drawn in white or black to follow the
bar. A click opens a small panel with the session, the week, the four Raccos, the "Desktop widget" switch and the links to the
dashboard, "Refresh now" and "Quit". The widget on the desk and the dashboard are the same as on Windows, without the "Just Racco"
layout, since the menu bar already does that job. "Open at login" in the dashboard writes a LaunchAgent at
`~/Library/LaunchAgents/dev.clawdboard.banditboard.plist`. The data lives in `~/Library/Application Support/Banditboard`, and the
alerts are macOS notifications (allow them in System Settings → Notifications → Banditboard). "Connect Claude Code" installs
`~/.claude/clawdboard-usage.sh`, the POSIX sh version of the hook, which only uses tools that come with macOS (`openssl`,
`plutil`, `osascript`).

## Connecting from anywhere

[banditboard.pages.dev/conectar](https://banditboard.pages.dev/conectar/) creates a "box" on the Banditboard server, a Cloudflare
Worker with a D1 database. The browser generates four random values: the box ID, a write key, a read key and a 256-bit master
key. Only the ID and the SHA-256 hashes of the two keys go to the server.

- The **QR code** (`banditboard://box?...`) carries the server address, the ID, the read key and the master key. The phone uses it to read.
- The **command** for Windows or macOS carries the ID, the write key and the master key. It installs the hook with a new destination, or adds that destination if the hook is already there.

Each push is encrypted on the computer: AES-256-CBC with a random IV, then HMAC-SHA256 over the IV and the ciphertext, with the
encryption and MAC keys derived from the master key with HMAC ("banditboard-enc" and "banditboard-mac"). Along with it goes a
coarse level, the bucket (0, 80, 90 or 100) of the session and of the week, which is the only thing the server can read. When that
level changes, the server sends the ciphertext to the registered Android phones through Firebase Cloud Messaging; the phone
decrypts it and shows the alert, even with the app closed. A box that receives nothing for 45 days is deleted, along with its
phones.

## QR code on the home network

On the Windows or Mac dashboard, "Share usage with the iPhone on your home network" starts a small read-only server on the first
free port between 47830 and 47839. It answers `GET /api/usage` only with the key from the QR code (`banditboard://pair?...`), which
also lists the computer's local addresses. Both the iPhone and Android can scan it. Turning the switch off stops the server.

## iPhone and Apple Watch

The iPhone app has the dashboard (session, week, the four Raccos, status and news), the settings (raccoon, skin, tint, language,
alerts and pairing) and widgets: small and medium on the Home Screen, circular and rectangular on the Lock Screen, and the small
one in StandBy. Pairing happens by scanning a QR code with the Camera. The app asks iOS to refresh in the background about
every 15 minutes and sends the alerts as local notifications, so the timing depends on iOS. The widgets fetch the usage
themselves and schedule their next refresh around the next reset.

The Apple Watch app gets the pairing from the iPhone through WatchConnectivity once, and from then on fetches by itself, over the
home network or through the server. It has three pages (session, week and models) and complications in four shapes: circular,
rectangular, corner and inline.

## Claude activity

Turning on "Show what Claude Code is doing" on the Windows or Mac dashboard reruns the installer with the activity hooks: one
async command hook on `SessionStart`, `UserPromptSubmit`, `PreToolUse`, `PostToolUse`, `PostToolUseFailure`, `PermissionRequest`,
`PermissionDenied`, `Notification`, `Stop`, `StopFailure` and `SessionEnd`. Each one pipes the event Claude Code gives it to
`curl`, which posts it to `http://127.0.0.1:<port>/api/hook` with the pairing key. `curl` is called by its full path, gives up
after half a second when the app is closed and always exits with 0, so Claude Code never waits or shows a hook error. Turning
the option off removes only these hooks.

The app (`core/Activity.kt`) turns the events into one state per session:

| Event | State |
|---|---|
| A prompt | Working ("Thinking") |
| Edit, Write | Working ("Editing <file>") |
| Read, Grep, Glob | Working ("Reading") |
| Bash | Running command: tests, build or other, guessed from the command without keeping it |
| Permission request | Waiting for permission |
| AskUserQuestion, plan approval | Asking question |
| Tool finished, permission answered | Working |
| Stop | Finished, and Idle after 3 minutes |
| StopFailure | Error |
| No event for 10 minutes, or idle notification | Idle |

The project is the name of the folder that holds `.git`, and the branch is read from `.git/HEAD` (worktrees included), without
running `git`. With several sessions, the most urgent one wins: question, permission, error, command, working, finished, idle.
Each session's model decides which Racco reacts. When a question or a permission is still waiting after 6 seconds, the computer
shows a notification.

The app sends only `{"activity": {"at", "sessions": [{project, branch, model, state, doing, file, since, seen}]}}` to the other
devices, together with the latest usage and `usage_at`, at most every 3 seconds: straight to the phone paired by PIN, in the
answer of the home network sharing and encrypted to the server box. `file` is empty unless you turn it on. With the activity
arriving, the phone fetches every 5 seconds on the home network and every 20 seconds through the server, and it also notifies
after 6 seconds. Activity older than 15 minutes is ignored.

## Antigravity

Banditboard for Windows also follows Google Antigravity. When Antigravity is open, its language server listens on a random
local port. Banditboard finds it through `%APPDATA%\Antigravity\logs\language_server.log` (the process ID and the ports) and,
as a fallback, by looking for a `language_server` process. It reads the local token that Antigravity passes on that process's
command line and keeps it only in memory.

Every 15 seconds it asks the language server, on `127.0.0.1`, for `GetUserStatus` (the plan and the model list) and
`RetrieveUserQuotaSummary` (the groups and their windows), the same calls the Antigravity window uses for View Usage. The
answer usually comes from Antigravity's own cache. Banditboard asks Antigravity to refresh it from Google when the log shows new
activity, every 5 minutes, and when you click the widget.

The quota comes in groups: Gemini (Pro and Flash) and Claude and GPT. Each group has a weekly window and, on plans that have
one, a 5-hour window. Each Racco shows the tightest window of its group.

The phone gets only the percentages, the reset times, the plan name and whether Antigravity is open, in the same envelope as
the Claude Code usage. The alert level that tells the server when to wake the Android app gets four more digits for
Antigravity. The desktop app writes them to `~/.claude/clawdboard-ag.txt` and the hook adds them to its own level, so both
always send the same one. When you turn Antigravity off, the phone hides it.

On Android, an alert at 80%, 90% or 100% also leaves a reminder for the reset time, so the "reset" alert shows up on time even
with the computer off and the app closed.

If Antigravity runs as administrator, Windows hides its command line from a normal process, and the widget says so. Tools →
Export Antigravity diagnostics saves a text file to Downloads with the whole check, without tokens, passwords or e-mail.

## Where the data comes from

- **Usage:** the output of `claude -p "/usage"`, a local Claude Code command that makes no model call: the "Current session", "Current week (all models)" and "Current week (<model>)" lines, with their reset times.
- **Antigravity:** `GetUserStatus` and `RetrieveUserQuotaSummary` from the Antigravity language server, on `127.0.0.1`.
- **Status:** `https://status.claude.com/api/v2/incidents/unresolved.json`
- **News:** the public RSS feed `Olshansk/rss-feeds`.
