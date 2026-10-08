# Changelog

All notable changes to this mod are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1+1.20.1] - 2026-10-08
### Changed
- Ported to Minecraft 1.20.1 (Forge).
- With [Embeddium](https://www.curseforge.com/minecraft/mc-mods/embeddium) installed, blocks around glowsticks are lit
  and colored by their light (on 1.21.1 the same works with Sodium).
- The settings are edited in the config files (`config/neon_glowsticks-common.toml` and
  `config/neon_glowsticks-client.toml`); Forge 1.20.1 has no in-game config screen for them.

## [1.0.1] - 2026-10-07
### Fixed
- With [Sodium](https://modrinth.com/mod/sodium) installed, blocks around glowsticks were lit but not colored; the
  light now has the glowstick's color there too.

### Changed
- Light from glowsticks of different colors mixes like real light: red and green make yellow instead of a dull
  olive.

## [1.0.0] - 2026-10-05
### Added
- Glowsticks in all 16 dye colors, crafted from a copper ingot, glowstone dust or a glow ink sac and a dye
  (4 per craft).
- Use to crack and throw a glowstick, sneak-use to drop it at your feet; dispensers shoot them too.
- Thrown glowsticks tumble, bounce off blocks and mobs, roll and settle lying flat; they sink slowly in water.
- Colored dynamic light: thrown glowsticks light up the area around them in their color; the light follows them,
  is blocked by walls and also lights up mobs. It is client-side, so nothing is placed in the world.
- A soft halo glows around thrown glowsticks.
- Glowsticks glow for 10 minutes, dim during the last minute and then go out.
- Right-click a thrown glowstick to pick it up with the glow it has left; hit it to knock it away.
- Configurable glow time, light level, color strength and number of lights.
