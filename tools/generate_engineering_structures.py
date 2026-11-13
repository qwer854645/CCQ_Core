"""Build the facility templates from editable block layouts. Requires nbtlib."""
from pathlib import Path
import json
import nbtlib as n

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'src/main/resources/data/ccq_testground/structure/engineering'

def parse(block):
    base, sep, tail = block.partition('[')
    return base, dict(entry.split('=') for entry in tail[:-1].split(',')) if sep else {}

def state(block):
    base, props = parse(block)
    if ':' not in base:
        base = ('create:' if base == 'industrial_iron_block' else 'minecraft:') + base
    result = n.Compound({'Name': n.String(base)})
    if props:
        result['Properties'] = n.Compound({key: n.String(value) for key, value in props.items()})
    return result

class Layout:
    def __init__(self, key):
        source = json.loads((ROOT / 'tools/engineering/base_v3' / (key + '.json')).read_text())
        self.key = key
        self.size = source['size']
        self.blocks = {tuple(block['pos']): block['block'] for block in source['blocks']}
        self.tags = {}

    def put(self, x, y, z, block):
        if block == 'air':
            self.blocks.pop((x, y, z), None)
        else:
            self.blocks[x, y, z] = block

    def box(self, x, y, z, X, Y, Z, block):
        for dx in range(x, X + 1):
            for dy in range(y, Y + 1):
                for dz in range(z, Z + 1):
                    self.put(dx, dy, dz, block)

    def export(self, filename=None):
        palette = ['air'] + sorted(set(self.blocks.values()))
        indices = {block: index for index, block in enumerate(palette)}
        blocks = []
        for x in range(self.size[0]):
            for y in range(self.size[1]):
                for z in range(self.size[2]):
                    pos = (x, y, z)
                    block = n.Compound({'pos': n.List[n.Int](pos), 'state': n.Int(indices[self.blocks.get(pos, 'air')])})
                    if pos in self.tags:
                        block['nbt'] = self.tags[pos]
                    blocks.append(block)
        tag = n.Compound({'DataVersion': n.Int(3955), 'size': n.List[n.Int](self.size),
                          'palette': n.List[n.Compound]([state(block) for block in palette]),
                          'blocks': n.List[n.Compound](blocks), 'entities': n.List[n.Compound]([])})
        DEST.mkdir(parents=True, exist_ok=True)
        n.File(tag, gzipped=True).save(DEST / ((filename or self.key) + '.nbt'))

t = Layout('survey_tower')
# Retire the incomplete rotating instrument; keep the observation instrument static.
for y in range(2, 20):
    t.put(12, y, 8, 'air')
t.put(13, 2, 8, 'air')
t.put(12, 17, 8, 'stone_bricks')
t.box(12, 18, 8, 12, 19, 8, 'industrial_iron_block')
# The winch gearbox connects X and Z. Its Y faces intentionally have no shaft.
t.put(17, 10, 4, 'create:gearbox[axis=y]')
t.put(17, 3, 7, 'air')
t.put(17, 3, 6, 'barrel[facing=south,open=false]')
# Reach the crank from the existing stair landing; keep both stair flights unobstructed.
t.box(7, 9, 12, 18, 9, 13, 'stone_bricks')
t.box(16, 9, 3, 18, 9, 12, 'stone_bricks')
for z in range(3, 14):
    t.put(18, 10, z, 'iron_bars[east=false,north=true,south=true,west=false,waterlogged=false]')
t.put(18, 10, 4, 'create:hand_crank[facing=east,waterlogged=false]')
t.box(17, 5, 6, 17, 9, 6, 'create:rope')  # Leave a cargo opening through the access platform.

r = Layout('calibration_range')
r.size = [54, 12, 221]
# Extend the original footprint and move each target assembly to its own long lane.
old = dict(r.blocks)
for pos in list(r.blocks):
    if pos[2] >= 18:
        r.blocks.pop(pos)
r.box(0, 0, 18, 53, 0, 220, 'andesite')
r.box(37, 0, 0, 53, 0, 17, 'andesite')
r.box(2, 1, 18, 51, 1, 218, 'polished_andesite')
r.box(6, 1, 2, 51, 1, 17, 'polished_andesite')
r.box(6, 2, 2, 51, 9, 24, 'air')
r.box(5, 2, 4, 5, 5, 4, 'stone_bricks')  # Complete the front corner under the side building's roof.
for old_center, center, old_z, new_z, thickness in [(10, 10, 29, 80, 1), (21, 25, 40, 140, 2), (30, 40, 51, 200, 3)]:
    for (x, y, z), block in old.items():
        if old_center - 4 <= x <= old_center + 4 and old_z - 1 <= z <= old_z + thickness + 1 and y >= 2:
            if 'cannon_barrel' not in block:
                r.put(x + center - old_center, y, z + new_z - old_z, block)
    r.box(center - 2, 1, 5, center + 2, 1, 10, 'deepslate_tiles')
    for x in [center - 3, center + 3]:
        for z in range(6, new_z):
            r.put(x, 1, z, 'yellow_concrete' if z % 4 == 0 else 'gray_concrete')
    for z in range(28, new_z - 2, 20):
        r.box(center - 2, 1, z, center + 2, 1, z, 'white_concrete')
        r.put(center, 1, z, 'black_concrete')

for center, material, length in [(10, 'bronze', 5), (25, 'cast_iron', 4), (40, 'steel', 7)]:
    # Clear the old podium, extension and gun. A normal mount anchors its gun TWO blocks above.
    r.box(center - 1, 2, 5, center + 1, 5, 18, 'air')
    r.put(center, 1, 8, 'create:gearbox[axis=z]')  # X crank input, Y output.
    r.put(center + 1, 1, 8, 'create:shaft[axis=x]')
    r.put(center, 2, 8, 'create:shaft[axis=y]')
    r.put(center, 3, 8, 'createbigcannons:cannon_mount[assembly_powered=false,facing=south,fire_powered=false,vertical_direction=down]')
    # A speed controller takes the Z-axis crank input and drives its top X-axis cog.
    r.put(center + 1, 3, 8, 'create:clutch[axis=x,powered=false]')
    r.put(center + 2, 3, 8, 'create:large_cogwheel[axis=x]')
    r.put(center + 2, 2, 8, 'create:rotation_speed_controller[axis=z]')
    r.tags[center + 2, 2, 8] = n.Compound({'id': n.String('create:rotation_speed_controller'), 'ScrollValue': n.Int(1)})
    r.put(center + 2, 2, 7, 'create:hand_crank[facing=north,waterlogged=false]')
    r.put(center + 3, 3, 8, 'create:gearbox[axis=z]')
    r.put(center + 3, 2, 8, 'create:clutch[axis=y,powered=false]')
    r.put(center + 3, 1, 8, 'create:gearbox[axis=z]')
    r.put(center + 2, 1, 8, 'create:shaft[axis=x]')
    r.put(center + 1, 3, 7, 'lever[face=wall,facing=north,powered=false]')
    r.put(center + 3, 2, 7, 'lever[face=wall,facing=north,powered=false]')
    # Lever is on the assembly side (north), not on the firing side (south).
    r.put(center, 3, 7, 'lever[face=wall,facing=north,powered=false]')
    r.put(center, 5, 5, f'createbigcannons:{material}_sliding_breech[axis_along_first=false,facing=north]')
    r.box(center, 5, 6, center, 5, 7, f'createbigcannons:{material}_cannon_chamber[facing=north]')
    r.box(center, 5, 8, center, 5, 7 + length, f'createbigcannons:{material}_cannon_barrel[facing=north]')
    for z in range(5, 8 + length):
        connections = (['north'] if z > 5 else []) + (['south'] if z < 7 + length else [])
        entity_id = 'createbigcannons:sliding_breech' if z == 5 else 'createbigcannons:cannon'
        r.tags[center, 5, z] = n.Compound({'id': n.String(entity_id), 'Facing': n.String('north'),
                                          'Connections': n.List[n.String](connections), 'Welds': n.List[n.String]([])})
    for z in [19, 22]:
        r.put(center + 2, 2, z, 'industrial_iron_block')
    r.box(center + 2, 3, 19, center + 2, 3, 22, f'createbigcannons:{material}_cannon_barrel[facing=north]')
r.put(4, 3, 14, 'air')  # No crank on a decorative casing.

w = Layout('assembly_pad')
# Retain the fixed structural bridge, replacing two independent gantry drives with a
# single through-shaft and two native mirrored rope pulleys carrying the same beam.
for pos, block in list(w.blocks.items()):
    if 'create:gantry_' in block or 'create:rope' in block or 'create:pulley_magnet' in block:
        w.blocks.pop(pos)
w.box(7, 14, 4, 23, 15, 6, 'air')
for x in [7, 23]:
    w.box(x, 4, 5, x + 1, 13, 5, 'air')
    w.box(x, 14, 6, x, 14, 30, 'industrial_iron_block')
w.box(7, 14, 17, 23, 15, 17, 'air')
w.box(7, 15, 17, 23, 15, 17, 'industrial_iron_block')
w.box(7, 14, 17, 23, 14, 17, 'create:shaft[axis=x]')
w.put(6, 14, 17, 'create:hand_crank[facing=west,waterlogged=false]')
for x in [11, 19]:
    w.put(x, 14, 17, 'create:rope_pulley[axis=x]')
    w.box(x, 7, 17, x, 13, 17, 'create:rope')
    w.put(x, 6, 17, 'create:pulley_magnet')
w.box(11, 5, 17, 19, 5, 17, 'create:linear_chassis[axis=x,sticky_bottom=false,sticky_top=false]')
w.put(19, 4, 17, 'air')  # Loading gate: the raised beam must not attach to the guardrail.
# Clear remnants of the previous central drive and lift.
for x in range(9, 17):
    w.put(x, 13, 17, 'air')
for x in [11, 19]:
    w.put(x, 13, 17, 'create:rope')
w.box(6, 4, 16, 6, 13, 16, 'industrial_iron_block')
w.box(5, 13, 15, 8, 13, 16, 'stone_bricks')
w.box(5, 4, 16, 5, 12, 16, 'ladder[facing=west,waterlogged=false]')
# Keep the gear service bench, without the former bearing, test cube or output stub.
w.box(27, 6, 17, 29, 6, 17, 'air')
# Access to the raised work floor.
w.box(12, 1, 34, 17, 3, 36, 'air')
w.box(12, 1, 36, 17, 1, 36, 'stone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
w.box(12, 2, 35, 17, 2, 35, 'stone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
w.box(12, 3, 34, 17, 3, 34, 'stone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')

def loot_entry(item, weight=1, low=1, high=1):
    return {'type': 'minecraft:item', 'name': item, 'weight': weight,
            'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': low, 'max': high}}]}

def bonus_pool(chance, entries):
    return {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': chance}], 'entries': entries}

rare = {
    'survey_tower': [loot_entry('create:precision_mechanism', 4), loot_entry('minecraft:diamond', 3),
                     loot_entry('minecraft:golden_apple', 3), loot_entry('minecraft:netherite_scrap')],
    'calibration_range': [loot_entry('create:sturdy_sheet', 5, 1, 2), loot_entry('minecraft:diamond', 3),
                         loot_entry('create:precision_mechanism', 4), loot_entry('minecraft:netherite_scrap')],
    'assembly_pad': [loot_entry('create:precision_mechanism', 6), loot_entry('create:rotation_speed_controller', 3),
                     loot_entry('create:deployer', 2), loot_entry('create:mechanical_arm'), loot_entry('minecraft:netherite_scrap')]}
barrel_positions = {'survey_tower': [(12, 2, 6)], 'calibration_range': [(2, 2, 8)],
                    'assembly_pad': [(3, 5, 24), (3, 5, 30), (27, 4, 9), (28, 4, 25)]}
barrel_supplies = {
    'survey_tower': [('minecraft:bread', 6, 1, 3), ('minecraft:torch', 8, 3, 8), ('minecraft:copper_ingot', 6, 1, 3), ('create:shaft', 5, 1, 3)],
    'calibration_range': [('minecraft:gunpowder', 8, 1, 4), ('minecraft:charcoal', 8, 2, 5), ('minecraft:iron_ingot', 6, 1, 3), ('create:iron_sheet', 5, 1, 2)],
    'assembly_pad': [('create:andesite_alloy', 8, 1, 3), ('create:shaft', 8, 1, 3), ('create:cogwheel', 5, 1, 2), ('create:iron_sheet', 5, 1, 3)]}
loot_root = ROOT / 'src/main/resources/data/ccq_testground/loot_table'
(loot_root / 'barrels/engineering').mkdir(parents=True, exist_ok=True)
for layout, chest in [(t, (14, 2, 6)), (r, (2, 2, 10)), (w, (3, 5, 27))]:
    layout.tags[chest] = n.Compound({'id': n.String('minecraft:chest'),
                                     'LootTable': n.String('ccq_testground:chests/engineering/' + layout.key)})
    for pos in barrel_positions[layout.key]:
        assert parse(layout.blocks[pos])[0] == 'barrel'
        layout.tags[pos] = n.Compound({'id': n.String('minecraft:barrel'),
                                      'LootTable': n.String('ccq_testground:barrels/engineering/' + layout.key)})
    chest_table_path = loot_root / 'chests/engineering' / (layout.key + '.json')
    chest_table = json.loads(chest_table_path.read_text())
    chest_table['pools'] = chest_table['pools'][:1] + [bonus_pool(0.25, rare[layout.key]),
        bonus_pool(0.02, [loot_entry('minecraft:netherite_ingot'), loot_entry('minecraft:enchanted_golden_apple')])]
    chest_table_path.write_text(json.dumps(chest_table, indent=2) + '\n')
    barrel_table = {'type': 'minecraft:chest', 'pools': [
        {'rolls': {'type': 'minecraft:uniform', 'min': 1, 'max': 3},
         'entries': [loot_entry(*entry) for entry in barrel_supplies[layout.key]]},
        bonus_pool(0.05, [loot_entry('create:precision_mechanism', 3), loot_entry('minecraft:diamond', 2), loot_entry('create:sturdy_sheet', 3)])]}
    (loot_root / 'barrels/engineering' / (layout.key + '.json')).write_text(json.dumps(barrel_table, indent=2) + '\n')
    layout.export()

print('Generated corrected facility templates, firing range 54 x 221 blocks.')
