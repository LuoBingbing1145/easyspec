# EasySpec

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-orange)
![Fabric](https://img.shields.io/badge/loader-Fabric-dbd0b4)
![License](https://img.shields.io/badge/license-CC0--1.0-blue)

**EasySpec** is a lightweight, server-side-only Fabric mod that lets players toggle **Spectator Mode** instantly by typing a configurable trigger word in chat. No commands, no permissions, no client-side installation required — just type and go.

> ⚡ Type `!s` (configurable) in chat to enter spectator mode. Type it again to return to exactly where you were.

---

## Features

- **One-key toggle** — Type `!s` (or your custom trigger) to switch to spectator; type it again to restore.
- **Full state preservation** — Your game mode (survival/creative/adventure), position (x/y/z), rotation (yaw/pitch), and **dimension** are all saved and restored.
- **Fully server-side** — No client mod needed. Works with vanilla clients.
- **Silent trigger** — The trigger message is intercepted and never broadcast to other players.
- **Multilingual** — 9 languages supported; messages show the actual trigger word you configured.
- **Config auto-repair** — Missing or invalid config fields are automatically reset with a warning, keeping your settings intact.
- **ModMenu support** — Description translation key included for ModMenu compatibility.
- **Thread-safe** — Safe for use with chat plugins and proxy environments (Velocity, BungeeCord).

---

## How It Works

1. Player types `!s` (or configured trigger) in chat.
2. The server intercepts the message, cancels it (other players don't see it), and saves the player's current state.
3. Player is switched to spectator mode.
4. Typing `!s` again restores the saved state — game mode, position, rotation, and dimension.

The whole mod runs on the **server thread** via `server.execute(...)`, so it's safe with Netty-based chat handling.

---

## Installation

1. Install **Fabric Loader** (≥0.19.2) on your server.
2. Drop the `easyspec-1.1.0.jar` into your server's `mods/` folder.
3. Restart the server. That's it — clients need nothing.

### Dependencies

| Dependency                                  | Version | Required      |
|---------------------------------------------|---------|---------------|
| [Fabric Loader](https://fabricmc.net/use/)  | ≥0.19.2 | ✅            |
| [ModMenu](https://modrinth.com/mod/modmenu) | any     | ❌ (optional) |

---

## Configuration

The config file is located at `config/easyspec.json` in your server's root directory. It is **automatically created** on first run.

### Default Config

```json
{
  "_comment1": "Language: en_us, zh_cn, ja_jp, ko_kr, fr_fr, de_de, es_es, ru_ru, pt_br",
  "_comment2": "Trigger word: type '!' + this in chat to toggle spectator. Default: s (i.e. !s)",
  "language": "en_us",
  "trigger": "s"
}
```

### Options

| Field      | Type   | Default   | Description                                                      |
|------------|--------|-----------|------------------------------------------------------------------|
| `language` | string | `"en_us"` | Language for feedback messages. See supported languages below.   |
| `trigger`  | string | `"s"`     | The word after `!` used to toggle. E.g. `"spec"` → type `!spec`. |

If any field is missing or invalid, the mod will **auto-reset** the bad field, log a warning, and save the corrected file.

> **Tip:** The messages shown to players always reflect the actual trigger configured. For example, if `trigger` is set to `"spec"`, your players will see "Type **!spec** to toggle."

### Supported Languages

| Code    | Language                         |
|---------|----------------------------------|
| `en_us` | English (US)                     |
| `zh_cn` | 简体中文 (Chinese Simplified)    |
| `ja_jp` | 日本語 (Japanese)                |
| `ko_kr` | 한국어 (Korean)                  |
| `fr_fr` | Français (French)                |
| `de_de` | Deutsch (German)                 |
| `es_es` | Español (Spanish)                |
| `ru_ru` | Русский (Russian)                |
| `pt_br` | Português (Brazilian Portuguese) |

---

## Usage

1. Join your server.
2. Type `!s` (or your configured trigger) in chat.
3. You are now in spectator mode — fly through walls, observe players, explore freely.
4. Type `!s` again to return to your original position and game mode.

> **No permissions required** — any player on the server can use the trigger.

---

## Language Files

If you want to customize or add a language, the translation files are located in the mod JAR at `assets/easyspec/lang/`. Each file is a simple JSON with the following keys:

| Key                                       | Purpose                                                         |
|-------------------------------------------|-----------------------------------------------------------------|
| `mod.easyspec.name`                       | Mod display name                                                |
| `modmenu.descriptionTranslation.easyspec` | ModMenu description                                             |
| `message.easyspec.toggled`                | Message when entering spectator (use `%s` for the trigger word) |
| `message.easyspec.restored`               | Message when returning from spectator                           |

---

## Build from Source

```bash
git clone https://github.com/LuoBingbing1145/easyspec.git
cd easyspec
./gradlew build
```

The built JAR will be in `build/libs/`.

**Requirements:**
- Java 17+
- Gradle (bundled via Gradle Wrapper)

---

## For Developers

EasySpec's core is intentionally minimal — one mixin, one manager, one config class:

```
src/main/java/lbb/easyspec/
├── EasySpec.java              # Mod entrypoint
├── SpectatorManager.java      # Toggle logic & state storage
├── config/
│   ├── Config.java            # Config loading & auto-repair
│   └── Messages.java          # Translation system
└── mixin/
    └── ChatMessageMixin.java  # Chat interception mixin
```

The mixin targets `ServerGamePacketListenerImpl#handleChat` and defers work to the server thread, making it safe for any server environment.

---

## License

This project is available under the **CC0-1.0** license. Feel free to learn from it, modify it, and incorporate it into your own projects.

**Authors:** LBB285, MotherWang  
**Repository:** [github.com/LuoBingbing1145/easyspec](https://github.com/LuoBingbing1145/easyspec)
