# Neon Glowsticks

Throwable glowsticks that bounce, roll and light up the dark with colored light.

![Thrown glowsticks light up a cave in their own colors](https://raw.githubusercontent.com/Pocky-l/neon-glowsticks/main/docs/screenshots/cave.jpg)

*Thrown glowsticks light up a cave in their own colors*

## Features

- **16 glowsticks**: one for every dye color, with 3D models in hand and on the ground. The black one glows
  ultraviolet, the brown one amber.
- **Throw them**: *Use* cracks a glowstick and throws it, *Sneak* + *Use* drops it at your feet. Dispensers shoot
  glowsticks too.
- **Real physics**: glowsticks tumble through the air, bounce off walls, floors and mobs, roll when they land sideways
  and settle lying flat. They sink slowly in water and drift with the current.
- **Colored dynamic light**: a thrown glowstick lights up the area around it in its color. The light spreads like
  vanilla light (walls block it), blends with Minecraft's smooth shading, lights up mobs and players and follows the
  stick while it flies, rolls or sinks. A soft halo glows around the stick.
- **Glows in your hand**: a glowstick held in either hand lights up the area around whoever holds it (you, other
  players and mobs), so you can walk through a cave with one in hand. It is a bit weaker than a thrown stick and
  holding it never uses up its glow.
- **Client-side light**: nothing is placed in the world, so it is safe for servers and other mods. It does not stop
  mobs from spawning.
- **Burns out**: a glowstick glows for 10 minutes, dims during the last minute and goes out with a puff of smoke.
- **Pick it back up**: right-click a glowstick to take it back, it keeps the glow it has left. Hitting it knocks it
  away, fire and lava put it out.

## Crafting

Shapeless: **Copper Ingot** + **Glowstone Dust** or **Glow Ink Sac** + any **Dye** gives 4 glowsticks of that color.

## Configuration

Glow time is in the common config; light level, held light, color strength, the number of lights and the halo are in
the client config. Both are editable from the in-game mod list.

## Compatibility

Works with [Sodium](https://modrinth.com/mod/sodium), colored light included. Other mods that replace the chunk
renderer (such as Embeddium) may bypass the light: the game keeps working and the sticks still glow, but blocks around
them may not be lit.

## Requirements

[NeoForge](https://neoforged.net) 1.21.1. Needed on both the client and the server.

## Credits

Made by **Pocky**. Source code: [GitHub](https://github.com/Pocky-l/neon-glowsticks)

<!-- more-mods:start -->
## More mods by Pocky

[![Turbo for Distant Horizons](https://raw.githubusercontent.com/Pocky-l/dhturbo/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/turbo-for-distant-horizons)

**[Turbo for Distant Horizons](https://www.curseforge.com/minecraft/mc-mods/turbo-for-distant-horizons)** - Distant Horizons addon: generates distant terrain from the world noise many times faster, with real trees nearby. ([source](https://github.com/Pocky-l/dhturbo))

[![Holy Staff](https://raw.githubusercontent.com/Pocky-l/holy-staff/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/holy-staff)

**[Holy Staff](https://www.curseforge.com/minecraft/mc-mods/holy-staff)** - A holy staff with three healing skills, aim previews and flying heal numbers. ([source](https://github.com/Pocky-l/holy-staff))

[![Lumen Rigs](https://raw.githubusercontent.com/Pocky-l/lumen-rigs/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/lumen-rigs)

**[Lumen Rigs](https://www.curseforge.com/minecraft/mc-mods/lumen-rigs)** - Aimable spotlights, floodlights, searchlights and soft panels with colored light and visible beams. ([source](https://github.com/Pocky-l/lumen-rigs))

[![Petrichor: Rain & Storms](https://raw.githubusercontent.com/Pocky-l/petrichor/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms)

**[Petrichor: Rain & Storms](https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms)** - Realistic rain and storms: rain types, puddles, runoff and drips, branching lightning with delayed thunder. ([source](https://github.com/Pocky-l/petrichor))

[![Rustling Leaves](https://raw.githubusercontent.com/Pocky-l/rustling-leaves/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/rustling-leaves)

**[Rustling Leaves](https://www.curseforge.com/minecraft/mc-mods/rustling-leaves)** - Physically simulated leaves: falling leaves, leaf piles you can wade through, rake and blow away, gusts, whirlwinds and leaf tools. ([source](https://github.com/Pocky-l/rustling-leaves))

[![Rancher's Vacpack](https://raw.githubusercontent.com/Pocky-l/ranchers-vacpack/main/docs/icon.png)](https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack)

**[Rancher's Vacpack](https://www.curseforge.com/minecraft/mc-mods/ranchers-vacpack)** - A Slime Rancher inspired vacuum gun: suck up items and small mobs, store them in a tank and shoot them back out. ([source](https://github.com/Pocky-l/ranchers-vacpack))

<!-- more-mods:end -->
