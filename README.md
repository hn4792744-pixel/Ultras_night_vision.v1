# Ultras_NightVision

A clean, persistent Night Vision toggle for Paper servers. Author: **UC_Hussein**.

## ⚠️ Build status

This has been checked structurally (balanced braces, valid YAML, every message key used in Java exists in
both `messages/en.yml` and `messages/ar.yml`) but **has not been run through an actual Gradle/Java compiler**
in the environment this was written in (no internet/JDK access there). Please run the build yourself:

```
gradle wrapper      # once, to generate ./gradlew
./gradlew clean build
```

Requires JDK 21 (matches Paper 1.21.x). The jar is written to `build/libs/UltrasNightVision-1.0.0.jar`.
If the first build errors, it's most likely a small typo a compiler catches instantly - happy to fix it
if you paste the error back.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/nv` | Toggle your own Night Vision | `ultras.nv.use` (default: everyone) |
| `/nv <player>` | Toggle Night Vision for another player | `ultras.nv.others` (default: op) |
| `/nv all` | Toggle Night Vision for every online player | `ultras.nv.all` (default: op) |
| `/nv reload` | Reload config.yml + the active language file | `ultras.nv.admin` (default: op) |

Alias: `/nightvision`. Full tab-completion, filtered to what the sender can actually use.

## Permissions

- `ultras.nv.use` - default `true`
- `ultras.nv.others` - default `op`
- `ultras.nv.all` - default `op`
- `ultras.nv.admin` - default `op`

All four permission node names are themselves configurable in `config.yml` under `permissions:`, in case
they clash with another plugin on your server.

## Behaviour

- Night Vision is applied with **no particles and no HUD icon** (`night-vision.hide-particles` /
  `hide-icon`, both true by default) - just the vision effect, nothing else.
- **Persists** across logout/login and death/respawn: the enabled/disabled state is saved to
  `players.yml` (async writes, never blocks the main thread) and re-applied on join and on respawn.
- **Self-healing**: a periodic check (`night-vision.refresh-check-interval-seconds`, default 15s)
  re-applies the effect if anything removed it, and an `EntityPotionEffectEvent` listener catches
  removals (e.g. a milk bucket, another plugin) immediately rather than waiting for the next check.
- **No duplicate effects**: re-application is skipped whenever the player already has a fresh-enough
  effect at the right amplifier, so it never stacks or spams `addPotionEffect` calls.
- Action bar shows `ɴɪɢʜᴛ ᴠɪsɪᴏɴ: ᴏɴ` / `ɴɪɢʜᴛ ᴠɪsɪᴏɴ: ᴏғғ` (no server name, ever) for a configurable
  duration (`action-bar.duration-seconds`) by resending it every `action-bar.resend-interval-ticks`.
- Every chat message is prefixed with the ULTRAS gradient (`<gradient:#ff1f1f:#8b0000>ULTRAS</gradient> |`),
  configurable once in `config.yml` and shared by every message via the `%prefix%` placeholder.
- Two distinct, soft sounds for enable/disable (`sounds.enable` / `sounds.disable`), fully configurable
  (sound name, volume, pitch) and toggleable as a whole via `sounds.enabled`.
- A small per-player cooldown (`cooldown-seconds`, default 2) stops a player from spamming their own
  `/nv` toggle; it does not affect staff using `/nv <player>` or `/nv all`.

## Configuration

Everything is in `config.yml` (command name/aliases, permission node names, cooldown, Night Vision
amplifier/duration/refresh timing, persistence file/interval, action-bar text timing, sounds,
the ULTRAS prefix, and target/all notification toggles) plus `messages/en.yml` and `messages/ar.yml`
for every piece of chat text. Add your own `messages/<code>.yml` and set `language: <code>` in
`config.yml` to support another language - no Java changes needed.

## Architecture

- `config/ConfigService` - typed, reload-safe access to `config.yml` + the active language file
- `message/MessageService` - MiniMessage rendering (gradients/colors) and the action-bar duration loop
- `storage/PlayerStorage` - async-safe YAML persistence of the enabled-player set
- `service/NightVisionService` - single source of truth: effect apply/remove, self-healing, cooldowns
- `command/NvCommand` - the one command executor + tab completer for every sub-behaviour
- `listener/*` - join/quit, respawn, and external potion-effect-removal handling
- `util/ToggleLogic` - the pure, unit-tested decision behind `/nv all`'s on/off choice
