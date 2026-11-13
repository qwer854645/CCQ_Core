"""Fill every remaining base plot and complete its perimeter (Python + nbtlib)."""
from pathlib import Path
from copy import deepcopy
import nbtlib as n
from engineering_template_cleanup import remove_names_and_signs
from split_engineering_base import main as split

ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/"src/main/resources/data/ccq_testground/structure/engineering"


def main():
    data=n.load(ROOT/"tools/engineering/base_before_yards_084.nbt")
    palette=data["palette"]
    blocks={tuple(map(int,b["pos"])):deepcopy(b) for b in data["blocks"]}
    original=deepcopy(blocks)
    # Remove the former reserve markers and the block-built entrance number panel.
    for pos in list(blocks):
        x,y,z=pos
        if (x in [8,40] and z in [208,240] and y in [1,2])or(45<=x<=57 and 9<=y<=14 and z in [4,5]):del blocks[pos]

    def put(x,y,z,name,**properties):
        assert 0<=x<120 and 0<=y<30 and 0<=z<300,(x,y,z)
        if ':'not in name:name='minecraft:'+name
        state=n.Compound({'Name':n.String(name)})
        if properties:state['Properties']=n.Compound({k:n.String(str(v).lower()) for k,v in properties.items()})
        if state not in palette:palette.append(state)
        blocks[x,y,z]=n.Compound({'pos':n.List[n.Int]([x,y,z]),'state':n.Int(palette.index(state))})

    def box(x,y,z,X,Y,Z,name,**props):
        for xx in range(x,X+1):
            for yy in range(y,Y+1):
                for zz in range(z,Z+1):put(xx,yy,zz,name,**props)

    def paved(x,z,X,Z,name='polished_andesite'):
        box(x,1,z,X,1,Z,name)
        for xx in [x,X]:box(xx,1,z,xx,1,Z,'stone_bricks')
        for zz in [z,Z]:box(x,1,zz,X,1,zz,'stone_bricks')

    def roof(x,z,X,Z,top=7):
        for xx in [x+1,X-1]:
            for zz in [z+1,Z-1]:box(xx,2,zz,xx,top-1,zz,'create:industrial_iron_block')
        box(x,top,z,X,top,Z,'oxidized_cut_copper_slab',type='bottom',waterlogged=False)
        for xx in [x,X]:box(xx,top,z,xx,top,Z,'create:industrial_iron_block')
        for zz in [z,Z]:box(x,top,zz,X,top,zz,'create:industrial_iron_block')
        for xx in [x+3,X-3]:put(xx,top-1,z+3,'lantern',hanging=True,waterlogged=False)

    def cabinet(x,y,z,facing='south'):
        put(x,y,z,'farmersdelight:spruce_cabinet',facing=facing,open=False)

    def supplies(x,y,z,table='barrels/engineering/assembly_pad',kind='barrel',facing='south'):
        if kind=='chest':put(x,y,z,'chest',facing=facing,type='single',waterlogged=False)
        else:put(x,y,z,'barrel',facing=facing,open=False)
        blocks[x,y,z]['nbt']=n.Compound({'id':n.String('minecraft:'+kind),'LootTable':n.String('ccq_testground:'+table)})

    def planter(x,z,X,Z):
        box(x,1,z,X,1,Z,'stone_bricks')
        for xx in range(x,X+1,2):
            for zz in range(z,Z+1,3):put(xx,2,zz,'potted_fern')

    def copy_module(low,high,destination):
        for (x,y,z),block in original.items():
            if all(a<=v<=b for a,v,b in zip(low,(x,y,z),high)):
                pos=tuple(v-a+d for v,a,d in zip((x,y,z),low,destination))
                item=deepcopy(block);item['pos']=n.List[n.Int](pos);blocks[pos]=item

    # East forecourt: furnished courtyard, three parking bays, ammunition preparation.
    paved(61,11,83,33,'stone_bricks');roof(61,11,83,33,7)
    for z in [18,26]:
        for x,part in [(67,1),(68,2),(69,3)]:put(x,2,z,'kaleidoscope_cookery:table_spruce',axis='x',position=part,has_carpet=False,waterlogged=False)
        for x in [67,69]:
            for zz,f in [(z-1,'south'),(z+1,'north')]:put(x,2,zz,'kaleidoscope_cookery:chair_spruce',facing=f,has_carpet=False,waterlogged=False)
        put(68,3,z,'kaleidoscope_cookery:teapot')
        put(69,3,z,'kaleidoscope_cookery:empty_cup',cup_count=2,facing='south')
    for z in range(17,29):cabinet(80,2,z,'west')
    put(80,2,20,'farmersdelight:stove',facing='west',lit=True)
    put(80,3,20,'farmersdelight:cooking_pot',facing='west',support='none')
    put(80,3,23,'farmersdelight:cutting_board',facing='west')
    put(80,3,26,'farmersrespite:coffee_cake',bites=0)
    supplies(80,2,28,'chests/engineering/pantry')
    planter(62,12,62,30);planter(73,13,73,30)
    paved(87,11,113,35);roof(87,11,113,35,9)
    for x in [90,99,108]:
        for xx in [x-3,x+3]:box(xx,1,15,xx,1,31,'white_concrete')
        box(x-2,2,14,x+2,2,14,'polished_blackstone_slab',type='bottom',waterlogged=False)
        for z in [19,26]:put(x,1,z,'yellow_concrete')
        cabinet(x,2,33,'north')
    box(55,1,37,113,1,42,'gray_concrete')
    paved(61,45,83,64);roof(61,45,83,64,7)
    for x in [65,72,79]:
        for z in [49,57]:
            box(x-1,2,z,x+1,2,z+2,'spruce_planks')
            for zz in [z,z+2]:put(x,3,zz,'createbigcannons:solid_shot',facing='north')
            put(x-1,3,z+1,'barrel',facing='south',open=False)
    supplies(64,2,61,'chests/engineering/calibration_range','chest')
    supplies(67,2,61,'barrels/engineering/calibration_range')
    paved(87,45,113,64);roof(87,45,113,64,10)
    # A level, complete fabrication bay: pressing, mixing, assembly and tool storage.
    copy_module((13,4,98),(15,7,100),(97,2,49))
    for x in [96,100]:box(x,2,51,x,6,51,'create:industrial_iron_block')
    box(96,6,51,100,6,51,'create:industrial_iron_block')
    put(107,2,51,'create:basin')
    put(107,4,51,'create:mechanical_mixer')
    put(108,4,51,'create:cogwheel',axis='y')
    put(108,3,51,'create:gearbox',axis='z')
    put(109,3,51,'create:shaft',axis='x')
    put(110,3,51,'create:hand_crank',facing='east',waterlogged=False)
    put(108,2,51,'polished_andesite')
    for z in range(48,61):cabinet(90,2,z,'east')
    put(90,2,54,'crafting_table')
    for z in [49,52,57]:put(90,3,z,'create:depot')
    for x in range(96,110):cabinet(x,2,60,'north')
    for z in range(48,61,3):cabinet(111,2,z,'west')
    put(111,3,54,'create:brown_toolbox')
    # A genuine rail loading apron links the old workshop to the new assembly line.
    paved(5,124,43,130)
    for z in [126,129]:
        for x in range(9,39):put(x,2,z,'rail',shape='east_west',waterlogged=False)
        for x in [8,39]:put(x,2,z,'create:industrial_iron_block')
    supplies(6,2,125)

    # The former reserved slab now contains the existing, verified synchronized lift.
    paved(5,132,45,182)
    copy_module((15,0,83),(34,17,107),(16,0,141))
    for x in [7,39]:
        for z in [137,149,169,177]:
            box(x,2,z,x+4,2,z+3,'spruce_planks')
            box(x+1,3,z+1,x+3,4,z+2,'iron_block'if z<160 else'copper_block')
    for z in [136,173]:
        box(18,1,z,34,1,z,'yellow_concrete')
    # Bulk materials: six large sorted bays with actual usable construction blocks.
    paved(5,193,43,220);roof(5,193,43,220,8)
    for x,material in [(8,'andesite'),(20,'stone_bricks'),(32,'spruce_log')]:
        for z in [197,210]:
            box(x,2,z,x+7,2,z+5,'spruce_planks')
            if material=='spruce_log':box(x+1,3,z+1,x+6,4,z+4,material,axis='x')
            else:box(x+1,3,z+1,x+6,4,z+4,material)
    supplies(7,2,217,'chests/engineering/assembly_pad','chest')

    # Utility station: a contained source-water reservoir and a hand-driven pump.
    paved(5,229,43,252)
    box(8,2,231,18,4,241,'stone_bricks')
    box(9,2,232,17,4,240,'water',level=0)
    for x in [8,18]:box(x,5,231,x,5,241,'iron_bars',east=False,west=False,north=True,south=True,waterlogged=False)
    for z in [231,241]:box(9,5,z,17,5,z,'iron_bars',east=True,west=True,north=False,south=False,waterlogged=False)
    for x in [8,18]:
        for z in [231,241]:put(x,5,z,'iron_bars',east=x==8,west=x==18,north=z==241,south=z==231,waterlogged=False)
    put(18,3,236,'create:fluid_pipe',east=True,west=True,north=False,south=False,up=False,down=False,waterlogged=False)
    put(20,3,236,'create:fluid_pipe',west=True,down=True,east=False,north=False,south=False,up=False,waterlogged=False)
    put(20,2,236,'create:fluid_pipe',up=True,east=True,west=False,north=False,south=False,down=False,waterlogged=False)
    put(19,3,236,'create:mechanical_pump',facing='east',waterlogged=False)
    put(19,4,236,'create:cogwheel',axis='x')
    put(20,4,236,'create:shaft',axis='x')
    put(21,4,236,'create:hand_crank',facing='east',waterlogged=False)
    box(19,2,236,19,2,238,'polished_andesite')
    for x in [21,26]:box(x,2,234,x,2,239,'stone_bricks')
    for z in [234,239]:box(21,2,z,26,2,z,'stone_bricks')
    put(21,2,236,'create:fluid_pipe',east=True,west=True,north=False,south=False,up=False,down=False,waterlogged=False)
    put(21,4,236,'create:hand_crank',facing='east',waterlogged=False)
    roof(29,231,41,249,7)
    for z in range(233,248,3):cabinet(39,2,z,'west')
    supplies(39,2,246)
    for x in [30,34]:
        box(x,2,234,x+2,2,237,'iron_block')
        box(x,2,243,x+2,2,246,'create:copper_casing')
    # Two rear buildings: shared supplies and range observation/control.
    for x,X in [(6,23),(27,43)]:
        paved(x,263,X,284);roof(x,263,X,284,8)
        for xx in [x,X]:box(xx,2,264,xx,7,283,'stone_bricks')
        box(x+1,2,283,X-1,7,283,'stone_bricks')
        for z in [267,275]:box(x,3,z,x,5,z+3,'glass')
    for x in range(9,21):cabinet(x,2,281,'north')
    for x in [10,17]:box(x,2,268,x+2,3,272,'spruce_planks')
    for z in [268,274]:
        for x,part in [(31,1),(32,2),(33,3)]:put(x,2,z,'kaleidoscope_cookery:table_spruce',axis='x',position=part,has_carpet=False,waterlogged=False)
        for x in [31,33]:put(x,2,z+1,'kaleidoscope_cookery:chair_spruce',facing='north',has_carpet=False,waterlogged=False)
    for z in range(267,280):cabinet(41,2,z,'west')
    put(41,3,269,'radiologistics:main_computer',facing='up')
    put(41,3,271,'supplementaries:book_pile_horizontal')
    supplies(41,2,278,'chests/engineering/survey_tower','chest')
    # Pedestrian and freight connections turn the gaps into deliberate circulation.
    for z,Z in [(185,190),(223,227),(255,260),(287,292)]:box(5,1,z,55,1,Z,'gray_concrete')
    box(5,1,131,45,1,131,'gravel')
    box(5,1,60,43,1,61,'gravel')
    box(5,1,75,43,1,76,'gravel')
    box(5,1,114,23,1,122,'gravel')
    box(5,1,77,8,1,112,'gravel')
    box(41,1,77,44,1,112,'gravel')
    for z in [81,87,93,99,105,111]:
        box(5,2,z,7,2,z+1,'spruce_planks')
        put(6,3,z,'barrel',facing='east',open=False)
    for z in [80,88,96,104]:
        box(42,2,z,44,2,z+3,'spruce_planks')
        box(42,3,z+1,44,3,z+2,'copper_block')
    planter(5,12,6,33);planter(8,7,39,8)
    # Side berms and inspection walks, kept outside all three firing lines.
    for x in [61,112]:
        for z in range(99,278):
            # Preserve original target supports and their immediate approach.
            if any(abs(z-target)<=6 for target in [148,208,268]):continue
            box(x,1,z,x+1,2,z,'stone_bricks')
    box(114,1,69,115,1,288,'gravel')
    for z in range(100,280,16):put(114,2,z,'lantern',hanging=False,waterlogged=False)
    # A stepped, destructible backstop occupies the run-out beyond all original targets.
    for y,z in [(2,281),(4,283),(6,285),(8,287)]:box(63,y,z,113,y+1,289,'stone_bricks')
    for x in [70,85,100]:box(x-2,4,283,x+2,5,283,'iron_block')
    # Finish the narrow spaces between plots as level circulation/drainage strips.
    for x0,z0,X,Z in [(5,123,45,292),(58,8,115,67)]:
        for x in range(x0,X+1):
            for z in range(z0,Z+1):
                if (x,1,z)not in blocks:put(x,1,z,'gravel')
    box(55,1,21,61,1,25,'polished_andesite')

    # Complete continuous walls. The only openings are the front and rear gateways.
    for x in [3,116]:
        box(x,1,5,x,2,294,'stone_bricks')
        box(x,3,5,x,4,294,'iron_bars',east=False,west=False,north=True,south=True,waterlogged=False)
        for z in [5,*range(21,294,16),294]:box(x,1,z,x,5,z,'stone_bricks')
    for z in [5,294]:
        for x in range(4,116):
            if (z==5 and 43<=x<=59)or(z==294 and 46<=x<=56):continue
            box(x,1,z,x,2,z,'stone_bricks')
            box(x,3,z,x,4,z,'iron_bars',east=True,west=True,north=False,south=False,waterlogged=False)
            if x%16==3:box(x,1,z,x,5,z,'stone_bricks')
    box(47,1,291,55,1,299,'gray_concrete')
    for x in [46,56]:box(x,1,294,x,8,294,'create:industrial_iron_block')
    box(46,8,294,56,8,294,'create:industrial_iron_block')
    # One continuous grade inside the boundary; keep real machines embedded in it.
    for x in range(4,116):
        for z in range(6,294):
            pos=(x,1,z)
            if pos not in blocks or str(palette[int(blocks[pos]['state'])]['Name'])=='minecraft:gravel':
                put(x,1,z,'polished_andesite')
    box(47,1,0,55,1,4,'gray_concrete')
    for x in range(47,56):
        put(x,1,0,'stone_brick_stairs',facing='south',half='bottom',shape='straight',waterlogged=False)
        put(x,1,299,'stone_brick_stairs',facing='north',half='bottom',shape='straight',waterlogged=False)
    # Recessed floor lighting covers every canopy without lowering vehicle clearance.
    for x0,z0,X,Z in [(6,43,34,59),(61,11,83,33),(87,11,113,35),(61,45,83,64),
                       (87,45,113,64),(5,193,43,220),(29,231,41,249),(6,263,23,284),(27,263,43,284)]:
        for x in range(x0+2,X,5):
            for z in range(z0+2,Z,5):
                if (x,2,z)not in blocks:put(x,1,z,'sea_lantern')
    remove_names_and_signs(blocks,palette)
    # All original machinery, supplies and lodge blocks must remain untouched.
    for pos,block in original.items():
        x,y,z=pos;name=str(palette[int(block['state'])]['Name'])
        if (y>=2 and name.startswith(('create:','createbigcannons:')))or('nbt'in block and any(k in block['nbt']for k in ['Items','LootTable'])):
            assert blocks.get(pos)==block,('Existing machine or supplies overwritten',pos,name)
        if 29<=x<=46 and 14<=z<=32:assert blocks.get(pos)==block,('Lodge changed',pos)
    data['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)])
    data.save(FOLDER/'engineering_base.nbt')
    split()
    print('Expanded base:',len(blocks),'blocks; seven new supply containers; front/rear gates retained')


if __name__=='__main__':main()
