"""Author fixed warehouse stock and richer lodge loot, then export base sections.

Run with Python + nbtlib. Re-running replaces the same authored stock and tables.
"""
from pathlib import Path
import json
import nbtlib as n
from split_engineering_base import main as split
from engineering_template_cleanup import remove_names_and_signs

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/ccq_testground"

# Five racks, eight containers per rack. Counts are fixed item NBT, never loot rolls.
# Unqualified IDs are vanilla; Create and CBC IDs remain explicit.
RACKS = [
    (10, "建材仓", [
        ("安山岩", [("andesite",64),("andesite",64),("andesite",64),("polished_andesite",32)]),
        ("石材", [("stone",64),("stone",64),("stone_bricks",64)]),
        ("圆石", [("cobblestone",64)]*4),
        ("深板岩", [("deepslate",64),("deepslate",64),("cobbled_deepslate",64)]),
        ("木材", [("spruce_planks",64),("spruce_planks",64),("spruce_log",32),("stick",64)]),
        ("工业建材", [("create:industrial_iron_block",16),("create:andesite_casing",16),("scaffolding",32)]),
        ("玻璃与饰面", [("glass",64),("glass_pane",32),("white_concrete",32)]),
        ("轨道", [("rail",64),("powered_rail",16),("redstone_torch",16)]),
    ]),
    (14, "金属仓", [
        ("铁", [("iron_ingot",64),("iron_ingot",64),("iron_nugget",64),("iron_block",8)]),
        ("铜", [("copper_ingot",64),("copper_ingot",64),("copper_ingot",64),("copper_block",16)]),
        ("锌", [("create:zinc_ingot",64),("create:zinc_ingot",64)]),
        ("黄铜", [("create:brass_ingot",32),("create:brass_ingot",32)]),
        ("金属板", [("create:iron_sheet",64),("create:copper_sheet",64),("create:brass_sheet",32)]),
        ("贵金属", [("gold_ingot",32),("create:golden_sheet",16),("emerald",8)]),
        ("重型材料", [("create:sturdy_sheet",32),("createbigcannons:cast_iron_ingot",32),("createbigcannons:steel_ingot",32)]),
        ("精加工材料", [("diamond",8),("quartz",32),("amethyst_shard",32),("netherite_scrap",2)]),
    ]),
    (18, "传动仓", [
        ("传动轴", [("create:shaft",64),("create:shaft",64),("create:andesite_alloy",32)]),
        ("齿轮", [("create:cogwheel",64),("create:large_cogwheel",32)]),
        ("齿轮箱与皮带", [("create:gearbox",16),("create:vertical_gearbox",8),("create:belt_connector",32)]),
        ("离合与换向", [("create:clutch",16),("create:gearshift",16),("create:sequenced_gearshift",4)]),
        ("链式传动", [("create:encased_chain_drive",32),("create:adjustable_chain_gearshift",8)]),
        ("活塞与龙门", [("create:mechanical_piston",8),("create:piston_extension_pole",32),("create:gantry_shaft",32),("create:gantry_carriage",8)]),
        ("轴承与吊运", [("create:mechanical_bearing",4),("create:rope_pulley",4),("create:linear_chassis",16),("create:radial_chassis",8)]),
        ("动力源", [("create:water_wheel",4),("create:large_water_wheel",2),("create:hand_crank",8)]),
    ]),
    (22, "装配控制仓", [
        ("红石", [("redstone",64),("redstone",64),("repeater",16),("comparator",8)]),
        ("精密零件", [("create:electron_tube",32),("create:precision_mechanism",16)]),
        ("机壳", [("create:brass_casing",32),("create:copper_casing",32),("create:andesite_casing",32)]),
        ("调速与仪表", [("create:rotation_speed_controller",4),("create:stressometer",4),("create:speedometer",4)]),
        ("加工设备", [("create:deployer",4),("create:mechanical_arm",2),("create:mechanical_press",2),("create:mechanical_mixer",2)]),
        ("流体设备", [("create:mechanical_pump",8),("create:fluid_pipe",64),("create:fluid_tank",8)]),
        ("物流设备", [("create:brass_funnel",16),("create:andesite_funnel",16),("create:brass_tunnel",8),("create:smart_chute",8)]),
        ("维修用品", [("create:wrench",1),("create:goggles",1),("create:clipboard",1),("create:super_glue",1)]),
    ]),
    (26, "火炮仓", [
        ("实心弹与穿甲弹", [("createbigcannons:solid_shot",16),("createbigcannons:ap_shot",8)]),
        ("发射药", [("createbigcannons:powder_charge",16),("createbigcannons:packed_gunpowder",16)]),
        ("火药原料", [("gunpowder",64),("gunpowder",64),("createbigcannons:nitropowder",32)]),
        ("炮管备件", [("createbigcannons:cast_iron_cannon_barrel",8),("createbigcannons:bronze_cannon_barrel",8),("createbigcannons:steel_cannon_barrel",4)]),
        ("炮钢与铸铁", [("createbigcannons:steel_ingot",32),("createbigcannons:cast_iron_ingot",32)]),
        ("装填工具", [("createbigcannons:ram_rod",1),("createbigcannons:worm",1)]),
        ("引信", [("createbigcannons:impact_fuze",8),("createbigcannons:timed_fuze",8),("createbigcannons:proximity_fuze",4)]),
        ("药筒与药包材料", [("createbigcannons:big_cartridge_sheet",16),("createbigcannons:autocannon_cartridge_sheet",16),("createbigcannons:empty_powder_charge",16)]),
    ]),
]


def full_id(value):
    return value if ":" in value else "minecraft:"+value


def stock(kind, items):
    return n.Compound({"id":n.String("minecraft:"+kind),
        "Items":n.List[n.Compound]([n.Compound({"Slot":n.Byte(i),"id":n.String(full_id(item)),"count":n.Int(count)})
                                   for i,(item,count) in enumerate(items)])})


def entry(item, low, high, weight=1, components=None):
    functions=[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":low,"max":high}}]
    if components: functions.append({"function":"minecraft:set_components","components":components})
    return {"type":"minecraft:item","name":full_id(item),"weight":weight,"functions":functions}


def pool(low, high, entries, chance=None):
    result={"rolls":{"type":"minecraft:uniform","min":low,"max":high},"entries":entries}
    if chance is not None: result["conditions"]=[{"condition":"minecraft:random_chance","chance":chance}]
    return result


def write_table(name, pools):
    p=DATA/"loot_table"/name;p.parent.mkdir(exist_ok=True,parents=True)
    p.write_text(json.dumps({"type":"minecraft:chest","pools":pools},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def main():
    path=DATA/"structure/engineering/engineering_base.nbt"
    master=n.load(path);blocks={tuple(map(int,b["pos"])):b for b in master["blocks"]}
    palette=master["palette"]
    chest=next(i for i,s in enumerate(palette) if str(s["Name"])=="minecraft:chest" and str(s.get("Properties",{}).get("facing"))=="east")
    barrel=next(i for i,s in enumerate(palette) if str(s["Name"])=="minecraft:barrel" and str(s.get("Properties",{}).get("facing"))=="west") if any(str(s["Name"])=="minecraft:barrel" and str(s.get("Properties",{}).get("facing"))=="west" for s in palette) else len(palette)
    if barrel==len(palette):palette.append(n.Compound({"Name":n.String("minecraft:barrel"),"Properties":n.Compound({"facing":n.String("west"),"open":n.String("false")})}))
    for x,title,bins in RACKS:
        positions=[(xx,y,z) for y in [2,4] for z in [47,46] for xx in [x,x+1]]
        for pos,(label,items) in zip(positions,bins,strict=True):
            assert str(palette[int(blocks[pos]["state"])]["Name"])=="minecraft:barrel"
            blocks[pos]["nbt"]=stock("barrel",items)
    blocks[29,2,55]["nbt"]=stock("chest",[("iron_pickaxe",1),("iron_axe",1),("iron_shovel",1),
        ("shears",1),("flint_and_steel",1),("water_bucket",1),("bucket",4),("torch",64),("lantern",16),("create:brown_toolbox",1)])
    locations=[((35,2,15),None,"barrel","pantry","餐厨 / 食物储备"),
               ((30,7,30),None,"chest","duty_lodge","宿舍 / 生活用品"),
               ((30,2,21),chest,"chest","first_aid","休息区 / 医疗与应急"),
               ((34,7,30),chest,"chest","duty_lodge","宿舍 / 个人用品"),
               ((41,7,16),barrel,"barrel","duty_lodge","宿舍 / 衣物与杂物")]
    for pos,state,kind,table,title in locations:
        if state is not None:
            assert pos not in blocks or str(palette[int(blocks[pos]["state"])]["Name"])=="minecraft:"+kind
            blocks[pos]=n.Compound({"pos":n.List[n.Int](pos),"state":n.Int(state)})
        blocks[pos]["nbt"]=n.Compound({"id":n.String("minecraft:"+kind),"LootTable":n.String("ccq_testground:chests/engineering/"+table)})
    remove_names_and_signs(blocks,palette)
    master["blocks"]=n.List[n.Compound]([blocks[p] for p in sorted(blocks)])
    master.save(path)

    # Increase all six original supply tables without compounding on repeated runs.
    for kind in ["chests","barrels"]:
        for key in ["survey_tower","assembly_pad","calibration_range"]:
            p=DATA/f"loot_table/{kind}/engineering/{key}.json";data=json.loads(p.read_text(encoding="utf-8"));pools=data["pools"]
            pools[0]["rolls"]={"type":"minecraft:uniform","min":8 if kind=="chests" else 4,"max":12 if kind=="chests" else 7}
            for e in pools[0]["entries"]:
                item=e["name"];low,high=(8,24) if kind=="chests" else (4,12)
                if item.endswith("wrench"):low,high=1,1
                elif item.endswith(("large_cogwheel","cogwheel","electron_tube")):low,high=(4,12) if kind=="chests" else (2,6)
                for f in e.get("functions",[]):
                    if f["function"]=="minecraft:set_count":f["count"]={"type":"minecraft:uniform","min":low,"max":high}
            pools[1]["conditions"][0]["chance"]=0.65 if kind=="chests" else 0.20
            if kind=="chests":pools[2]["conditions"][0]["chance"]=0.06
            p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

    food=[entry(i,4,12,w) for i,w in [("bread",8),("cooked_beef",6),("cooked_porkchop",6),("baked_potato",8),("apple",5),("carrot",5),("cookie",4),("pumpkin_pie",3)]]
    write_table("chests/engineering/pantry.json",[pool(8,12,food),pool(2,3,[entry("honey_bottle",1,3),entry("milk_bucket",1,1),entry("sugar",4,12)]),pool(1,1,[entry("golden_apple",1,2)],0.35)])
    household=[entry("torch",16,32,8),entry("paper",12,32,6),entry("book",2,6,5),entry("string",6,16,5),entry("leather",4,12,4),
               entry("white_wool",4,12,4),entry("candle",4,12,4),entry("flower_pot",1,3,2),entry("painting",1,2,2),entry("compass",1,1,2),
               entry("clock",1,1,2),entry("fishing_rod",1,1,2),entry("shears",1,1,2),entry("water_bucket",1,1,2),entry("name_tag",1,2,1)]
    write_table("chests/engineering/duty_lodge.json",[pool(7,10,household),pool(2,3,food),pool(1,1,[entry("red_bed",1,1),entry("campfire",1,1)]),
        pool(1,1,[entry("golden_apple",1,3),entry("emerald",3,8),entry("diamond",1,3)],0.65),pool(1,1,[entry("enchanted_golden_apple",1,1)],0.06)])
    write_table("chests/engineering/first_aid.json",[pool(2,3,[entry("potion",1,1,components={"minecraft:potion_contents":{"potion":"minecraft:healing"}})]),
        pool(1,2,[entry("potion",1,1,components={"minecraft:potion_contents":{"potion":"minecraft:regeneration"}})]),
        pool(3,5,[entry("honey_bottle",2,4,4),entry("golden_carrot",4,12,4),entry("bread",6,16,3),entry("white_wool",4,12,3),entry("milk_bucket",1,1,2)]),
        pool(1,1,[entry("golden_apple",2,4)])])
    split()
    print("Warehouse: 40 categorized barrels + 1 tool chest; lodge: 5 stocked containers")


if __name__=="__main__":main()
