# CCQ Core 0.4.67

Changes since **0.4.66**.

- Fixed concurrent modification of scanner sessions when multiplayer scans finish or cancel.
- Made the block spout goggles callback cancellable before overriding its return value.
- Migrated dependency metadata to NeoForge's `type = "required" / "optional"` fields.
- Fixed simulated higher-tier coolant insertion being rejected while lower-tier fluid remains.
- Prevented lower-tier fluid consumption while higher-tier cooling is still active.
- Added regression tests for session iteration, coolant replacement, and fuel tier transitions.

# CCQ Core 0.4.66

Changes since **0.4.65**. Current version: `0.4.66`.

## Changes

- **Fixed swapped "Cooled" / "Frozen" labels in JEI on multiplayer servers**  
  Create More Recipes appends `COOLED` / `FROZEN` to Create's `HeatCondition` enum from a mixin,
  in the iteration order of a `Map.of(...)` — which is randomised per JVM. Create syncs recipe
  heat requirements through `HeatCondition.STREAM_CODEC`, an ordinal-based codec, so whenever the
  server and the client roll a different order the heat condition is decoded as the other constant:
  recipes still run correctly, but the JEI label flips. Singleplayer shares one JVM, which is why
  it never reproduces locally.

  ccq_core now replaces that codec with a name-based one (`getSerializedName`), so the wire format
  no longer depends on ordinals. Only an unknown name produces a WARN on decode; normal logging is
  unchanged.

  Server and client must run the same version; no save or recipe changes are needed.

# CCQ Core 0.4.65

Changes since **0.4.63**. Current version: `0.4.65`.

## Changes

- **Omni Speed Controller**  
  Output-side convey now follows Create's Rotation Speed Controller: the scrolled target RPM is applied directly to ordinary ±1 shaft/gearbox receivers, instead of gating on input theoretical speed (which could make live output disagree with the setting). Recursion protection is unchanged — output feedback still cannot replace an established input, and the mixin gate accepts Create `6.0.10` build suffixes.

# CCQ Core 0.4.64

Changes since **0.4.63**. Current version: `0.4.64`.

## Changes

- **Omni Speed Controller**  
  Hardened the 0.4.63 recursion fix: output-side reverse convey reports zero so Create cannot outrank an established input via floating-point edge cases, and the mixin gate accepts Create `6.0.10` build suffixes (e.g. `6.0.10-280`). Neighbour filtering still skips reverse traversal into an established Omni.

# CCQ Core 0.4.63

- Fixed a recursive rotation-network crash involving the Dreams n' Desires Omni Speed Controller. Output-side connections can no longer repeatedly replace an established input and crash the server during network rebuilding or chunk loading.
- Fixed invalid Omni transmission ratios when an overstressed input reports zero effective speed.
- Stabilized requested output RPM to prevent floating-point rounding from repeatedly triggering network updates.

The fix activates for Create 6.0.10 with Dreams n' Desires 2.3a-BETA. Requested RPM, speed limits, recipes, and stress settings are unchanged. Install the same CCQ Core version on the server and clients. Existing worlds do not need to be reset.

Conflicting powered connections still follow Create's normal mechanical failure rules. This fix prevents runaway recursion; it does not make incompatible transmission layouts valid.

# CCQ Core 0.4.61

Changes since **0.4.60**. Current version: `0.4.61`.

## Changes

- **Storage bridge**  
  Simplified multi-bridge handling: dedupe by stored controller positions only, slot-strict `insertItem`, and no auto-link side effects while resolving inventories. Same controller can have several bridges without double-exposing the same FXNT / FS network to Create Lazy Tick funnels.

# CCQ Core 0.4.60

Changes since **0.4.57**. Current version: `0.4.60`.

## Changes

- **Ore vein scanner (dedicated servers)**  
  Scans no longer force-load unloaded chunks. A leftover "scanning" flag after disconnect is cleared so the GUI cannot stay locked. Scan progress syncs with container data slots, and result packets still apply if the menu id does not match.

- **Storage bridge (multi-bridge + Create Lazy Tick)**  
  Several bridges may stay linked to the same FS / FXNT controller, but each network inventory is exposed only once (keyed by stored controller positions). Capability `insertItem` stays slot-strict so Create funnels (including Lazy Tick) can walk slots via `insertItemStacked`. Inventory resolve no longer auto-links as a side effect.

# CCQ Core 0.4.56

Changes since **0.4.50**. Current version: `0.4.56`.

## Changes

- **LeiShiDong language (zh_ls)**  
  Registers custom language「雷石东语 / 整活」via `pack.mcmeta` so the modpack can inject phonetic strings through KubeJS. Translation bodies are not bundled in this mod.

- **BnB cogwheel compat (embedded 1.0.1)**  
  Bits 'n' Bobs 2.3.5 removed the `default_cogwheel*` parents; the legacy retarget pack broke Create / Northstar / Fantasizing cog models on 2.3.5+. The pack now enables only for BnB 2.2.0–2.3.4 and is skipped on 2.3.5+.

- **Liquid snowman cooler**  
  Piping water and then coolant now correctly upgrades to freezing instead of staying on cooling. Higher-tier fluids can displace lower-tier fluid in the tank; if the tank tier is above the active fuel, the cooler switches immediately. Flowing variants were added to the regular and special coolant fluid tags.

- **Create: Fluid — fluid / smart fluid interface**  
  Interfaces glued onto minecart (and similar) contraptions no longer silently vanish on disassemble when placement order briefly fails `canSurvive`. Breaks are deferred by one tick and only destroy-and-drop if still unsupported.

- **Create: Fluid — minecart assembly crash (Create 6.0.10)**  
  Fluid 1.2.4 still read the removed `Contraption.presentBlockEntities` field, which kicked the client when assembling or loading a contraption with a gutter outlet or copper sink. Block entities are now resolved via the public API / reflection, and the copper-sink mixin apply failure was fixed.
