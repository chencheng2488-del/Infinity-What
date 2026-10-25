# Infinity What v1.0.0b (archived)

Infinity What is a Minecraft mod for Forge 1.20.1. It adds two generator blocks:

- **Infinity Fluid Generator** (`infwhat:inf_fluid_generator`) — stores one fluid and, while the
  infinite source is enabled in the config, turns it into an unlimited source exposed through the
  standard Forge fluid capability.
- **Infinity Item Generator** (`infwhat:inf_item_generator`) — while redstone powered, the single
  stored item becomes an unlimited source: it is pushed into adjacent inventories and exposed
  through the standard Forge item capability.

Both generators work with pipe and storage mods (AE2, Mekanism, Pipez, Create, ...) through the
generic Forge capabilities, so no optional dependency is required. Behaviour can be tuned in the
common config (`enableInfiniteSource`, `finiteTankCapacity`, `fluidBlacklist`).

This is the archived v1.0.0b source tree, kept for reference. For the current version see the
"Infinity What Forge 1.20.1" project.

- Mod id: `infwhat`
- Package: `com.bandybus.infwhat`
- Author: BandyBus
- License: Apache License 2.0
