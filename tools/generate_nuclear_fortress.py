"""Independent, editable nuclear-reserve fortress prototype; no instance writes."""
from pathlib import Path
from copy import deepcopy
import json,math,collections,hashlib
import nbtlib as n

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'tools/engineering/nuclear_fortress_v1'
SIZE=[96,40,96]

def main():
    palette=[];indices={};blocks={};features={};containers=[]
    def put(x,y,z,block,**props):
        p=(x,y,z);assert all(0<=v<limit for v,limit in zip(p,SIZE)),p
        if ':'not in block:block='minecraft:'+block
        signature=(block,tuple(sorted((k,str(v).lower())for k,v in props.items())))
        if signature not in indices:
            state=n.Compound({'Name':n.String(block)})
            if props:state['Properties']=n.Compound({k:n.String(str(v).lower())for k,v in props.items()})
            indices[signature]=len(palette);palette.append(state)
        blocks[p]=n.Compound({'pos':n.List[n.Int](p),'state':n.Int(indices[signature])})
    def box(x,y,z,X,Y,Z,block,**props):
        for xx in range(x,X+1):
            for yy in range(y,Y+1):
                for zz in range(z,Z+1):put(xx,yy,zz,block,**props)
    def name(p):return str(palette[int(blocks[p]['state'])]['Name'])if p in blocks else'minecraft:air'
    def clear(x,y,z,X,Y,Z):box(x,y,z,X,Y,Z,'air')
    def stock(x,y,z,items,kind='barrel',facing='south'):
        props={'facing':facing,'open':False}if kind=='barrel'else{'facing':facing,'type':'single','waterlogged':False}
        put(x,y,z,kind,**props)
        identifier='minecraft:'+kind
        blocks[x,y,z]['nbt']=n.Compound({'id':n.String(identifier),'Items':n.List[n.Compound]([
            n.Compound({'Slot':n.Byte(slot),'id':n.String(item if ':'in item else'minecraft:'+item),'count':n.Int(count)})
            for slot,(item,count)in zip([0,2,4,9,11,13,18,20,22],items)])})
        containers.append({'pos':[x,y,z],'items':items})
    def lamp(x,y,z):put(x,y,z,'lantern',hanging=True,waterlogged=False)
    def railing(x,y,z,X,Z):
        for xx in range(x,X+1):
            for zz in range(z,Z+1):
                if xx in [x,X]or zz in [z,Z]:put(xx,y,zz,'iron_bars',north=False,south=False,east=False,west=False,waterlogged=False)
    def room(x,z,X,Z,roof,material='deepslate_bricks'):
        box(x,9,z,X,roof-1,z,material);box(x,9,Z,X,roof-1,Z,material)
        box(x,9,z,x,roof-1,Z,material);box(X,9,z,X,roof-1,Z,material)
        box(x,roof,z,X,roof,Z,'polished_deepslate')
        for xx in range(x+3,X,6):
            for zz in [z,Z]:
                box(xx,11,zz,min(xx+1,X-1),12,zz,'createnuclear:reinforced_glass')
        for xx in range(x+3,X,6):
            for zz in range(z+3,Z,6):lamp(xx,roof-1,zz)
    # The underground authoring offset is eight blocks; surrounding terrain hides
    # the perimeter substrate in the overview just as it will in a real placement.
    box(2,0,2,93,8,93,'andesite')
    box(5,8,5,90,8,90,'polished_andesite')
    for v in [8,27,68,87]:
        box(v,8,8,v,8,87,'deepslate_tiles');box(8,8,v,87,8,v,'deepslate_tiles')
    # Thick curtain walls, internal wall walks, alternating merlons and buttresses.
    for side in ['west','east','north','south']:
        if side in ['west','east']:
            x=6 if side=='west'else 87
            box(x,9,16,x+2,16,79,'deepslate_bricks')
            box(x-1 if side=='west'else x,16,16,x+2 if side=='west'else x+3,16,79,'polished_deepslate')
            edge=x if side=='west'else x+2
            for z in range(17,79):
                if z%5<3:box(edge,17,z,edge,18,z,'deepslate_bricks')
            for z in range(25,77,9):
                ox=x-1 if side=='west'else x+3;box(ox,9,z,ox,13,z+1,'polished_basalt',axis='y')
        else:
            z=6 if side=='north'else 87
            box(16,9,z,79,16,z+2,'deepslate_bricks');box(16,16,z-1 if side=='north'else z,79,16,z+2 if side=='north'else z+3,'polished_deepslate')
            edge=z if side=='north'else z+2
            for x in range(17,79):
                if x%5<3:box(x,17,edge,x,18,edge,'deepslate_bricks')
    # Four octagonal bastions; their roof decks are connected to the wall walk.
    for cx,cz in [(13,13),(82,13),(13,82),(82,82)]:
        for x in range(cx-10,cx+11):
            for z in range(cz-10,cz+11):
                dx,dz=abs(x-cx),abs(z-cz)
                if dx+dz>15:continue
                box(x,8,z,x,9,z,'polished_deepslate')
                boundary=dx>=9 or dz>=9 or dx+dz>=14
                if boundary:box(x,10,z,x,16,z,'deepslate_bricks')
                put(x,17,z,'polished_deepslate')
                if boundary and (x+z)%5<3:box(x,18,z,x,19,z,'deepslate_bricks')
        for x,z in [(cx-6,cz),(cx+6,cz),(cx,cz-6),(cx,cz+6)]:put(x,17,z,'sea_lantern')
        # Open access arches on the inward-facing side, not sealed empty towers.
        if cx<48:clear(cx+8,10,cz-1,cx+10,14,cz+1)
        else:clear(cx-10,10,cz-1,cx-8,14,cz+1)
        stock(cx-4,10,cz-3,[('arrow',24),('bread',10),('iron_ingot',8),('create:iron_sheet',8)])
    # Main vaulted entrance, two gatehouses, raised portcullis and passage.
    clear(43,9,85,52,15,90)
    for x in [40,53]:box(x,9,84,x+2,20,91,'polished_deepslate')
    box(40,16,85,55,20,90,'deepslate_bricks')
    box(43,16,87,52,17,87,'iron_bars',north=False,south=False,east=True,west=True,waterlogged=False)
    box(42,21,84,53,21,91,'polished_deepslate');railing(42,22,84,53,91)
    clear(45,9,5,50,13,9)
    # Courtyard avenues and the loading apron.
    box(44,8,48,51,8,86,'gray_concrete');box(28,8,53,67,8,58,'gray_concrete')
    for z in range(51,86,6):box(47,8,z,48,8,z+2,'white_concrete')
    for x,z in [(30,39),(65,39),(30,64),(65,64)]:
        box(x,9,z,x,12,z,'createdeco:industrial_iron_support',facing='up',waterlogged=False)
        put(x,13,z,'sea_lantern')
    for x in range(68,81):
        for z in [51,54]:put(x,9,z,'rail',shape='east_west',waterlogged=False)
    for z in [50,55]:box(68,8,z,80,8,z,'yellow_concrete')
    # Surface keep: thick lower storey, glazed command room, roof observation deck.
    room(30,13,65,34,16)
    box(30,9,13,32,15,34,'deepslate_bricks');box(63,9,13,65,15,34,'deepslate_bricks')
    clear(44,9,32,51,13,34)
    room(36,17,59,30,24,'stone_bricks')
    box(37,17,18,58,17,29,'polished_andesite')
    for x in range(39,57,4):
        box(x,19,17,x+1,21,17,'createnuclear:reinforced_glass')
        box(x,19,30,x+1,21,30,'createnuclear:reinforced_glass')
    railing(36,25,17,59,30)
    # Access to the upper command floor and wall walk, with complete stair treads.
    clear(32,9,24,35,21,33)
    for step in range(8):
        for x in range(32,35):
            if step:box(x,9,32-step,x,8+step,32-step,'deepslate_bricks')
            put(x,9+step,32-step,'deepslate_brick_stairs',facing='north',half='bottom',shape='straight',waterlogged=False)
    clear(33,17,25,35,20,27)
    box(33,16,24,36,16,27,'polished_deepslate')
    clear(36,18,25,36,21,27)
    for cx,cz in [(13,13),(82,13),(13,82),(82,82)]:
        for step in range(8):
            x=cx+(step-4)
            for z in [cz+5,cz+6]:
                if step:box(x,10,z,x,9+step,z,'deepslate_bricks')
                clear(x,11+step,z,x,13+step,z)
            put(x,10+step,cz+5,'deepslate_brick_stairs',facing='east',half='bottom',shape='straight',waterlogged=False)
            put(x,10+step,cz+6,'deepslate_brick_stairs',facing='east',half='bottom',shape='straight',waterlogged=False)
    # Underground vault: four independent recessed payload bays and a common hall.
    clear(29,2,12,66,7,39)
    box(29,1,12,66,1,39,'deepslate_tiles')
    box(28,1,11,67,7,11,'createnuclear:lead_block')
    box(28,1,12,28,7,39,'createnuclear:lead_block');box(67,1,12,67,7,39,'createnuclear:lead_block')
    box(29,7,12,66,7,39,'createnuclear:steel_block')
    charges=[]
    for x,tier in [(34,1),(43,1),(52,2),(61,3)]:
        box(x-3,2,13,x+3,6,13,'createnuclear:lead_block')
        box(x-4,2,13,x-4,6,24,'createnuclear:lead_block')
        box(x+4,2,13,x+4,6,24,'createnuclear:lead_block')
        box(x-3,1,14,x+3,1,23,'polished_deepslate')
        box(x-1,2,17,x+1,2,19,'createnuclear:steel_block')
        put(x,3,18,f'createnucleararmaments:nuclear_charge_t{tier}')
        charges.append({'pos':[x,3,18],'tier':tier})
        for xx in [x-2,x+2]:put(xx,1,23,'yellow_concrete')
        put(x,1,24,'black_concrete');lamp(x,6,21)
        # Loading opening and a viewing slit, with an actual route into each bay.
        clear(x-1,2,24,x+1,5,25)
    for x in range(32,65,5):
        for z in [28,35]:put(x,1,z,'sea_lantern')
    # Broad sunken stair starts in the courtyard and ends at the vault hall.
    clear(43,2,35,52,12,48)
    for step in range(8):
        for x in range(44,52):
            if step<6:box(x,2,48-step,x,7-step,48-step,'deepslate_bricks')
            put(x,8-step,48-step,'deepslate_brick_stairs',facing='south',half='bottom',shape='straight',waterlogged=False)
    for x in [42,53]:
        box(x,8,39,x,8,48,'polished_deepslate');railing(x,9,39,x,48)
    clear(44,2,35,51,5,40)
    # Two purposeful service wings, with manual press stations and categorized stock.
    room(14,36,28,64,15)
    clear(26,9,52,28,12,57)
    room(67,36,81,64,15)
    clear(67,9,51,69,12,57)
    for x,z in [(20,43),(20,57)]:
        put(x,9,z,'create:depot')
        put(x,11,z,'create:mechanical_press',axis='x')
        put(x+1,11,z,'create:shaft',axis='x')
        put(x+2,11,z,'create:hand_crank',facing='east',waterlogged=False)
        for xx in [x-1,x+3]:box(xx,9,z+1,xx,12,z+1,'create:industrial_iron_block')
        box(x-1,12,z+1,x+3,12,z+1,'create:industrial_iron_block')
    for z in [39,45,59]:
        stock(16,9,z,[('create:andesite_alloy',16),('create:shaft',12),('create:cogwheel',8),('iron_ingot',16),('create:iron_sheet',12)])
        stock(78,9,z,[('createnuclear:lead_ingot',16),('create:copper_sheet',12),('create:sturdy_sheet',8),('redstone',12),('glass',16)])
    for x,z in [(73,42),(73,59)]:
        box(x,9,z,x+2,9,z+2,'spruce_planks')
        box(x,10,z,x+2,10,z+2,'createnuclear:lead_block')
    # Barracks and supplies on either side of the main gate avenue.
    room(16,68,34,81,15,'createdeco:dean_bricks');clear(32,9,73,34,12,76)
    for x in [19,23,27,31]:
        put(x,9,70,'blue_bed',facing='north',part='head',occupied=False)
        put(x,9,71,'blue_bed',facing='north',part='foot',occupied=False)
        stock(x,9,79,[('bread',12),('baked_potato',12),('string',8),('book',3),('coal',12)])
    room(61,68,80,81,15,'createdeco:dean_bricks');clear(61,9,73,63,12,76)
    for x in [66,68,70]:put(x,9,70,'smoker',facing='south',lit=False)
    for x in [65,69,73,77]:stock(x,9,79,[('bread',16),('cooked_beef',12),('carrot',16),('bowl',12),('glass_bottle',8)])
    # Interior command desks use full-height bases; every cabinet contains supplies.
    for x in [40,44,48,52,56]:
        put(x,18,20,'spruce_planks');put(x,19,20,'radiologistics:main_computer',facing='up')
    stock(38,18,27,[('paper',24),('book',6),('compass',1),('clock',1),('spyglass',1),('create:precision_mechanism',3)])
    for x in [34,39,56,61]:stock(x,9,15,[('iron_ingot',16),('create:electron_tube',8),('create:brass_sheet',12),('create:precision_mechanism',2)])
    # Copy the tested bronze cannon assembly, including its real mount/crank and
    # chamber metadata, instead of inventing an unverified decorative mechanism.
    source=ROOT/'src/main/resources/data/ccq_testground/structure/engineering/engineering_base.nbt'
    original=n.load(source);original_palette=original['palette'];guns=[]
    for cx in [13,82]:
        count=0
        for b in original['blocks']:
            x,y,z=map(int,b['pos']);s=original_palette[int(b['state'])];nm=str(s['Name'])
            if not(68<=x<=73 and 2<=y<=5 and 71<=z<=84):continue
            if nm=='minecraft:air':continue
            dest=(x+cx-70,y+16,z+6)
            put(*dest,nm,**{str(k):str(v)for k,v in s.get('Properties',{}).items()})
            if'nbt'in b:blocks[dest]['nbt']=deepcopy(b['nbt'])
            count+=1
        guns.append({'bastion_center':[cx,82],'copied_verified_blocks':count})
    # Roof vents, supported parapet cap details and flush yard lights.
    for x,z in [(18,48),(72,48),(24,74),(70,74),(40,24),(55,24)]:
        roof=24 if z==24 else 15
        put(x,roof+1,z,'create:industrial_iron_block')
        put(x,roof+2,z,'iron_trapdoor',facing='north',half='bottom',open=False,powered=False,waterlogged=False)
    for x in range(11,86,9):
        for z in range(11,86,9):
            if name((x,9,z))=='minecraft:air':put(x,8,z,'sea_lantern')
    # Normalize authored iron-bar connections, close slab support gaps and check all
    # cell coordinates. A native four-orientation bake follows on integration.
    for p,b in list(blocks.items()):
        nm=name(p)
        if nm=='minecraft:iron_bars':
            x,y,z=p;props={'waterlogged':False}
            for k,(dx,dz)in {'north':(0,-1),'south':(0,1),'west':(-1,0),'east':(1,0)}.items():
                neighbor=name((x+dx,y,z+dz))
                props[k]=neighbor!='minecraft:air'and not any(v in neighbor for v in ['stairs','lantern','glass','door'])
            put(x,y,z,nm,**props)
    assert len(charges)==4
    for charge in charges:
        x,y,z=charge['pos'];assert name((x,y-1,z))=='createnuclear:steel_block'
        assert all(not any(word in name((x+dx,y+dy,z+dz))for word in ['redstone_block','redstone_torch','lever','button','pressure_plate'])
                   for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)])
    data=n.File(n.Compound({'DataVersion':n.Int(3955),'size':n.List[n.Int](SIZE),
                  'palette':n.List[n.Compound](palette),'blocks':n.List[n.Compound]([blocks[p]for p in sorted(blocks)]),'entities':n.List[n.Compound]()}),gzipped=True)
    DEST.mkdir(parents=True,exist_ok=True);data.save(DEST/'nuclear_fortress.nbt')
    report={'size':SIZE,'ground_y':8,'template_entries':len(blocks),'charges':charges,'guns':guns,'stocked_containers':len(containers),
            'structure_id':'ccq_prototype:nuclear_fortress','prototype_only':True,'instance_files_changed':False}
    (DEST/'manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (DEST/'stock.json').write_text(json.dumps(containers,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False),flush=True)

if __name__=='__main__':main()
