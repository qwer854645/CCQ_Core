"""Rebuild industrial interiors over v86's tested mechanics; Python + nbtlib.

Keeps original stocked containers, kinetic assemblies, lodge layout and firing lines.
Run after the earlier yard/supply/lodge steps, then re-bake native neighbor states.
"""
from pathlib import Path
from copy import deepcopy
import json
import nbtlib as n
from engineering_template_cleanup import remove_names_and_signs
from split_engineering_base import main as split

ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'src/main/resources/data/ccq_testground/structure/engineering'


def main():
    data=n.load(ROOT/'tools/engineering/base_before_detail_086.nbt')
    palette=data['palette'];blocks={tuple(map(int,b['pos'])):deepcopy(b)for b in data['blocks']}
    original=deepcopy(blocks)
    def name(p):return str(palette[int(blocks[p]['state'])]['Name'])if p in blocks else 'minecraft:air'
    def lodge(p):return 29<=p[0]<=46 and 14<=p[2]<=32
    def protected(p):
        b=original.get(p,{})
        return lodge(p)or('nbt'in b and any(k in b['nbt']for k in ['Items','LootTable']))or(p[1]>=2 and name(p).startswith(('create:','createbigcannons:')))
    def put(x,y,z,block,**props):
        p=(x,y,z);assert all(0<=v<s for v,s in zip(p,[120,30,300]))
        if protected(p):return False
        if ':'not in block:block='minecraft:'+block
        state=n.Compound({'Name':n.String(block)})
        if props:state['Properties']=n.Compound({k:n.String(str(v).lower())for k,v in props.items()})
        if state not in palette:palette.append(state)
        blocks[p]=n.Compound({'pos':n.List[n.Int](p),'state':n.Int(palette.index(state))})
        return True
    def empty(x,y,z,block,**props):
        if name((x,y,z))=='minecraft:air':return put(x,y,z,block,**props)
        return False
    def box(x,y,z,X,Y,Z,block,**props):
        for xx in range(x,X+1):
            for yy in range(y,Y+1):
                for zz in range(z,Z+1):put(xx,yy,zz,block,**props)
    def erase(p):
        if not protected(p):blocks.pop(p,None)
    def lamp(x,y,z,facing='down'):
        put(x,y,z,'createdeco:yellow_industrial_iron_lamp',facing=facing,inverted=False,lit=True,waterlogged=False)
    def counter(x,y,z,facing='south'):
        put(x,y,z,'farmersdelight:spruce_cabinet',facing=facing,open=False)
    def barrel(x,y,z,facing='south'):
        put(x,y,z,'barrel',facing=facing,open=False)
    def brick_panel(x,z,X,Z,height=5):
        box(x,2,z,X,2,Z,'createdeco:dean_bricks')
        box(x,3,z,X,height,Z,'createdeco:industrial_iron_window')
        box(x,height+1,z,X,height+1,Z,'createdeco:industrial_iron_sheet_metal',axis='y')
    def bench(x,y,z,X,Z):
        box(x,y,z,X,y,Z,'kaleidoscope_cookery:table_spruce',axis='x',position=1,has_carpet=False,waterlogged=False)
    def rack(x,y,z,length=7,facing='south'):
        # Two reachable shelves of stocked barrels, with steel uprights and ties.
        for xx in [x,x+length-1]:box(xx,y,z,xx,y+4,z,'createdeco:industrial_iron_support',facing='up',waterlogged=False)
        for yy in [y+1,y+3]:
            box(x+1,yy-1,z,x+length-2,yy-1,z,'createdeco:industrial_iron_sheet_metal',axis='y')
            for xx in range(x+1,x+length-1):barrel(xx,yy,z,facing)
        box(x,y+4,z,x+length-1,y+4,z,'createdeco:industrial_iron_sheet_metal',axis='x')
    def roof_detail(x,z,X,Z,y,kind='monitor'):
        # Clerestory strips, ridge caps, cross-ribs and lamps vary each roof silhouette.
        mid=(x+X)//2
        for zz in range(z+2,Z,6):
            box(x+1,y,zz,X-1,y,zz,'createdeco:industrial_iron_sheet_metal',axis='x')
            for xx in [x+3,X-3]:lamp(xx,y-1,zz)
        if kind=='monitor':
            for xx in [mid-2,mid+2]:box(xx,y+1,z+2,xx,y+2,Z-2,'createdeco:industrial_iron_window')
            box(mid-2,y+3,z+1,mid+2,y+3,Z-1,'waxed_weathered_cut_copper_slab',type='bottom',waterlogged=False)
            box(mid-1,y,z+2,mid+1,y,Z-2,'glass')
        else:
            for xx in range(x+3,X,6):box(xx,y,z+2,xx,y,Z-2,'glass')
        for xx in [x+1,X-1]:
            for zz in [z+1,Z-1]:
                # Brick piers with metal upper supports are less monolithic than iron towers.
                for yy in range(2,y-1):
                    if name((xx,yy,zz))=='create:industrial_iron_block':
                        p=(xx,yy,zz);blocks.pop(p)
                        put(xx,yy,zz,'createdeco:dean_bricks'if yy<4 else'createdeco:industrial_iron_support',**({}if yy<4 else{'facing':'up','waterlogged':False}))
        # End plates and roof outlets, decorative rather than disconnected machinery.
        for zz in [z+3,Z-3]:
            box(X-3,y+1,zz,X-2,y+2,zz+1,'createdeco:industrial_iron_sheet_metal',axis='y')
            box(X-4,y+3,zz-1,X-1,y+3,zz+2,'stone_slab',type='bottom',waterlogged=False)

    # Remove long unbroken cabinet runs; their replacement counters have clear work roles.
    replacements=[(80,2,z)for z in [17,18,19,21,22,24,25,27]]
    replacements += [(90,2,z)for z in [48,50,51,53,55,56,58,59,60]]
    replacements += [(x,2,60)for x in range(96,110)if x%3!=0]
    replacements += [(x,2,281)for x in range(9,21)if x%3!=0]
    replacements += [(41,2,z)for z in range(267,280)if z%3!=0 and z!=278]
    for x,y,z in replacements:
        if 'cabinet'in name((x,y,z)):put(x,y,z,'createdeco:industrial_iron_sheet_metal',axis='y')

    # Canteen courtyard: a serving alcove, washable counter, food display and planted edges.
    roof_detail(61,11,83,33,7,'skylights')
    brick_panel(79,12,81,12,4)
    for x in range(77,82):counter(x,2,13,'south')
    put(78,3,13,'kaleidoscope_cookery:fruit_basket',facing='south',waterlogged=False)
    put(80,3,13,'kaleidoscope_cookery:enamel_basin',has_lid=False,oil_count=0,waterlogged=False)
    put(81,3,13,'kaleidoscope_cookery:bamboo_tray',stand=False)
    put(80,4,12,'kaleidoscope_cookery:kitchenware_racks',facing='south',waterlogged=False)
    for z in [18,24]:put(80,3,z,'flower_pot')
    put(80,3,17,'supplementaries:jar')
    put(80,3,22,'water_cauldron',level=3)
    put(82,2,30,'kaleidoscope_cookery:trash_can')
    for z in [15,30]:
        bench(64,2,z,70,z)
        put(71,2,z,'potted_azalea_bush')
    # Parking/service bays: glazed rear wall, tool wall and separate repair materials.
    roof_detail(87,11,113,35,9,'skylights')
    brick_panel(89,12,111,12,6)
    for x in [88,97,106,112]:box(x,2,12,x,8,12,'createdeco:dean_bricks')
    for x in [90,99,108]:
        put(x,3,33,'create:depot')
        put(x-1,2,33,'crafting_table');put(x+1,2,33,'stonecutter',facing='north')
        box(x-2,4,34,x+2,4,34,'createdeco:industrial_iron_sheet_metal',axis='x')
        lamp(x,6,34,'north')
        for xx in [x-3,x+3]:
            for z in range(16,32):
                if name((xx,1,z))!='minecraft:sea_lantern':put(xx,1,z,'yellow_concrete'if z%2 else'black_concrete')
    rack(89,2,32,5,'north')
    # Preparation hall: lower workbenches, blast separation, packing shelves and safe aisles.
    roof_detail(61,45,83,64,7)
    for x in [68,75]:brick_panel(x,47,x,59,3)
    for x in [65,72,79]:
        for z in [49,57]:
            for xx in range(x-1,x+2):
                for zz in range(z,z+3):
                    if name((xx,2,zz))=='minecraft:spruce_planks':put(xx,2,zz,'createdeco:industrial_iron_sheet_metal',axis='y')
            # Separate tables have gloves/packing stock in their existing side barrels.
            put(x+1,3,z+1,'create:depot')
    rack(69,2,62,7,'north')
    # Press and mixer hall: glazed rear wall, storage drawers, tooling and task lamps.
    roof_detail(87,45,113,64,10)
    brick_panel(89,46,111,46,7)
    for x in [89,95,103,111]:box(x,2,46,x,9,46,'createdeco:dean_bricks')
    for z in [48,51,57,60]:
        empty(90,3,z,'stonecutter',facing='east')
        box(89,4,z,89,6,z,'createdeco:industrial_iron_sheet_metal',axis='y')
        lamp(90,6,z,'east')
    for x in [97,101,105,109]:
        empty(x,3,60,'create:depot')
        lamp(x,6,61)
        box(x,7,61,x,9,61,'chain',axis='y',waterlogged=False)
    rack(92,2,63,8,'north')
    # Low brick screens mark operator areas, leaving the actual hand cranks unobstructed.
    box(94,2,50,94,3,53,'createdeco:dean_bricks')
    box(104,2,50,104,3,53,'createdeco:dean_bricks')
    for x in range(95,112):
        for z in [47,55]:
            if name((x,1,z))!='minecraft:sea_lantern':put(x,1,z,'yellow_concrete'if x%2 else'black_concrete')

    # Original warehouse: packing counter, steel roof ribs, glazed loading-side wall.
    roof_detail(6,43,34,59,8)
    for z in [45,57]:
        for x in range(9,32,5):
            empty(x,5,z,'createdeco:industrial_iron_sheet_metal',axis='x')
            empty(x,6,z,'createdeco:industrial_iron_window')
    for x in [24,27]:
        empty(x,2,55,'crafting_table');counter(x+1,2,55,'north')
        empty(x+1,3,55,'create:depot')
    rack(25,2,57,7,'north')
    # Survey tower: masonry foundation accents and a small outside survey preparation bench.
    bench(9,2,31,13,32);counter(14,2,31,'south')
    put(10,3,31,'supplementaries:globe',facing='south')
    put(12,3,31,'supplementaries:hourglass',facing='up')
    for x in [9,14]:
        box(x,2,35,x,5,35,'createdeco:dean_bricks');lamp(x,6,35,'up')
    # Old workshop: clad retaining walls, real workbench furniture on its raised deck.
    for (x,y,z),b in list(blocks.items()):
        if 11<=x<=39 and 78<=z<=112 and 2<=y<=3 and name((x,y,z))=='minecraft:stone_bricks':
            put(x,y,z,'createdeco:dean_bricks')
    for z in [82,88,94,106]:
        for x in [12,38]:
            if name((x,5,z))=='minecraft:air':
                counter(x,5,z,'east'if x==12 else'west')
                empty(x,6,z,'create:depot')
    for z in [85,101,109]:
        empty(37,5,z,'stonecutter',facing='west')
        for y in range(5,9):empty(39,y,z,'createdeco:industrial_iron_support',facing='up',waterlogged=False)
        empty(38,8,z,'createdeco:industrial_iron_sheet_metal',axis='x')
        empty(38,7,z,'createdeco:yellow_industrial_iron_lamp',facing='down',inverted=False,lit=True,waterlogged=False)
    # Material racks along its outside are now usable stocked barrels, not empty props.
    for z in [82,94,106]:rack(5,2,z,4,'east')
    # Loading apron and assembly yard: safety borders, packing desk and reachable spares.
    for z in [124,130]:
        for x in range(9,40):
            if name((x,1,z))!='minecraft:sea_lantern':put(x,1,z,'yellow_concrete'if x%2 else'black_concrete')
    bench(8,2,133,13,134);counter(14,2,133,'south')
    for z in [140,163,179]:
        rack(36,2,z,8,'south')
    for x in [7,43]:
        for z in [134,158,181]:
            empty(x,2,z,'createdeco:dean_bricks');empty(x,3,z,'createdeco:dean_bricks')
            empty(x,4,z,'createdeco:industrial_iron_support',facing='up',waterlogged=False)
            empty(x,5,z,'createdeco:yellow_industrial_iron_lamp',facing='up',inverted=False,lit=True,waterlogged=False)
    # Materials hall: retaining bins, a roof monitor and a cutting/packing station.
    roof_detail(5,193,43,220,8)
    for x in [17,29]:
        box(x,2,196,x,4,215,'createdeco:dean_bricks')
        box(x,5,196,x,5,215,'stone_brick_slab',type='bottom',waterlogged=False)
    brick_panel(7,194,41,194,5)
    for x in [10,22,34]:
        counter(x,2,218,'north');put(x+1,2,218,'stonecutter',facing='north')
        put(x,3,218,'create:depot')
    # Waterworks: inspection walk, treatment bench and enclosed maintenance cabinet bay.
    roof_detail(29,231,41,249,7,'skylights')
    brick_panel(40,233,40,247,4)
    bench(6,2,246,14,247)
    for x in [7,10,13]:put(x,3,246,'kaleidoscope_cookery:enamel_basin',has_lid=False,oil_count=0,waterlogged=False)
    counter(15,2,246,'south')
    put(16,2,246,'water_cauldron',level=3)
    for z in range(232,242):
        if name((7,1,z))!='minecraft:sea_lantern':put(7,1,z,'createdeco:industrial_iron_catwalk',bottom=True)
    for x in [30,34]:
        for z in [234,243]:
            # Replace featureless bulk blocks with banded filter housings, not fake kinetic devices.
            box(x,3,z,x+2,3,z+3,'createdeco:copper_sheet_metal',axis='y')
            for xx in [x,x+2]:box(xx,4,z,xx,4,z+3,'createdeco:industrial_iron_sheet_metal',axis='z')
    # Rear stores and control office: distinct enclosed workspaces with windows and desks.
    for x,X in [(6,23),(27,43)]:
        roof_detail(x,263,X,284,8,'skylights')
        for (xx,yy,zz),b in list(blocks.items()):
            if x<=xx<=X and 263<=zz<=284 and 2<=yy<=7 and name((xx,yy,zz))=='minecraft:stone_bricks':put(xx,yy,zz,'createdeco:dean_bricks')
        brick_panel(x+2,264,X-2,264,5)
        # Keep a broad central entrance through the new facade.
        center=(x+X)//2
        for xx in range(center-1,center+2):
            for yy in range(2,6):erase((xx,yy,264))
        for xx in [x+2,X-2]:lamp(xx,6,265,'south')
    # Replace two meaningless solid timber masses with accessible drafting benches.
    for x in [10,17]:
        for xx in range(x,x+3):
            for y in [2,3]:
                for z in range(268,273):erase((xx,y,z))
        bench(x,2,268,x+2,269)
        counter(x,2,272,'north');put(x+1,2,272,'crafting_table')
        put(x,3,268,'supplementaries:hourglass',facing='up')
    rack(8,2,277,8,'north')
    put(31,3,268,'supplementaries:globe',facing='south')
    put(33,3,274,'supplementaries:hourglass',facing='up')
    put(31,3,274,'kaleidoscope_cookery:teapot')
    put(32,3,274,'kaleidoscope_cookery:empty_cup',cup_count=2,facing='south')
    put(40,4,282,'supplementaries:clock_block',facing='north',two_faced=False)
    # Range: small reinforced ammunition service points outside all three firing corridors.
    for z in [121,181,241]:
        box(60,2,z,60,4,z+7,'createdeco:dean_bricks')
        box(58,5,z,60,5,z+7,'waxed_weathered_cut_copper_slab',type='bottom',waterlogged=False)
        for zz in [z+2,z+5]:barrel(59,2,zz,'west')
        lamp(59,4,z+4,'west')
    # Range building now has industrial brick accents, task lights and maintenance storage.
    for (x,y,z),b in list(blocks.items()):
        if 61<=x<=65 and 72<=z<=85 and y in [2,3]and name((x,y,z))=='minecraft:stone_bricks':
            # Preserve the four tested structural columns in original stone.
            if x in [61,65]and z in [72,85]:continue
            put(x,y,z,'createdeco:dean_bricks')
    for z in [73,80]:empty(63,5,z,'createdeco:yellow_industrial_iron_lamp',facing='down',inverted=False,lit=True,waterlogged=False)
    # Site-wide drains are flush and pedestrian crossings stay at the established grade.
    for z in [40,73,119,188,225,258,290]:
        for x in range(47,56):
            if name((x,1,z))!='minecraft:sea_lantern':put(x,1,z,'white_concrete')
    for z in range(8,292):
        for x in [45,57]:
            if name((x,1,z))!='minecraft:sea_lantern':put(x,1,z,'createdeco:industrial_iron_catwalk',bottom=True)

    # Each surviving cabinet and previously empty barrel/chest receives actual curated stock.
    # Separate categories and deterministic subsets prevent every drawer looking identical.
    themes={
      'canteen': [('bowl',12),('bread',16),('cooked_beef',8),('carrot',16),('potato',16),('apple',12),('sugar',12),('glass_bottle',8),('wheat',24),('farmersdelight:rice',16),('farmersdelight:canvas',6),('farmersdelight:iron_knife',1)],
      'tools': [('iron_ingot',16),('create:iron_sheet',12),('create:andesite_alloy',12),('create:cogwheel',8),('create:shaft',12),('create:belt_connector',4),('create:electron_tube',4),('create:wrench',1),('create:brass_sheet',8),('redstone',12),('copper_ingot',16),('create:precision_mechanism',2)],
      'range': [('createbigcannons:solid_shot',4),('createbigcannons:ap_shot',2),('createbigcannons:powder_charge',4),('createbigcannons:empty_powder_charge',8),('createbigcannons:ram_rod',1),('createbigcannons:worm',1),('createbigcannons:impact_fuze',2),('createbigcannons:big_cartridge_sheet',8),('gunpowder',16),('paper',16),('string',12),('iron_ingot',12)],
      'materials': [('andesite',32),('stone_bricks',32),('spruce_planks',32),('scaffolding',12),('glass',16),('iron_ingot',16),('create:andesite_casing',8),('copper_ingot',16),('create:iron_sheet',12),('create:andesite_alloy',16),('coal',16),('stick',24)],
      'water': [('bucket',2),('glass_bottle',12),('create:fluid_pipe',16),('create:mechanical_pump',2),('create:copper_casing',4),('copper_ingot',16),('create:copper_sheet',12),('iron_ingot',12),('charcoal',16),('sand',16),('sponge',2),('create:wrench',1)],
      'office': [('paper',24),('book',6),('ink_sac',8),('compass',1),('clock',1),('spyglass',1),('redstone',12),('map',4),('glass_bottle',4),('bread',8),('feather',6),('create:clipboard',1)],
      'lodge': [('bread',12),('apple',8),('baked_potato',12),('bowl',8),('paper',12),('book',4),('string',8),('white_wool',8),('leather',8),('glass_bottle',6),('coal',12),('farmersdelight:canvas',4)]}
    workpieces=0
    for p,b in sorted(blocks.items()):
        if name(p)!='create:depot'or p in original and str(palette[int(original[p]['state'])]['Name'])=='create:depot':continue
        x,y,z=p
        item=('createbigcannons:big_cartridge_sheet'if 61<=x<=83 and 45<=z<=64 else'create:copper_sheet'if z>=229 else'create:iron_sheet'if x%2 else'create:andesite_alloy')
        b['nbt']=n.Compound({'id':n.String('create:depot'),
            'HeldItem':n.Compound({'Item':n.Compound({'id':n.String(item),'count':n.Int(3)}),
                'Pos':n.Float(.5),'PrevPos':n.Float(.5),'Offset':n.Float(0),'PrevOffset':n.Float(0),'Angle':n.Int(0),'InDirection':n.Int(2)}),
            'OutputBuffer':n.Compound({'Size':n.Int(8),'Items':n.List[n.Compound]()}),'Incoming':n.List[n.Compound]()})
        workpieces+=1
    filled=[]
    for p,b in sorted(blocks.items()):
        nm=name(p);be=b.get('nbt',{})
        if 'Items'in be or 'LootTable'in be:continue
        if not(nm=='farmersdelight:spruce_cabinet'or nm in ['minecraft:barrel','minecraft:chest']):continue
        x,y,z=p
        theme=('lodge'if lodge(p)else'canteen'if x>=61 and z<=33 else'range'if x>=58 and z>=45 and x<=83 else'water'if 229<=z<=252 else'office'if z>=263 or(z<=38 and x<29)else'materials'if 193<=z<=220 else'tools')
        choices=themes[theme];start=(x*7+z*3+y)%len(choices)
        items=[choices[(start+i*5)%len(choices)]for i in range(6)]
        identifier='farmersdelight:cabinet'if 'cabinet'in nm else nm
        b['nbt']=n.Compound({'id':n.String(identifier),'Items':n.List[n.Compound]([
            n.Compound({'Slot':n.Byte(slot),'id':n.String(item if ':'in item else'minecraft:'+item),'count':n.Int(count)})for slot,(item,count)in zip([0,2,4,9,11,13],items)])})
        filled.append({'pos':list(p),'theme':theme,'items':items})
    remove_names_and_signs(blocks,palette)
    for p,b in original.items():
        if 'nbt'in b and any(k in b['nbt']for k in ['Items','LootTable']):assert blocks[p]==b,('Existing stock changed',p)
        if lodge(p):
            assert blocks[p]['state']==b['state'],('Lodge layout changed',p)
        # Only originally structural iron columns were reclad; functioning mechanisms are untouched.
        if p[1]>=2 and str(palette[int(b['state'])]['Name']).startswith(('create:','createbigcannons:'))and str(palette[int(b['state'])]['Name'])!='create:industrial_iron_block':assert blocks[p]==b,('Mechanism changed',p)
    data['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)])
    data.save(FOLDER/'engineering_base.nbt')
    # Lodge standalone receives identical stock while keeping every existing block and supply.
    path=FOLDER/'duty_lodge.nbt';lod=n.load(path)
    for b in lod['blocks']:
        p=tuple(int(v)+o for v,o in zip(b['pos'],[29,0,14]))
        if p in blocks and 'nbt'in blocks[p]:b['nbt']=deepcopy(blocks[p]['nbt'])
    lod.save(path)
    (ROOT/'tools/engineering/detail_stock.json').write_text(json.dumps(filled,ensure_ascii=False,indent=2),encoding='utf-8')
    print('Detailed base:',len(blocks),'blocks;',len(filled),'newly stocked containers;',workpieces,'workpiece depots',flush=True)
    split()


if __name__=='__main__':main()
