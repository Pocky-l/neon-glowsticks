# Changelog

All notable changes to this mod are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-09
### Added
- Glowsticks glow in your hand: a glowstick held in the main hand or off hand lights up the area around you in its
  color, so you can walk through a cave with one in hand. Other players and mobs holding a glowstick (a zombie that
  picked one up, an allay carrying one) light up their surroundings too. Two glowsticks in both hands give two lights.
- Held light is a bit weaker than a thrown stick's (light level 11 by default) and never uses up the stick's glow time.
  A picked-up stick that was already dimming stays as dim in your hand.
- New client config options: `heldLight` turns the light of held glowsticks on or off, `heldLightLevel` sets how
  bright it is. Held glowsticks count towards `maxLights`; your own stick always gets a light first.

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
