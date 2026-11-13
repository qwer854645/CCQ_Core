"""Room-specific, curated bunker inventory. No repeated generic four-item kits."""
from hashlib import sha256

def potion(kind):
    return ('potion',1,{'minecraft:potion_contents':{'potion':'minecraft:'+kind}})

PROFILES={
 'personal': [('book',3),('paper',12),('leather',6),('string',12),('white_wool',8),('shears',1),('compass',1),('clock',1),('brush',1),('fishing_rod',1),('farmersdelight:canvas',8),('farmersdelight:rope',12),('kaleidoscope_cookery:empty_cup',2),('bundle',1)],
 'rations': [('farmersdelight:chicken_sandwich',6),('farmersdelight:egg_sandwich',6),('farmersdelight:stuffed_potato',6),('farmersdelight:beef_stew',2),('farmersdelight:vegetable_soup',2),('farmersdelight:honey_cookie',16),('kaleidoscope_cookery:baozi',8),('kaleidoscope_cookery:mantou',8),('kaleidoscope_cookery:beef_noodle',2),('kaleidoscope_cookery:egg_fried_rice',4),('kaleidoscope_cookery:tea_egg',8),('dried_kelp',24)],
 'pantry': [('farmersdelight:rice',24),('farmersdelight:onion',16),('farmersdelight:tomato',16),('farmersdelight:cabbage',12),('farmersdelight:raw_pasta',16),('farmersdelight:wheat_dough',16),('kaleidoscope_cookery:flour',24),('kaleidoscope_cookery:red_chili',12),('kaleidoscope_cookery:lettuce',12),('kaleidoscope_cookery:oil',4),('sugar',16),('brown_mushroom',12),('carrot',20),('potato',20)],
 'cookware': [('bowl',16),('bucket',2),('glass_bottle',12),('farmersdelight:iron_knife',1),('farmersdelight:skillet',1),('farmersdelight:cooking_pot',1),('farmersdelight:cutting_board',1),('kaleidoscope_cookery:iron_kitchen_knife',1),('kaleidoscope_cookery:kitchen_shovel',1),('kaleidoscope_cookery:bamboo_tray',2),('kaleidoscope_cookery:stockpot_lid',1),('kaleidoscope_cookery:empty_cup',4)],
 'tea': [('farmersrespite:coffee_beans',16),('farmersrespite:green_tea_leaves',16),('farmersrespite:black_tea_leaves',16),('farmersrespite:coffee',2),('farmersrespite:green_tea',2),('farmersrespite:rose_hip_tea',2),('farmersrespite:coffee_cake_slice',6),('kaleidoscope_cookery:tieguanyin_tea_bag',8),('kaleidoscope_cookery:barley_tea_bag',8),('kaleidoscope_cookery:dried_tea_leaves',16),('honey_bottle',4),('farmersdelight:milk_bottle',4)],
 'medical': [potion('healing'),potion('regeneration'),potion('strength'),('honey_bottle',4),('milk_bucket',1),('golden_apple',2),('farmersdelight:chicken_soup',2),('farmersdelight:bone_broth',2),('white_wool',12),('paper',16),('glass_bottle',8),('shears',1)],
 'protective': [('createbigcannons:gas_mask',1),('createnuclear:default_anti_radiation_chestplate',1),('iron_boots',1),('iron_helmet',1),('leather_leggings',1),('shield',1),('createnuclear:lead_ingot',12),('farmersdelight:canvas',12),potion('fire_resistance'),potion('night_vision')],
 'tools': [('create:wrench',1),('create:goggles',1),('iron_pickaxe',1),('iron_axe',1),('iron_shovel',1),('shears',1),('flint_and_steel',1),('create:sand_paper',1),('create:red_sand_paper',1),('create:super_glue',1),('create:filter',4),('create:clipboard',1)],
 'transmission': [('create:shaft',24),('create:cogwheel',16),('create:large_cogwheel',8),('create:gearbox',4),('create:clutch',4),('create:gearshift',4),('create:encased_chain_drive',8),('create:belt_connector',8),('create:andesite_alloy',24),('create:hand_crank',2),('create:piston_extension_pole',8),('create:mechanical_bearing',2)],
 'electrical': [('redstone',24),('quartz',16),('copper_ingot',24),('create:electron_tube',8),('create:precision_mechanism',2),('create:rose_quartz',8),('create:polished_rose_quartz',8),('comparator',4),('repeater',4),('create:redstone_link',4),('create:analog_lever',2),('create:display_link',2)],
 'plumbing': [('create:fluid_pipe',24),('create:mechanical_pump',4),('create:fluid_valve',4),('create:fluid_tank',8),('create:copper_sheet',16),('create:iron_sheet',16),('copper_ingot',24),('bucket',3),('create:spout',2),('glass',16)],
 'metals': [('iron_ingot',32),('copper_ingot',24),('create:zinc_ingot',24),('create:brass_ingot',16),('create:iron_sheet',16),('create:copper_sheet',16),('create:brass_sheet',12),('create:sturdy_sheet',8),('createnuclear:steel_ingot',12),('createnuclear:lead_ingot',24),('coal',24)],
 'ordnance': [('createbigcannons:solid_shot',8),('createbigcannons:he_shell',4),('createbigcannons:shrapnel_shell',4),('createbigcannons:smoke_shell',4),('createbigcannons:powder_charge',12),('createbigcannons:impact_fuze',6),('createbigcannons:timed_fuze',4),('createbigcannons:delayed_impact_fuze',4),('createbigcannons:big_cartridge_sheet',12),('gunpowder',24),('string',16),('createbigcannons:steel_ingot',12)],
 'gun_tools': [('createbigcannons:ram_rod',1),('createbigcannons:worm',1),('createbigcannons:block_armor_inspection_tool',1),('create:wrench',1),('create:goggles',1),('createbigcannons:gas_mask',1),('spyglass',1),('iron_ingot',16),('create:iron_sheet',12),('createbigcannons:steel_ingot',12)],
 'signals': [('create:redstone_link',12),('create:analog_lever',4),('create:nixie_tube',8),('create:display_board',4),('create:display_link',4),('create:clipboard',1),('create:empty_schematic',2),('redstone',24),('repeater',4),('comparator',4),('create:electron_tube',8),('paper',16)],
 'documents': [('paper',32),('book',6),('writable_book',1),('map',4),('compass',1),('clock',1),('spyglass',1),('feather',8),('ink_sac',12),('create:clipboard',1),('create:schematic',1),('create:empty_schematic',1)],
 'nuclear_lab': [('createnuclear:lead_ingot',24),('createnuclear:steel_ingot',16),('createnuclear:graphite_rod',8),('createnuclear:reinforced_glass',12),('create:sturdy_sheet',12),('create:precision_mechanism',2),('create:electron_tube',8),('glass_bottle',12),('glass',16),('create:fluid_pipe',12),('create:mechanical_pump',2),('createnuclear:default_anti_radiation_chestplate',1)],
 'valuables': [('diamond',3),('emerald',8),('gold_ingot',12),('create:precision_mechanism',4),('createbigcannons:nethersteel_ingot',4),('createnucleararmaments:super_enriched_uranium_billet',1),('enchanted_golden_apple',1),('experience_bottle',12)]
}

def contents(profile,pos,count=8):
    pool=PROFILES[profile]
    # Different cabinets serve the same room without cloning their inventories.
    seed=int.from_bytes(sha256((profile+':'+','.join(map(str,pos))).encode()).digest()[:8],'big')
    import random
    selected=random.Random(seed).sample(pool,min(count,len(pool)))
    return selected
