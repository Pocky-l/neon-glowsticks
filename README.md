<p align="center">
  <img src="src/main/resources/logo.png" alt="Neon Glowsticks" width="160">
</p>

<h1 align="center">Neon Glowsticks</h1>

<p align="center">
  Throwable glowsticks that bounce, roll and light up the dark with colored light.
</p>

<p align="center">
  <a href="https://www.curseforge.com/minecraft/mc-mods/neon-glowsticks"><img alt="CurseForge downloads" src="https://img.shields.io/curseforge/dt/1727688?logo=curseforge&label=CurseForge&color=F16436"></a>
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A">
  <a href="https://neoforged.net"><img alt="NeoForge" src="https://img.shields.io/badge/Loader-NeoForge-F16436"></a>
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue">
</p>

## Features

- **16 glowsticks** — one for every dye color: a glowing core inside a clear plastic tube with caps and a lanyard
  loop, in 3D in hand and on the ground. The black one glows ultraviolet, the brown one amber.
- **Throw them** — *Use* cracks a glowstick and throws it; *Sneak* + *Use* drops it at your feet.
  Dispensers shoot glowsticks too.
- **Real physics** — glowsticks tumble through the air, bounce off walls, floors and mobs, roll when they hit the
  ground sideways and settle lying flat on one of their sides. They sink slowly in water and drift with the current.
- **Colored dynamic light** — a thrown glowstick lights up the area around it in its color (light level 13 by
  default). The light spreads like vanilla light, so walls block it, blends smoothly with Minecraft's shading, lights
  up mobs and players too, and follows the stick while it flies, rolls or sinks. Several colors mix where their light
  meets. A soft halo glows around the stick itself.
- **Client-side light** — the light is computed by each player's game and nothing is placed in the world, so it is
  safe for servers and other mods. It does not stop mobs from spawning.
- **Burns out** — a glowstick glows for 10 minutes, dims during the last minute and goes out with a puff of smoke.
- **Pick it back up** — right-click a glowstick in the world to take it back; it keeps the glow it has left (shown in
  the tooltip). Hitting it knocks it away; fire and lava put it out.

## Controls

| Action | Default |
|---|---|
| Crack and throw a glowstick | *Use* (right click) |
| Drop a glowstick at your feet | *Sneak* + *Use* |
| Pick up a thrown glowstick | *Use* on it |
| Knock a glowstick away | *Attack* (left click) on it |

## Crafting

Shapeless: **Copper Ingot** + **Glowstone Dust** or **Glow Ink Sac** + any **Dye** → 4 glowsticks of that color.

In creative mode the glowsticks are in the **Pocky Mods** and **Tools & Utilities** tabs.

## Configuration

Common config (`config/neon_glowsticks-common.toml`, also editable from the in-game mod list):

| Option | Default | |
|---|---|---|
| `glowSeconds` | 600 | how long a thrown glowstick glows |

Client config (`config/neon_glowsticks-client.toml`):

| Option | Default | |
|---|---|---|
| `lightLevel` | 13 | light level of a glowstick |
| `coloredLight` | true | tint the light with the glowstick's color |
| `coloredLightStrength` | 1.0 | how strong the tint is |
| `maxLights` | 32 | how many of the nearest glowsticks give light at once |
| `halo` | true | soft glow around the sticks |

## Compatibility

The light hooks into Minecraft's own block and entity rendering. Mods that replace the chunk renderer (such as
Sodium or Embeddium) bypass it: the game keeps working, the glowsticks still glow and have their halo, but blocks
around them are not lit. The game log says which light hooks are active.

## Installation

1. Install [NeoForge](https://neoforged.net) for Minecraft 1.21.1.
2. Put this mod into the `mods` folder.

The mod is needed on both the client and the server.

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/`.

## Credits

- Author: **Pocky**.
- Sound effects are built from royalty-free sources: [Kenney](https://kenney.nl)'s Impact and Interface packs (CC0)
  and the OpenGameArt upload "Swishes Sound Pack" (CC0).

<!-- more-mods:start -->
## More mods by Pocky

<table>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><img src="https://raw.githubusercontent.com/Pocky-l/holy-staff/main/docs/icon.png" width="96" alt="Holy Staff"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><b>Holy Staff</b></a><br>
      A holy staff with three healing skills, aim previews and flying heal numbers.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/holy-staff"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1725465?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/holy-staff"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/lumen-rigs"><img src="https://raw.githubusercontent.com/Pocky-l/lumen-rigs/main/docs/icon.png" width="96" alt="Lumen Rigs"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/lumen-rigs"><b>Lumen Rigs</b></a><br>
      Aimable spotlights, floodlights, searchlights and soft panels with colored light and visible beams.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/lumen-rigs"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1727739?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/lumen-rigs"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><img src="https://raw.githubusercontent.com/Pocky-l/petrichor/main/docs/icon.png" width="96" alt="Petrichor: Rain & Storms"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><b>Petrichor: Rain & Storms</b></a><br>
      Realistic rain and storms: rain types, puddles, runoff and drips, branching lightning with delayed thunder.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1729574?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/petrichor"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><img src="https://raw.githubusercontent.com/Pocky-l/rustling-leaves/main/docs/icon.png" width="96" alt="Rustling Leaves"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><b>Rustling Leaves</b></a><br>
      Physically simulated leaves: falling leaves, leaf piles you can wade through, rake and blow away, gusts, whirlwinds and leaf tools.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1729578?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/rustling-leaves"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
  <tr>
    <td align="center" width="112"><a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><img src="https://raw.githubusercontent.com/Pocky-l/ranchers-vacpack/main/docs/icon.png" width="96" alt="Rancher's Vacpack"></a></td>
    <td>
      <a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><b>Rancher's Vacpack</b></a><br>
      A Slime Rancher inspired vacuum gun: suck up items and small mobs, store them in a tank and shoot them back out.<br>
      <a href="https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1725381?logo=curseforge&label=CurseForge&color=F16436"></a>
      <a href="https://github.com/Pocky-l/ranchers-vacpack"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    </td>
  </tr>
</table>
<!-- more-mods:end -->

## License

[MIT](LICENSE)
