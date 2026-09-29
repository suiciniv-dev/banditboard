# How Banditboard works

<p align="right"><b>English</b> · <a href="como-funciona.md">Português</a></p>

[← Back to the README](../README.md)

## Data flow

```
 Claude Code on your PC (VS Code or terminal)
          │  Stop / SessionStart hook, at most every 2 minutes
          ▼
 clawdboard-usage.ps1 ──► claude -p "/usage"   (local command, no tokens, hooks off)
          │
          └──► POST to each destination in clawdboard-targets.json   (pairing key, only the numbers)
                 ├──► http://PHONE-IP:8080/api/push
                 └──► http://127.0.0.1:47810/api/push   (Windows app)

 ┌──────────────────┐
 │  Android phone   │ ──► status.claude.com    open incidents
 │   Banditboard    │ ──► public RSS feed      Anthropic news
 └──────────────────┘
          ├──► 🦝 raccoons on the phone screen
          └──► web dashboard on your local network (http://PHONE-IP:8080)

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

## Where the data comes from

- **Usage:** the output of `claude -p "/usage"`, a local Claude Code command that makes no model call: the "Current session", "Current week (all models)" and "Current week (<model>)" lines, with their reset times.
- **Status:** `https://status.claude.com/api/v2/incidents/unresolved.json`
- **News:** the public RSS feed `Olshansk/rss-feeds`.
