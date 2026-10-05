# Neon Glowsticks

Throwable glowsticks that bounce, roll and light up the dark with colored light.

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
- **Client-side light**: nothing is placed in the world, so it is safe for servers and other mods. It does not stop
  mobs from spawning.
- **Burns out**: a glowstick glows for 10 minutes, dims during the last minute and goes out with a puff of smoke.
- **Pick it back up**: right-click a glowstick to take it back, it keeps the glow it has left. Hitting it knocks it
  away, fire and lava put it out.

## Crafting

Shapeless: **Copper Ingot** + **Glowstone Dust** or **Glow Ink Sac** + any **Dye** gives 4 glowsticks of that color.

## Configuration

Glow time is in the common config; light level, color strength, the number of lights and the halo are in the client
config. Both are editable from the in-game mod list.

## Compatibility

Mods that replace the chunk renderer (Sodium, Embeddium) bypass the light: the game keeps working and the sticks still
glow, but blocks around them are not lit.

## Requirements

[NeoForge](https://neoforged.net) 1.21.1. Needed on both the client and the server.

## Credits

Made by **Pocky**. Source code: [GitHub](https://github.com/Pocky-l/neon-glowsticks)

<!-- more-mods:start -->
<!-- more-mods:end -->
