## 0.4.133

See the [cumulative release notes since 0.4.67](docs/releases/0.4.133.en.md).

- Support detached cabinets, cutters, light frames, awnings and stair/tower steps in both engineering bases; close two half-slab attachment gaps and lower three work lights onto their supports. Retain CBC's native two-block mount connection.
- Remove ten isolated ruin fragments while retaining crystals, inventories, machines and encounters. Update the lodge and baked rotation sections. No detached components remain in the static connectivity audit. Compile and package this release without rerunning in-game or regression tests.

## 0.4.132

- Restyle the engineering test base in gray/white concrete, with light walls, white frames and roof edging, and dark plinths and roofs. Retain its layout, wooden lodge interiors, machines, inventories and loot.
- Preserve slab/stair geometry using concrete-clad Copycat shapes. Update intact and ruined templates, the standalone lodge and baked rotation sections, retaining ruin damage and encounters.
- Add four large branching amethyst formations to the ruined range crater, warehouse, maintenance and material zones, using the abandoned facility's crystal shape. Preserve circulation, inventories, machines and encounters across baked rotation sections.
- Render the template previews, compile, package and sync to ser as requested; no tests or in-game validation.

## 0.4.131

- Remove the spawn_campus template, structure definition, placement handling, elevator assembly, generation tools and related tests; archive original files.
- Compile, package and sync this revision to ser without running tests or in-game validation, as requested.
- Register abandoned_facility as a unique surface structure centered on the overworld spawn X/Z in the engineering dimension. Persist its anchor and backfill explored chunks once using a real structure start and references.
- Register wild_amethyst_facility with random-spread spacing 32 and separation 12, two buried base layers, and exclusion from the spawn facility and engineering base sites.
- Add six facility loot tables, rolled at generation from world seed and container position into native mod inventories without replacing existing contents. Include field supplies, research materials, salvage, electronics, metals and occasional valuables. Fill empty canisters with small randomized diesel/gasoline reserves; chunk reloads do not restock.

## 0.4.130

- Move campus water power underground and restore the original open entrance atrium, glass roof and entrance-facing wall. Keep placement aligned with ground level and update elevator coordinates.
- Widen entrance-facing return stairs to five blocks with industrial iron railings. Arrange 144 machines in compact groups with continuous marked aisles.
- Relocate 24 fan stations to the workshop side wall in three groups of eight for washing, smoking and blasting, with an access aisle.
- Render the template preview and package this revision without running in-game validation, as requested.

## 0.4.129

- Clear old campus offices, server racks and stairs while retaining walls and eight geared-guard spawners; restore upper floors and relocate the stairwell behind the entrance-facing wall.
- Install three native Create elevators in the existing right-hand shafts, with cabin controls, call buttons and landing doors. Assemble automatically after water power is ready and recover cabins/glue before replacement.
- Add 96 rear large water wheels powering 144 grouped machines: 24 presses, 24 mixers, 24 millstones, 24 saws, 24 fan processing cells and 12 crushing-wheel pairs, with encased drives, real processing interfaces and collection points.
- Transform signed controller RPM for rotated/mirrored placement and rebuild lighting for the new layout.

## 0.4.128

- Furnish the campus with reception desks, open offices, meeting rooms, grouped filing cabinets and executive offices; connect all four floors by stairs.
- Add two spawners per floor with enchanted diamond guards and netherite equipment upstairs. Custom light rules allow spawns in lit rooms with bounded activation, counts and delays.
- Add flush floor and ceiling lighting throughout occupied interiors to prevent additional natural hostile spawns.

## 0.4.127

- Remove /ccqtest campus, its teleport and one-time placement state. Use /place structure ccq_testground:spawn_campus or /place template ccq_testground:engineering/spawn_campus.
- Place at the requested coordinates with rotation/mirror support, full chunk loading and world bounds checks; no natural generation.

## 0.4.126

- Import the original 243×32×104 four-storey industrial campus, preserving inventories, copycat materials and vehicle blocks.
- Add /ccqtest campus: first visit places the campus at the engineering dimension center; later visits only teleport, preserving saved player edits.

## 0.4.125

- Search nearby vertical columns for portal exits, following vanilla Nether portal placement and accepting dry replaceable plants and solid support blocks.
- If no natural site fits, clear a bounded space and build a platform and frame within world/build limits; seal fluid boundaries for submerged fallback exits.
- Preserve aperture size, axis, saved pairing and Create interdimensional track support.

## 0.4.124

- Add seated and standing Azure Maid statues as rare engineering-dimension landmarks, avoiding reserved base sites.
- Hide four diamond blocks and two netherite blocks inside each statue, replacing only fully enclosed solids and preserving the exterior.

## 0.4.123

- Group storage locally throughout intact and ruined bases, including canteen, workshops, waterworks, rear offices/stores and ammunition points; join the lodge kitchen cabinets into one row.
- Preserve container types, exact stock, loot tables and existing ruin damage; update the standalone lodge and all baked section rotations.

## 0.4.122

- Group bunker cabinets and storage barrels into contiguous banks and shorten empty shelving.
- Move all eight controls beside each gun into one adjacent row with independent wireless channels.
- Route breech bridge power through the roof ladder backing, remove the two freestanding aisle risers, and move the ladder to the hall face with a clear roof exit.

## 0.4.121

- Fix CBC Ballistics 1.3.0a overwriting mouse sensitivity while a GUI is open. Restore sensitivity once when leaving the rangefinder and preserve subsequent settings edits.

# CCQ Core 0.4.120

- Rebuilt the bunker surface entrance around x=47.5 with an 8x6 opening (formerly 5x3), paired armoured sliding leaves and two independently operated gate layers.
- Each leaf travels four blocks into concealed pockets. Recessed crank controls remain reachable at the passage edge; lintels and side shells connect continuously to the hull.
- Recover both new and legacy pistons before replacement and clear obsolete entry geometry.
- Added four-rotation checks for synchronized leaves, independent layers, open passage, closed-door bypass sealing, facade symmetry, continuous shell/lintels and rebuilding over an open gate; retain the remaining mechanical regressions.

# CCQ Core 0.4.119

- Move official TACZ pack rewriting to the GunPackLoader scan entry, before archives are opened. Avoid Windows AccessDeniedException from rewriting ZIPs during common/client Setup while client resources already hold them open.
- Enable the new scan hook only for TACZ 1.2.x; preserve legacy-port behavior.
- Validated hook execution, default-export stripping, three gunpacks and workbench icons using copies of SER packs/history. SER client GUI interaction was not tested.

# CCQ Core 0.4.118

- Fix re-conversion after restoring legacy gunpacks when previous same-named converter output already exists. Use unique work directories without removing historical results/reports.
- Serialize patch entry points to avoid overlapping initialization writes; include conversion diagnostics in failures.
- Regression tested using copies of SER legacy packs and existing conversion history, including recipe registration and workbench icon components.

# CCQ Core 0.4.117

- Fix empty official-TACZ workbenches by invoking the official universal converter for Applied, Create and Ars Armorer, retaining the separate Applied workbench override.
- Normalize legacy bare conditions on conversion copies. Reject erroneous or skipped-recipe conversions, back up originals and atomically publish verified output; retain conversion reports outside tacz.
- Official loader validation registered 68 Applied, 36 Create and 45 conditional Ars recipes (90 Ars recipes converted), plus 12g and component-based workbench icons. Existing modpack KubeJS recipe changes are retained and were not loaded by this test.

# CCQ Core 0.4.116

- Restore official TACZ 1.2 gunpack automation via a separate adapter that flattens the 1.21.1 overlay into the stripped default pack and migrates legacy output.
- Put Applied Armorer workbench overrides in an independent ccq_1_21_1 overlay using component-based icons, retaining base files and other overlays.
- Back up changed/retired packs under ccq-gunpack-backups; use source fingerprints and content comparisons for idempotence. Preserve legacy-port behavior.
- Gunpack unit tests and the official 1.2 loader check passed: 12g ammo/recipe, thirteen component-based icons and removal of the default AK47. Validation retains the pack configuration DefaultPackDebug=true.

# CCQ Core 0.4.115

- Skip legacy default gun-pack stripping and Applied Armorer rewriting with TACZ 1.2.0 or later, leaving official overlay/conversion handling intact. Retain legacy behavior for older ports.

# CCQ Core 0.4.114

Changes since **0.4.113**.

- Added four armoured hemispherical observation rooms above the gun galleries, enclosing breech lifts and piston poles without obstructing their motion.
- Added eight narrow rifle embrasure/window groups per dome, protective breastworks, rear ladder access from the upper floor and overhead lighting.
- Equipped observation rooms with Radiologistics main computers, radio transmitters and memory modules, plus Simulated gimbal instruments.
- Replaced former computer stand-in nixie tubes with Radiologistics main computers throughout the bunker, intact base, ruined base and rotated/section templates; updated authoring scripts.
- All six mechanical tests passed, including lifts, breech operation, full cannon yaw, both pitch directions, wireless controls, gates and workshop processing. No extra interior inspection.

# CCQ Core 0.4.113

Changes since **0.4.112**.

- Increased all four breech-bridge lift drives from 8 to 32 RPM (controller target -8 to -32). Both docking and retraction are four times faster: roughly 2.5 seconds for the three-block stroke instead of ten.
- Retained the separate 8 RPM breech open/close drive, console layout, reversing logic and initially parked bridges.

- All six mechanical tests passed. Sixteen rotated fixtures completed each lift stroke within 70 ticks; breech operation, cannon controls, gates and workshop processing also passed.

# CCQ Core 0.4.112

Changes since **0.4.111**.

- Connected all four bronze sliding breeches to water power through retractable overhead shaft bridges. Bridges lower three blocks onto the vertical breech port and lift clear before aiming.
- Added independent docking and breech open/close levers to the one-block-high consoles. Their redstone-link second frequencies are iron nuggets and paper. Bridges start parked with the close direction selected.
- Added local DnDesires Omni controllers, using the existing CCQ propagation fix, to stabilize direction and nominal 8 RPM across generated rotations. Breech reversal does not reverse the lift branch.
- Routed the north-west workshop ceiling main around the bridge sweep. Replacing a bunker recovers docked lifts and their glue before restoring parked hardware.
- All six mechanical tests passed, including sixteen rotated bridges docking, fully opening/closing breeches, retracting and replacing while docked, plus cannon aiming, fire signaling, gates, workshop processing and default-off arms. No extra interior inspection.

Operation: disassemble to return the gun home; dock (about ten seconds); open; load; close (about three seconds); retract (about ten seconds); assemble, aim and fire. Docking and assembly use a manual sequence. No automatic loader or automatic interlock is included.

# CCQ Core 0.4.111

Changes since **0.4.110**.

- Lowered all four cannon consoles to one block high. Levers, buttons and transmitting links share the same height; links are mounted on the back rather than above the console.
- Moved console supports from eight to four blocks behind the mount, bringing the operating face from nine to five blocks behind it. Retained six isolated channels and spaced columns.
- Updated mechanical-test panel coordinates without running tests or extra validation, as requested. Generated and packaged the template directly.

# CCQ Core 0.4.110

Changes since **0.4.109**.

- Removed the hall-facing concrete rear partitions and inward returns from all four gun galleries, keeping the exterior protection and firing openings.
- Grouped each gun's controls on a hall-side panel: assembly, yaw stop, yaw reverse, pitch stop, pitch reverse and fire button, starting at the end nearest the gun.
- Connected six independent Create redstone-link channels per gun. Red, blue, yellow and green first frequencies identify the four stations; each control has a distinct second frequency. Spaced panel columns prevent direct redstone cross-talk.
- Added a polished blackstone fire button connected to the mount's firing input, with automatic pulse release and a separate assembly signal. Guns start unassembled and stopped, without loaded ammunition.
- All five mechanical tests passed: sixteen rotated fixtures completed wireless assembly, full yaw, independent yaw/pitch reversal and replacement; the fire-button pulse was verified for single-gun isolation and release. Workshops, gates and default-off arms also passed. No extra interior inspection.

# CCQ Core 0.4.109

Changes since **0.4.108**.

- Changed every lower corner-room entrance to three manually operable copper door leaves.
- Added a flush lower-floor deck over the basement gate housing and guards around the stair opening.
- Filled rear exterior recesses beside the gun galleries while retaining firing sectors and access.
- Rebuilt the power room with twelve large water wheels in a contained race. Added a DnDesires single-block Omni Speed Controller, preset to 8 RPM, using CCQ Core's existing propagation fix.
- Connected all four cannon mounts to water power, with local speed control and separate yaw/pitch stop and reverse switches. Cannons start stopped.
- Routed cannon power beneath the deck, clear of the gate hall and yaw loops. Retained encased workshop shafts and default-off basement arms.

- Mechanical tests passed: Omni master RPM changes (16/8), sixteen rotated cannon fixtures completing full yaw, both pitch directions and independent reversal, forty-eight real processing machines, independent gates and default-off arms. No extra interior inspection.

# CCQ Core 0.4.108

Changes since **0.4.107**.

- Added paired manually operable copper doors, jambs and lintels to every downstairs corner-room entrance.
- Reworked basement lighting with always-on ceiling panels, continuous aisle strips and separate vault-entry illumination, avoiding moving tracks and drive interfaces.
- Replaced raw andesite generated by this structure with gray concrete, including foundation/skirt surfaces and infill. Retained polished finishes and andesite machine casings.
- Filled unused upper corners in gate pockets and removed oversized service voids; retained the authored leaf/hanger travel and exact kinetic hardware cells.
- No extra inspection or game tests were run, as requested. Generated and packaged the updated template.

# CCQ Core 0.4.107

Changes since **0.4.106**.

- Added four wall-side barracks beds (six to ten), retaining the library/reading furniture and relocating nearby lockers.
- Added a stocked two-level storage bank along the empty wall beside the upper furnaces.
- Reworked the lower floor's always-on ceiling-light grid over tables, corner rooms and corridors, preserving symmetry and authored openings/equipment.
- Replaced CC command terminals with Create nixie instruments and analog controls. Migrated computers/printers in the intact base, ruined base and rotated/section templates to nixie displays/archive book stacks, and replaced related CC stock with Create/redstone instrumentation supplies.
- Updated authoring scripts to retain these replacements. Interior/lighting changes only; no extra inspection or repeated mechanical tests, per user instruction.

# CCQ Core 0.4.106

Changes since **0.4.105**.

- Mirrored lower-floor public dining groups, rugs, tea cabinets and lights about the building centerline. Removed the isolated northern table group and unmatched reading suite.
- Added room-specific corner details: barracks library and writing/washing supplies, two mess dining groups and food reserves, medical cleaning/treatment counters and stools, and stocked power-room service benches/racks.
- Interior/lighting changes only; existing mechanics retained. Per user instruction, no additional interior checks or repeated mechanical tests were run.

# CCQ Core 0.4.105

Changes since **0.4.104**.

- Added upright iron-trapdoor shields beyond each fan depot. Actual recipe processing remains functional and the operator position is outside the airflow bounds.
- Encased all transmission shafts in andesite casing, preserving their axes and machine interfaces.
- Added dining/reading/rest groups to the lower living hall while retaining wall furniture. Replaced free-standing upper computer desks with a centered annular command post featuring opposing entrances, internal seats, plotting table and four terminals.
- Extended casemate rear returns into the original outer hull to close side seams without changing the firing galleries.
- Rebuilt the basement as an eighteen-stand nuclear-charge store with two handling scenes and four mechanical arms. Correct side-meshing cog inputs connect to buried transmission; a separate basement clutch is OFF by default. Per user instruction, arms were not enabled or run, only their power interfaces were validated.
- Passed five mechanics-only tests covering real outputs from 48 machines across rotations, fan shielding, gates, cannon movement/firing clearance and disabled arm interfaces. No additional decoration/layout validation.

# CCQ Core 0.4.104

Changes since **0.4.103**.

- Moved production machines against the walls: three presses, three fan-processing lines and three millstones along the west wall; three saws along the north wall. Removed the central machine island and retained front access aisles.
- Assigned the fan lines to smoking, washing and bulk blasting, using the matching Create Connected catalysts and separate depots. Configured gearshifts for the actual airflow direction.
- Routed the common drive through the north/west ceiling service layer with local drops, avoiding casemate walls. Preserved correct press/depot spacing, bottom-driven mills and horizontal saw inputs with authored stripping filters.
- Moved furnaces and fitting stations toward the walls and relocated an observation supply that would overlap a saw input.
- Limited validation to mechanics: 48 production machines across four orientations receive usable power and produce their assigned real recipe outputs. Gate and cannon operation remain covered; no additional decorative/layout checks.

# CCQ Core 0.4.103

Changes since **0.4.102**.

- Connected the diagonal casemate screen segments and rear walls, retaining two three-wide access openings. Added CCA coils around exposed gun-deck edges as well as the existing roof and outer perimeter.
- Moved lower-floor beds, galley/mess counters, dispensary furniture and supplies against room walls; removed scattered central furniture and storage.
- Consolidated upper inventories into purpose-specific wall shelving and grouped pressing, milling, sawing and fan processing into adjacent stations. Retained ceiling transmission and moved brackets/lighting away from shafts.
- Built a complete fan-processing path using a Create Connected smoking catalyst and depot instead of the campfire.
- Per user instruction, limited validation to mechanical supply, real processing, gate operation and cannon motion/firing clearance; omitted additional interior/decorative checks.

# CCQ Core 0.4.102

Changes since **0.4.101**.

- Detailed the four downstairs rooms with clothing/protective supplies, bedside items, galley washing/preparation/storage, medical and linen reserves, and power-room servicing equipment. Includes 153 stocked storage points across 18 profiles.
- Organized the upper deck into metalworking, electrical/control, storage, milling/woodworking and command/fitting zones. Consolidated desks and retained only two upper terminals.
- Moved long distribution shafts directly below the ceiling, with local machine drops. Eight waterwheels now power the press, ventilation fan, bottom-driven millstone and upward saw. Authored an oak-stripping output filter to avoid competing CBC mould recipes.
- Replaced front firing-line columns with rear supports and moved the armored screen closer to the muzzle. Three-block primary slots use half-slab bevels. Validated 3600 outgoing clearance rays over 16 guns, including diagonal bearings, both pitch signs and 0.2-block margins, plus actual full yaw and pitch motion.
- Added supported CCA barbed-wire coils along exterior/window approaches and casemate roof edges, with directional placement and a clear main entrance.
- Passed seven native GameTests, including real pressing/milling/sawing recipes, water power and containment, complete placement, furniture/inventories, independent gates, firing paths and gun motion. Static checks cover every stock front, room partitions, shared stairs, vault routes and ceiling transmission.

# CCQ Core 0.4.101

Changes since **0.4.100**.

- Reassigned the lower floor to garrison living and the upper floor to production, repair, storage, command and artillery preparation; reset furnishings, equipment, stocks and finishes.
- Enclosed four downstairs corner rooms with ceiling-height partitions and public entrances: northwest six-bed barracks, northeast power room, southwest galley/mess and southeast medical/washing facilities. Added shared daily-life furniture to the central hall.
- Moved eight flowing-water wheels into the power room and added a vertical riser and overhead distribution shafts feeding the upstairs press and ventilation fan. Corrected shaft interfaces and extended press supports to the roof.
- Relocated industrial racks and workstations upstairs, resolving radio/stock-aisle overlaps. Includes 163 stocked containers across 18 profiles plus 10 randomized observation supplies; retains shared stairs, casemates and independent gates.
- Validated real water-driven iron-sheet processing, containment, complete placement, lower-floor beds and room entrances, furniture, inventories, gates and guns. Geometry checks cover full-height partitions and routes to every storage front, corner room, stairway, gun and vault.

# CCQ Core 0.4.100

Changes since **0.4.99**.

- Rebuilt the bunker as a two-storey armored structure. The open ground floor concentrates powered production, repairs, classified storage and issue counters; shared barracks, galley, command, first aid and gun preparation occupy the upper deck.
- Added two four-wide shared stair flights connecting all four casemates. Removed individual gun ladders and the observation-cupola ladder. Reset furnishings, finishes, lights and curated inventories: 160 stocked storage points plus 10 randomized observation supplies.
- Added broad half-open firing galleries with low breastworks, deep concrete cover and corner piers, retaining complete cannon transmission and aiming controls.
- Eight naturally flowing waterwheels drive a common shaft and correctly oriented gearboxes feeding a mechanical press and ventilation fan. The contained armored-glass channel requires no manual input.
- Rebuilt all three gate lintels outside the leaf/hanger travel plane. Widened the vault leaf and stroke to six blocks and aligned its center with the eight-wide staircase and symmetric housing.
- Seven native GameTests cover complete placement, two-storey geometry, inventory and furniture connections, independent gates and regeneration, genuine water-driven iron-sheet processing, fluid containment, and full yaw/both pitch directions for 16 cannons. Static checks cover both stairways, every stock front, casemates and the vault.

# CCQ Core 0.4.99

Changes since **0.4.98**.

- Rebuilt the bunker interior as an open garrison deck. Removed room walls and glazed partitions; floor finishes, clear circulation routes and furnished work islands define each function. The underground charge stands also share an open loading gallery.
- Expanded the six-bed barracks, communal reading/tea counter, plotting and computer stations, maintenance island, medical washing area, protective-equipment benches and entrance issue counters.
- Increased curated inventories from 81 to 104 storage points across 18 profiles. Every storage front is accessible, without custom container names or signs. Added supported hanging lights and correctly connected manual machinery; retained independent gates and protected roof guns.
- Passed all six native GameTests, including command/natural placement, open sightlines, inventory and block-state validation, independent gate operation, and full yaw, positive/negative pitch and aimed regeneration for 16 guns. Static geometry checks also cover circulation and storage access.

# CCQ Core 0.4.98

Changes since **0.4.97**.

- Added two-layer overhead cover, four peripheral concrete piers and recessed lighting to each roof gun emplacement, retaining open firing galleries, access hatches and ammunition supplies.
- Matched the underground gate housing to the surface entry with light gray concrete, polished andesite coping and dark trim. Armored leaves and independent concealed drives remain operational.

# CCQ Core 0.4.97

Changes since **0.4.96**.

- Completed floor-to-ceiling room boundaries and removed incomplete bed screens. Restored hull walls beside the rebuilt entrance and added a complete steel vault-gate cover.
- Replaced constrained indoor guns with four independent roof emplacements, sealing their former wall openings. Added ladder wells, hand-operated metal hatches, parapets and ammunition supplies.
- Restored the omitted floor-level yaw transmission. Both axes retain their clutches and speed controller; the default output is now 8 RPM.
- Six native tests passed, including sixteen actual full-circle yaw runs, positive/negative pitch, rotated gun clearance and regeneration over assembled, aimed guns. Offline checks cover eighteen partition segments, hull seams, the vault cover and room/roof access.

# CCQ Core 0.4.96

Changes since **0.4.95**.

- Replaced the gate leaves with solid CBC steel and nethersteel armor, including the new underground vault gate.
- Rebuilt four gun ports as tight barrel-through-wall embrasures with three-block-deep steel collars. Extended and connected the barrels beyond the armor; glazed the other observation slits.
- Added an independent concealed sliding gate at the vault stair landing, with controls on both sides and sealed side/overhead bypass routes. Bunker replacement also resets the vault mechanism.
- Six native tests passed, including independent gates in four rotations, replacement over open gates, sixteen complete cannon assemblies, muzzle clearance and 62,496 static collision probes using player and hostile-mob dimensions.

# CCQ Core 0.4.95

Changes since **0.4.94**.

- Rebuilt the bunker entry with continuous armor, a finished ceiling and dark metal leaves with inspection windows. Explicit air removes obsolete entrance concrete and the previous exposed mechanism.
- Added two independent five-wide, three-high sliding gates, each with controls on both sides and its own wall pocket and transmission. Both gates can be open together.
- Concealed drive components behind the walls and ceiling. Shaped glue regions exclude the fixed frame and ceiling.
- Replacing a bunker now disassembles old gates, removes old entry glue and resets entry block entities before rebuilding. Tested independent operation and rebuilding over an open gate in all four rotations.

# CCQ Core 0.4.94

Changes since **0.4.93**.

- Rebuilt the bunker interior with sleeping alcoves, a galley and mess, briefing and communications rooms, workshops, armory, medical room, protective changing area and underground service benches, using 53 mod block types.
- Replaced repeated stock kits with 18 room-specific supply categories: 77 fixed-stock containers contain 146 item types, including prepared meals, cookware, drinks, potion components, protective equipment, tools, transmission parts, electrical/plumbing supplies, ordnance, documents and nuclear service materials.
- Removed seven unnecessary single-shaft crank adapters across the intact base, ruined base and bunker. Cranks connect directly to the correct shaft face; necessary long synchronized transmissions remain. Updated section/rotation templates and corrected the bunker press orientation.

# CCQ Core 0.4.93

Changes since **0.4.92**.

- Equipped all four bunker corners with correctly oriented bronze cannons and aligned ammunition racks.
- Added distinct combat, living/dining, maintenance, armory, command and observation areas. Storage is grouped into ordered banks with actual supplies and no custom names or signs.
- Added ten observation supply cabinets, each with a 45% chance of a Create Armorer gun and three matching ammunition families.
- Added a five-block-travel sliding blast door powered by real Create pistons, gearboxes, shafts and cranks accessible from both sides. The actuator runs above the doorway and the template preserves its door-leaf glue region.

# CCQ Core 0.4.92

Changes since **0.4.91**.

- Added a 96×96 low-profile concrete nuclear storage bunker to the engineering test dimension, including firing slits, a dogleg entrance, two cannon rooms, an observation cupola, internal support rooms and fourteen stocked barrels.
- The underground vault stores two small, one standard and one high-yield nuclear charge. Generation embeds the template eight blocks below its ground layer, selects flat terrain and avoids existing intact/ruined engineering base sites.
- Added locate and whole-bunker placement through `ccq_testground:nuclear_bunker`. Manual placement loads all covered chunks and treats the requested height as ground level; template placement also supports rotation and mirroring.

# CCQ Core 0.4.91

Changes since **0.4.90**.

- Added sixteen persistent authored encounters to ruined bases: eight 40-health zombies, five 36-health skeletons and three 28-health spiders. Zombies use iron swords/axes; zombies and skeletons wear mixed iron/chainmail armor, and skeletons carry bows.
- Distributed encounters across safe lodge, workshop, warehouse-edge, utility, logistics and loading positions, including clearance for spiders. Entities are partitioned per section, with native rotation, health and equipment preserved for both world generation and manual placement.
- Corrected the pack's KubeJS Chinese naming fix at its actual source: complete language assets are read after language events and had overwritten the JS supplement. Updated the complete Create Deco Chinese JSON and removed redundant JS entries; corrected validation and priority auditing.

# CCQ Core 0.4.90

Changes since **0.4.89**.

- Fixed 758 bottom-slab support gaps in the intact base and reviewed legacy standalone templates.
- Restored wall/roof cells replaced by ten lamps and moved the fixtures into neighboring open positions. Re-baked native connection states and lighting for all orientations.
- Added distant shockwave damage to surviving buildings: torn lodge/workshop roofs, a damaged observation-tower corner, broken windows and railings, cracked brickwork and perimeter breaches, while retaining explorable supplies.
- Updated the pack's KubeJS Chinese overrides for 28 Create Deco brick names, replacing phonetic or inconsistent names with appearance-based names without editing mod language files.

# CCQ Core 0.4.89

Changes since **0.4.88**.

- Added a chain-detonation engineering base with eight main blast sites, irregular craters up to 11 blocks deep, secondary blast lobes, broken walls, landslides and remaining living quarters/salvage.
- Exported three chunk-safe sections with four native-baked orientations. Ruins use a 12-block underground offset and a separate grid avoiding intact bases; existing intact-generation sites remain unchanged.
- Fixed base `/place structure` commands being rejected outside natural-generation sites and `/place template` failing on unloaded distant chunks. Both variants now place completely in one command, with the ruined ground offset applied automatically. Other structures/templates retain vanilla handling.
- Manual placement acquires temporary chunk tickets and releases them afterwards, preserving persistent force-loaded chunks, rotated layouts and container stock.

# CCQ Core 0.4.88

Changes since **0.4.87**.

- Organized core, Test Ground and embedded Physics Gantry resources into matching English, Simplified Chinese, Russian and custom-language dictionaries.
- Localized the Test Ground info command and added dimension/biome labels. Clarified coolant, cooling ticks, vein scan distance and completion messages.
- Aligned the storage bridge name with pack overrides and removed the obsolete hardcoded resource-pack version label.
- Clarified belt-wheel instructions and input/output labels, reviewed Chinese terminology and punctuation, and supplied Russian/custom-language coverage.
- Added checks for language coverage, duplicate keys, invalid characters, argument indices and formatting. Structure and machine layouts are unchanged.

# CCQ Core 0.4.87

Changes since **0.4.86**.

- Reworked industrial detailing outside the lodge, using installed pack blocks for brick-and-steel framing, glazing, roof monitors, vents, task lighting, workbenches and flush drains.
- Distinguished the canteen, parking/service bays, ammunition preparation hall, fabrication bay, warehouse, loading yard, stockyard, waterworks and observation offices with themed furniture and supplies.
- Reduced repetitive cabinetry. Stocked every remaining cabinet and previously empty chest/barrel, including household supplies in lodge cupboards. Added 212 fixed-stock containers for 253 total, alongside 21 random supply containers.
- Preserved all 41 original warehouse containers and 3787 items, plus existing random supplies. Added removable materials on 25 new depots; original processing inputs remain unfilled. No custom container names or signs.
- Retained the lodge layout, working machinery, synchronized cranes, actual water delivery and clear firing lanes. Re-baked all four rotations of native connection states and lighting.
- All sixteen native integration tests pass, including actual stock and workpiece loading. Changes apply to newly generated bases; existing bases are not automatically rebuilt or restocked.

# CCQ Core 0.4.86

Changes since **0.4.85**.

- Baked native neighbor-derived states for iron bars, tables, sofas and stairs into four rotation-specific sets of base sections, correcting corners and rotated furniture connections.
- Rebuilt the isolated press platform as a ground-level service station with a driven press, mixer and basin, workbenches and tool cabinets. Verified hand-powered operation.
- Added recessed lighting throughout shelters, stores, the original workshop and lodge. Vanilla light propagation checks require block light of at least 8 above covered, walkable ground and authored raised floors.
- Lowered the waterworks output basin to a one-block rim and rerouted its outlet pipes, retaining working pump delivery.
- Completed continuous paving, unified gravel plots, aligned foundations with surrounding ground and added entrance transitions.
- Preserved 41 fixed-stock containers and 21 random supply containers, without custom names or signs.
- All sixteen native integration tests pass, including four rotations of connection and lighting checks, actual machine operation, water delivery and natural section generation. Layout changes apply to newly generated bases.

# CCQ Core 0.4.85

Changes since **0.4.84**.

- Filled the remaining base plots with a courtyard, three-bay parking canopy, ammunition preparation shelter, service annex, rail loading apron, assembly/lifting yard, bulk stockyard, waterworks, rear stores and observation/duty rooms.
- Added a working synchronized pulley lift, a hand-driven mechanical press and a cogwheel-driven pump supplying a separate basin from the reservoir.
- Added supply racks, material pallets and connecting walks. Retained all three firing lines and added a stepped backstop beyond the original targets.
- Completed continuous stone-brick and iron-bar boundaries, retaining front/rear entrances and a clear service road. No custom container names or signs were added.
- Added seven themed supply containers for a total of 21 random loot containers; preserved all 41 original fixed-stock containers and the lodge layout.
- Added native checks for walls, access, lifting, press rotation and actual pump delivery. All fourteen integration tests pass.
- The layout applies to newly generated bases; existing bases are not rebuilt automatically.

# CCQ Core 0.4.84

Changes since **0.4.83**.

- Restored four missing stone-brick blocks in the front corner column of the range's side building, including the merged base, generation sections and legacy range template.
- Removed custom container names, warehouse signs and the lodge notice board. Fixed categorized inventories and lodge loot remain unchanged.
- Reorganized the upper lodge into one aligned row of four blue beds, matching bedside storage, wall-aligned wardrobes and a continuous reading desk, with a regular rug and symmetric pendant lights.
- Extended native checks for all range-building corners, default container names, absence of signs and access to every bed. All thirteen integration tests pass.
- New layouts apply to newly generated bases; existing furnishings and inventories are not reset automatically.

# CCQ Core 0.4.83

Changes since **0.4.82**.

- Refurnished the engineering lodge with a dual-stove kitchen, four-seat dining table, sofa/tea lounge and duty desk downstairs; four colored beds, bedside storage, bookshelf screens, wardrobes and a reading/tea desk upstairs.
- Uses the pack's Kaleidoscope Cookery/Tavern, Farmer's Delight/Respite, Supplementaries, Create Deco and CC:Tweaked furniture, kitchenware, food, clocks, notice board, globes, lighting, computer and printer. Added rugs and plants.
- Preserved all five lodge supply containers, warehouse inventories and other facility machinery. Added an independently placeable lodge template.
- Added native checks for furniture connections, supports, player circulation and actual cooking. All thirteen integration tests pass.
- Declared furnishing mods as required dependencies and raised minimum NeoForge to 21.1.247, already satisfied by the test instance. Existing lodges are not refurnished automatically.

# CCQ Core 0.4.82

Changes since **0.4.81**.

- Replaced random warehouse supplies with authored fixed inventories: forty categorized barrels and one shared tool chest, containing 114 item types and 3,787 items across construction, metals, transmission, controls/assembly, and cannon supplies. Added rack signs and container labels.
- Expanded lodge supplies to five containers with food, household items, clothing materials, books, tools, and personal effects. The medical chest guarantees healing/regeneration potions and golden apples.
- Increased facility chest rolls to 8–12 and barrel rolls to 4–7, with larger material stacks. Chest rare/exceptional chances are now 65%/6%; barrel bonus chance is 20%.
- Added native checks for fixed IDs/counts, category labels, serialized inventory reload, no rerolls, household supplies, and real potion effects, alongside existing generation and machinery checks.
- New stock applies to newly generated bases; existing inventories are preserved and old warehouses are not rewritten automatically.

# CCQ Core 0.4.81

Changes since **0.4.80**.

- Fixed incomplete natural engineering bases when Structurify overlap prevention rejects adjoining sections of the same base. Only the engineering base is exempt from that check.
- Preserved overlap checks for other structures, global settings, and spacing multipliers without editing instance configuration.
- Reproduced the failure using the test instance's Structurify version/configuration (overlap prevention enabled, spacing multiplier 1.25) with C2ME, then verified complete natural bases at positive and negative coordinates.
- Added a check confirming the base exemption while vanilla desert pyramids retain overlap checks. The optional compatibility mixins do not apply without Structurify.
- Existing partial bases can still be restored with `/ccqtest repairbase`.

# CCQ Core 0.4.80

Changes since **0.4.79**.

- Fixed severely incomplete engineering bases with C2ME parallel generation. Each chunk now receives independent placement settings instead of racing over shared clipping bounds.
- Added the admin command `/ccqtest repairbase` to restore template blocks currently occupied by air while preserving existing non-air blocks, container contents, and loot state. Saved bases are not modified automatically.
- Extended integration checks for positive/negative coordinates, two base rotations, missing-chunk recovery, and preservation of player blocks and inventories, using the same C2ME and Structure Layout Optimizer versions as the test instance.
- `/ccqtest info` now displays the actual loaded version.

# CCQ Core 0.4.79

Changes since **0.4.78**.

- Replaced natural generation of three separate facilities with one 120×300×30 engineering base containing the tower, extended cannon range, and workshop.
- Added a two-floor duty lodge with four beds, kitchen, dining and duty areas, and balcony, plus storage, gatehouse, loading yard, roads, and expansion space.
- Retained working machinery and cannons; the base contains fifteen themed loot containers with rare bonus rolls.
- Fixed road lamp brackets and bedroom pendant support. Three coordinated generation sections prevent clipping beyond vanilla chunk-reference limits.
- Uses a 64/32-chunk random grid and rejects sites exceeding two blocks of sampled relief. Locate with `/locate structure ccq_testground:engineering_base`.
- Preserved legacy registrations for saved structures while disabling separate natural generation. Existing chunks remain intact.

# CCQ Core 0.4.78

Changes since **0.4.77**.

- Added themed loot to one tower barrel, one range barrel, and four workshop barrels, with 1–3 supply rolls. Other barrels and the winch cargo remain empty.
- Supply chests now have independent 25% rare and 2% exceptional bonus rolls, including precision mechanisms, diamonds, netherite materials, machinery, and enchanted golden apples.
- Supply barrels have an independent 5% bonus roll for a precision mechanism, diamond, or sturdy sheet.
- Extended natural-generation checks to cover barrel loot references, generated supplies, and preventing loot rerolls.

# CCQ Core 0.4.77

Changes since **0.4.76**.

- Removed the workshop service bench's mechanical bearing, iron test load, and terminal shaft. Retained the gear service bench.

# CCQ Core 0.4.76

Changes since **0.4.75**.

- Replaced the five-stage cannon reduction towers with speed controllers and one large cog each, preset to 1 RPM. Retained pitch and yaw clutches.
- Moved hand-crank controls to ground level and removed the elevated access platforms and ladders.
- Extended four-rotation operation tests to verify controller presets and actual 1 RPM on both cannon axes, alongside assembly and firing.

# CCQ Core 0.4.75

Changes since **0.4.74**.

- Removed the cannon-free template, runtime fallback, and optional-CBC test branches.
- Create Big Cannons 5.11.7+ is now required; firing ranges always use the complete cannon layout.
- Retained the extended range, corrected mounts, reduction gearing, and synchronized lift from 0.4.74.

# CCQ Core 0.4.74

Changes since **0.4.73**.

- Expanded the firing range to 54×221 blocks, with separate targets at longitudinal positions 80, 140, and 200.
- Corrected cannon mount height, assembly controls, segment connections, and material-appropriate barrel lengths. Added five-stage reduction and pitch/yaw clutch branches to provide sufficient hand-crank torque.
- Rebuilt the crane as a fixed bridge with a common shaft, twin pulleys, and one load beam using native Create mirroring. Removed unwanted guardrail attachments and added access steps and ladders.
- Reworked the tower supply winch and access walkway while retaining a static observation instrument.
- Added operation tests in four rotations for real shaft connections, transmission speeds, synchronized lifting, cannon assembly, and live firing. Generated guns remain unloaded; ammunition is supplied only by tests.
- Preserved supply loot, sparse generation, and the optional CBC fallback. Existing chunks are not rebuilt.

# CCQ Core 0.4.73

Changes since **0.4.72**.

- Rebuilt the three facilities with accessible stairs, bracing, separate firing lanes, inspection trenches, and dedicated work areas.
- Added Create transmissions, bearings, cranks, gantries, pulleys, and a press station, plus bronze, cast-iron, and steel CBC cannon layouts.
- Each facility includes one themed supply chest, rolling 3–5 loot entries on first opening without rerolling.
- Uses a cannon-free fallback template when CBC is absent. Machines and cannons generate as unassembled blocks; assembly and operation still need in-game testing.
- Preserved structure IDs, sparse placement, and terrain settings. Updated templates apply to newly generated chunks.

# CCQ Core 0.4.72

Changes since **0.4.71**.

- Added original survey towers, cannon calibration ranges, and assembly pads to the engineering dimension.
- Facilities share a sparse placement grid, favor towers, rotate randomly, and reject steep sites. Small foundation gaps are filled locally.
- Natural generation is restricted to the testground biome. Structures contain no entities or spawners and can be freely modified.
- Preserved andesite terrain and ore settings. Existing chunks remain unchanged; new structures support `/locate structure`.

# CCQ Core 0.4.71

Changes since **0.4.70**.

- Replaced the engineering dimension grass and dirt surface with five layers of andesite, preserving terrain shape and ore settings.
- Disabled world-generation mob spawning and cleared passive, ambient, and aquatic animal spawn lists. Existing and manually introduced animals remain.
- Surface changes apply to newly generated chunks; existing worlds are preserved.

# CCQ Core 0.4.70

Changes since **0.4.69**.

- Engineering portals now implement Minecraft's `Portal` interface and use `setAsInsidePortal`, `PortalProcessor`, and `DimensionTransition`.
- Removed custom warm-up, cooldown, and direct entity transport. Stationary contact detection only reports contact to the native processor.
- Transition delays follow the Nether portal gamerules; default survival delay is approximately four seconds. Ordinary entities use native immediate transfer and cooldown.
- Preserved industrial iron frames, registry IDs, persistent paired exits, 1:1 coordinates, and Create's native portal track integration.
- Retired the legacy custom entity timer without changing existing gates or pair data.
- All seven GameTests passed, including native processor charging/decay, item and XP preservation, native cooldown, and portal tracks.

# CCQ Core 0.4.69

Changes since **0.4.68**.

- Fixed unreliable portal transfers for dropped items, experience orbs, and stationary entities using server tick contact checks instead of movement-driven block callbacks.
- Ordinary entities now transfer on contact; survival players retain the two-second warm-up. Leaving clears interrupted progress, while entities remaining in the exit do not bounce back.
- Create contraptions bypass ordinary entity transport; trains continue using native interdimensional portal tracks.
- Added regression checks for stationary drops and XP, item count/components, experience value, and duplicate prevention. The existing mob round-trip test now uses a no-AI mob.

# CCQ Core 0.4.68

Changes since **0.4.67**.

- Integrated the Engineering Test Ground prototype 0.1.5: broad plains, gentle hills, deep solid terrain, rich ores, and no caves.
- Industrial iron frames activate with a Create wrench. Creative players teleport immediately; survival players wait approximately two seconds. Includes surface validation and persistent paired exits.
- Uses Create's native interdimensional portal track API and preserves the entrance aperture size when generating an exit.
- Preserved the existing dimension ID, portal block ID, and portal link save file for compatibility with prototype worlds.
- Remove the standalone `ccq_testground` JAR when upgrading. Install the same core version on clients and servers. Existing chunks are not regenerated.
- All six test-ground GameTests passed. Existing unit tests: 14 passed, 11 skipped due to missing reference files.

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
