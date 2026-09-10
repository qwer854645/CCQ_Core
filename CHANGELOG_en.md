# CCQ Core 0.4.54

Changes since **0.4.50**. Current version: `0.4.54`.

## Changes

- **Liquid snowman cooler**  
  Piping water and then coolant now correctly upgrades to freezing instead of staying on cooling. Higher-tier fluids can displace lower-tier fluid in the tank; if the tank tier is above the active fuel, the cooler switches immediately. Flowing variants were added to the regular and special coolant fluid tags.

- **Create: Fluid — fluid / smart fluid interface**  
  Interfaces glued onto minecart (and similar) contraptions no longer silently vanish on disassemble when placement order briefly fails `canSurvive`. Breaks are deferred by one tick and only destroy-and-drop if still unsupported.

- **Create: Fluid — minecart assembly crash (Create 6.0.10)**  
  Fluid 1.2.4 still read the removed `Contraption.presentBlockEntities` field, which kicked the client when assembling or loading a contraption with a gutter outlet or copper sink. Block entities are now resolved via the public API / reflection, and the copper-sink mixin apply failure was fixed.
