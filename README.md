# EarthquakeMC

A server plugin for Paper and Spigot (Minecraft 26.3) that makes servers friendlier to
[Horizon](https://playhorizon.dev) clients, a Rust implementation of Minecraft: Java Edition.

The long-term goal is a protocol-compatibility layer in the spirit of GeyserMC: one server that
Horizon, Java and, eventually, Bedrock players can all join. Today the plugin covers the Horizon and
Java side only.

## Features

- **Quiet commands (`horizon:quiet_commands`).** Horizon mods can send a batch of commands that the
  server runs as the sending player, at that player's own permission level. The plugin announces the
  channel during the configuration phase (Paper only), decodes each batch, and runs it at most 64 lines
  per player per tick, with at most 4096 lines queued per player. Commands that carry a chat message
  (`say`, `msg`, `tell`, `w`, `me`, `teammsg`, `tm`) are skipped, as Horizon's own client skips them.
  The payload layout and limits follow Horizon's published protocol code.
- **Version check.** Horizon speaks protocol 777 (Minecraft 26.3). If the server runs a different
  version and ViaVersion is not installed, the plugin logs a warning at startup. Translation between
  versions is done by ViaVersion and ViaBackwards, which are optional soft dependencies.

## Requirements

- Paper 26.3 (or a Spigot build of 26.3; see the limitations below)
- Java 25

## Installation

1. Download the jar from the releases page, or build it yourself (below).
2. Put it in your server's `plugins/` folder and restart.

## Building

```
./gradlew build
```

The jar is written to `build/libs/earthquakemc-<version>.jar`. A JDK 25 is required; Gradle will use a
matching toolchain if one is installed.

## Limitations

- Bukkit cannot mute command feedback, so the output of a quiet command still appears in chat.
  Horizon's own dedicated server suppresses it.
- On Spigot the channel cannot be announced during the configuration phase, so Horizon falls back to
  sending ordinary chat commands.
- Folia is not supported (the plugin uses the global Bukkit scheduler).
- Bedrock support is not implemented. A Bedrock bridge needs RakNet networking, the Bedrock login
  flow, and translation of blocks, items, entities and inventories, and is the planned next stage.

## Inspiration and credits

EarthquakeMC is inspired by [GeyserMC](https://github.com/GeyserMC/Geyser), which lets Bedrock players
join Java servers. Its public source was read to understand the overall architecture of a protocol
bridge that runs as a Paper/Spigot plugin. No GeyserMC code was copied, and EarthquakeMC currently
shares no code with it. Planned Bedrock work would use the same kind of third-party libraries
(CloudburstMC protocol and network, MCProtocolLib) as dependencies rather than reimplementing them.

EarthquakeMC is an independent project. It is not affiliated with or endorsed by GeyserMC, Mojang
Studios, Microsoft or the Horizon project. "Minecraft" is a trademark of Mojang Studios.

## License

[MIT](LICENSE)
