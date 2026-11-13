"""Four protected domestic/plant corner rooms below; working and fighting deck above."""
from nuclear_bunker_stock import contents
import json
STAIRS=[{'x':[25,28],'z':[53,58],'bottom_floor':8,'upper_floor':14},{'x':[67,70],'z':[53,58],'bottom_floor':8,'upper_floor':14}]
POWER={'wheels':[[x,10,19]for x in range(69,81)],'wheel_type':'large_water_wheel',
       'presses':[[20,17,z]for z in [35,38,41]],'fans':[[21,16,z]for z in [46,49,52]],
       'fan_media':[[22,16,z]for z in [46,49,52]],'fan_depots':[[23,15,z]for z in [46,49,52]],
       'millstones':[[20,16,z]for z in [57,60,63]],'saws':[[x,15,20]for x in [35,39,43]],
       'fan_processes':['smoking','splashing','blasting'],
       'master_controller':[64,11,19],
       'reservoir':[68,7,16,81,13,22],'riser':[64,10,19,64,22,19],'shaft_height':22}

def build(put,box,clear,stock,window_supply,name):
    ledger=[];assortments=set();walls=[]
    def supply(x,y,z,profile,facing='south',cabinet=False,count=8):
        for attempt in range(100):
            items=contents(profile,(x,y,z,attempt),count);signature=json.dumps(sorted(items,key=lambda item:item[0]),sort_keys=True)
            if signature not in assortments:break
        else:raise AssertionError(('Duplicate stock',profile,x,y,z))
        assortments.add(signature);stock(x,y,z,items,facing=facing,cabinet=cabinet);ledger.append({'pos':[x,y,z],'profile':profile,'items':items})
    def cabinet(x,y,z,profile,facing='south',count=8):supply(x,y,z,profile,facing,True,count)
    def table(x,y,z,length=1):
        for xx in range(x,x+length):put(xx,y,z,'kaleidoscope_cookery:table_spruce',axis='x',position=0,has_carpet=False,waterlogged=False)
    def chair(x,y,z,facing):put(x,y,z,'kaleidoscope_cookery:chair_spruce',facing=facing,has_carpet=False,waterlogged=False)
    def floor(x,z,X,Z,material,level=14):
        for xx in range(x,X+1):
            for zz in range(z,Z+1):
                if name((xx,level,zz))=='minecraft:polished_andesite':put(xx,level,zz,material)
    def lamp(x,z,upper=False):
        y,ceiling=(20,22)if upper else(12,13)
        while upper and any(name((x,yy,z)) in ['create:shaft','create:gearbox']for yy in [y,ceiling-1]):z+=1
        put(x,y,z,'createdeco:yellow_industrial_iron_lamp',facing='down',inverted=True,lit=True,waterlogged=False)
        for yy in range(y+1,ceiling):put(x,yy,z,'chain',axis='y',waterlogged=False)
    def desk(x,y,z,length=4):
        table(x,y,z,length);put(x+1,y+1,z,'radiologistics:main_computer',facing='up');chair(x+1,y,z+2,'north');put(x+3,y+1,z,'supplementaries:book_pile_horizontal')
    def rack(x,X,z,profiles,facing='south'):
        for xx in [x-1,X+1]:box(xx,15,z,xx,18,z,'createdeco:industrial_iron_support',facing='up',waterlogged=False)
        box(x-1,18,z,X+1,18,z,'createdeco:industrial_iron_sheet_metal',axis='x')
        for i,xx in enumerate(range(x,X+1,2)):
            for y in [15,16]:supply(xx,y,z,profiles[i%len(profiles)],facing,count=8 if y==15 else 6)
    # Four real ground-floor corner rooms; walls join the shell and ceiling.
    def partition(x,z,X,Z,doors):
        walls.append({'bounds':[x,z,X,Z],'doors':[list(d)for d in doors]})
        for xx in range(x,X+1):
            for zz in range(z,Z+1):
                if name((xx,14,zz))!='minecraft:air':box(xx,9,zz,xx,12,zz,'gray_concrete')
        for a,b,c,d in doors:clear(a,9,b,c,11,d)
    for x,z,Z,dz in [(31,13,31,22),(64,13,31,24),(31,64,82,71),(64,64,82,71)]:
        partition(x,z,x,Z,[(x,dz,x,dz+2)])
    for z,x,X,dx in [(31,13,31,22),(31,64,82,69),(64,13,31,22),(64,64,82,71)]:
        partition(x,z,X,z,[(dx,z,dx+2,z)])
    # Furniture is backed by actual walls; both corner entrances stay open.
    floor(10,10,30,30,'spruce_planks',8)
    for z in [19,20,21,26,27,28]:
        put(30,9,z,'gray_bed',facing='east',part='head',occupied=False)
        put(29,9,z,'gray_bed',facing='east',part='foot',occupied=False)
    for x,profile in [(21,'personal'),(20,'protective')]:cabinet(x,9,30,profile,'north',8)
    for x in [25,26,27,28]:
        put(x,9,30,'gray_bed',facing='south',part='head',occupied=False)
        put(x,9,29,'gray_bed',facing='south',part='foot',occupied=False)
    for x,profile in [(22,'personal'),(23,'protective')]:cabinet(x,9,30,profile,'north')
    table(16,9,10,7)
    for x in [17,20,22]:chair(x,9,12,'north')
    put(17,10,10,'supplementaries:book_pile_horizontal');put(21,10,10,'kaleidoscope_cookery:teapot')
    for x,z in [(19,15),(26,18),(25,28)]:lamp(x,z)
    # Galley line hugs the north partition; dining counter hugs the west shell.
    floor(10,65,30,82,'createdeco:tiled_pearl_bricks',8)
    for x in [16,17]:put(x,9,65,'kaleidoscope_cookery:stove',facing='south',lit=False)
    put(16,10,65,'kaleidoscope_cookery:stockpot',facing='south',has_base=True,has_chains=False,has_lid=True,waterlogged=False)
    put(17,10,65,'kaleidoscope_cookery:pot',facing='south',has_base=True,has_oil=False,show_oil=False,waterlogged=False)
    for x,profile in [(21,'cookware'),(19,'pantry'),(20,'rations'),(22,'tea'),(23,'pantry'),(24,'cookware')]:cabinet(x,9,65,profile,'south')
    put(21,10,65,'kaleidoscope_cookery:chopping_board',facing='south',waterlogged=False)
    put(22,10,65,'kaleidoscope_cookery:teapot')
    for x in [21,20]:put(x,11,65,'kaleidoscope_cookery:kitchenware_racks',facing='south',waterlogged=False)
    for z in range(73,80):put(10,9,z,'kaleidoscope_cookery:table_spruce',axis='z',position=0,has_carpet=False,waterlogged=False)
    for z in [74,77]:chair(12,9,z,'west')
    put(10,10,74,'kaleidoscope_cookery:empty_cup',cup_count=3,facing='east')
    put(10,10,77,'kaleidoscope_cookery:bamboo_tray',stand=False)
    for z,profile in [(66,'pantry'),(67,'cookware'),(68,'rations'),(69,'pantry')]:cabinet(30,9,z,profile,'west')
    put(30,10,67,'kaleidoscope_cookery:enamel_basin',has_lid=False,oil_count=0,waterlogged=False)
    put(26,9,65,'water_cauldron',level=3)
    for x,z in [(18,68),(26,69),(17,78)]:lamp(x,z)
    # First aid: examination beds and dispensary banks follow the room walls.
    floor(65,65,85,85,'createdeco:tiled_pearl_bricks',8)
    for z in [67,69]:
        put(65,9,z,'white_bed',facing='west',part='head',occupied=False);put(66,9,z,'white_bed',facing='west',part='foot',occupied=False)
    for x in [74,77]:
        put(x,9,85,'white_bed',facing='south',part='head',occupied=False);put(x,9,84,'white_bed',facing='south',part='foot',occupied=False)
    for x,profile in [(66,'medical'),(67,'protective'),(68,'medical'),(69,'personal')]:cabinet(x,9,65,profile,'south')
    for z,profile in [(75,'medical'),(76,'personal'),(77,'protective')]:
        cabinet(85,9,z,profile,'west')
        put(85,10,z,'kaleidoscope_cookery:enamel_basin',has_lid=False,oil_count=0,waterlogged=False)
    for x,z in [(69,68),(80,74),(75,81)]:lamp(x,z)
    # North-east plant: contained waterwheel bank, with power rising to the work deck.
    floor(66,13,82,29,'createdeco:industrial_iron_sheet_metal',8)
    box(68,7,16,81,12,22,'deepslate_tiles');clear(69,8,17,80,12,21)
    box(69,8,22,80,12,22,'createnuclear:reinforced_glass')
    box(69,13,17,80,13,22,'createnuclear:reinforced_glass')
    for x in range(69,81):
        for y in range(8,11):put(x,y,17,'water',level=8)
        put(x,11,17,'water',level=0)
        for z in range(18,22):put(x,8,z,'water',level=8)
        put(x,10,19,'create:large_water_wheel',axis='x',extension=x>69)
        # Create authors its own structural neighbours on the scheduled wheel tick.
    # Single-block controller uses CCQ Core's fixed output propagation.
    for x in [67,68]:put(x,10,19,'create:shaft',axis='x')
    put(66,10,19,'create:shaft',axis='x')
    put(65,10,19,'create:shaft',axis='x');put(64,10,19,'create:gearbox',axis='z')
    # Main distribution sits immediately below the ceiling. Only local drops
    # descend beside machines; no transverse shaft crosses at head height.
    for y in range(11,22):put(64,y,19,'create:shaft',axis='y')
    put(64,11,19,'dndesires:omni_speed_controller',axis='y')
    # North and west wall ceiling mains feed rows of three machines per process.
    put(64,22,19,'create:gearbox',axis='z')
    for x in range(19,64):put(x,22,19,'create:shaft',axis='x')
    put(19,22,19,'create:gearbox',axis='y')
    drops={z:17 for z in [35,38,41]}
    drops.update({z:16 for z in [46,49,52]});drops.update({z:15 for z in [57,60,63]})
    for z in range(20,64):
        put(19,22,z,'create:gearbox',axis='x')if z in drops else put(19,22,z,'create:shaft',axis='z')
    for z,bottom in drops.items():
        for y in range(bottom+1,22):put(19,y,z,'create:shaft',axis='y')
        put(19,bottom,z,'create:gearbox',axis='z')
    for x,y,z in POWER['presses']:
        put(x,y,z,'create:mechanical_press',facing='east');put(x,15,z,'create:depot')
        box(x,18,z,x,21,z,'createdeco:industrial_iron_support',facing='up',waterlogged=False)
    for (x,y,z),process in zip(POWER['fans'],POWER['fan_processes']):
        put(20,y,z,'create:gearshift',axis='x',powered=z!=49)
        put(20,y,z+1,'lever',face='wall',facing='south',powered=z!=49)
        put(x,y,z,'create:encased_fan',facing='east')
        put(22,15,z,'create:industrial_iron_block');put(22,16,z,'create_connected:fan_'+process+'_catalyst')
        put(23,15,z,'create:depot')
        for yy in [15,16]:put(24,yy,z,'iron_trapdoor',facing='west',half='bottom',open=True,powered=False,waterlogged=False)
    for x,y,z in POWER['millstones']:
        put(x,15,z,'create:gearbox',axis='z');put(x,y,z,'create:millstone')
    for x,y,z in POWER['saws']:
        put(x,22,19,'create:gearbox',axis='z')
        for yy in range(16,22):put(x,yy,19,'create:shaft',axis='y')
        put(x,15,19,'create:gearbox',axis='x')
        put(x,y,z,'create:mechanical_saw',facing='up',axis_along_first=False,flipped=False)
    # Shared hall stays open. Supplies sit along the outer wall and entry sides.
    for x,profile in [(34,'personal'),(35,'rations'),(36,'tea'),(61,'personal'),(60,'rations'),(59,'tea')]:cabinet(x,9,19,profile,'south')
    for x in [36,59]:put(x,10,19,'kaleidoscope_cookery:teapot')
    for z in [39,48,60]:
        window_supply(19,9,z,'east');window_supply(76,9,z,'west')
    for x,z in [(35,24),(60,24),(23,42),(72,42),(35,71),(60,71)]:lamp(x,z)
    # Compact processing island; static forge and fitting stations share its edge.
    floor(19,33,24,64,'createdeco:industrial_iron_sheet_metal')
    for x in [34,38]:
        put(x,15,75,'blast_furnace',facing='north',lit=False);box(x,16,75,x,21,75,'createdeco:industrial_iron_sheet_metal',axis='y')
    put(42,15,19,'smithing_table');put(45,15,19,'anvil',facing='south')
    for x,profile in [(46,'metals'),(47,'tools'),(48,'transmission'),(49,'electrical'),(54,'signals'),(55,'tools'),(56,'documents')]:cabinet(x,15,19,profile,'south')
    # Former free-standing electrical desk removed.
    # Warehouse inventory follows the eastern outer wall in three labeled-by-use
    # banks, leaving the central floor and both stair approaches clear.
    floor(73,32,75,65,'createdeco:industrial_iron_sheet_metal')
    for start,profiles in [(34,['metals','transmission','plumbing','electrical']),(45,['ordnance','gun_tools','protective']),(56,['protective','rations','medical','personal'])]:
        for i,z in enumerate(range(start,start+4)):
            for y in [15,16]:supply(76,y,z,profiles[i%len(profiles)],'west',count=8 if y==15 else 6)
        box(76,18,start,75,18,start+3,'createdeco:industrial_iron_sheet_metal',axis='z')
        for z in [start,start+3]:box(77,15,z,77,17,z,'createdeco:industrial_iron_support',facing='up',waterlogged=False)
    for x,profile in [(54,'ordnance'),(55,'gun_tools'),(56,'protective')]:cabinet(x,15,75,profile,'north')
    for x,z in [(36,27),(44,26),(36,37),(44,45),(56,26),(62,28),(57,38),(67,38),(62,47),(62,56),(72,57),(29,51),(29,59),(37,66),(58,72),(48,62)]:lamp(x,z,True)
    # Two-level stock bank against the previously empty wall beside the furnaces.
    for x in [40,47]:box(x,15,75,x,17,75,'createdeco:industrial_iron_support',facing='up',waterlogged=False)
    box(40,18,75,47,18,75,'createdeco:industrial_iron_sheet_metal',axis='x')
    for x,profile in [(41,'metals'),(42,'electrical'),(43,'tools'),(44,'transmission'),(45,'plumbing'),(46,'protective')]:
        for y in [15,16]:supply(x,y,75,profile,'north',count=8 if y==15 else 6)
    # Recessed gun stores are on the rear walls, away from access doors and gun sweep.
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        sx=-1 if cx==19 else 1;sz=-1 if cz==19 else 1
        for u,profile in [(-1,'ordnance'),(0,'gun_tools')]:supply(cx+sx*u,15,cz-sz*5,profile,'north'if sz==-1 else'south')
    for x,z,facing in [(50,19,'south'),(60,19,'south'),(35,76,'north'),(60,76,'north')]:window_supply(x,15,z,facing)
    # Two shared stair flights remove the need for individual ladders.
    for spec in STAIRS:
        x,X=spec['x'];clear(x,9,53,X,21,60)
        for step in range(6):
            z=58-step;y=9+step
            if y>9:box(x,9,z,X,y-1,z,'gray_concrete')
            box(x,y,z,X,y,z,'stone_brick_stairs',facing='north',half='bottom',shape='straight',waterlogged=False)
        for xx in [x-1,X+1]:box(xx,15,54,xx,15,60,'iron_bars',north=False,south=False,east=False,west=False,waterlogged=False)
        box(x,15,60,X,15,60,'iron_bars',north=False,south=False,east=False,west=False,waterlogged=False);lamp(x+1,51,True)
    # Annular command post centered on the bunker, with two opposing entrances.
    for x in range(40,56):
        for z in range(40,56):
            dx,dz=abs(x-47.5),abs(z-47.5)
            if dx+dz>12:continue
            put(x,14,z,'createdeco:tiled_blue_bricks')
            edge=max(dx,dz)>=6.5 or dx+dz>=11
            entrance=46<=x<=49 and z in [40,41,54,55]
            if edge and not entrance:put(x,15,z,'create:andesite_casing')
    for x,z,facing in [(43,40,'south'),(52,55,'north'),(40,48,'east'),(55,47,'west')]:
        put(x,15,z,'create:andesite_casing');put(x,16,z,'radiologistics:main_computer',facing='up')
    for x,z,facing in [(44,40,'south'),(51,55,'north'),(40,49,'east'),(55,46,'west')]:
        put(x,15,z,'create:andesite_casing');put(x,16,z,'create:analog_lever',face='floor',facing=facing)
    box(46,15,46,49,15,49,'polished_andesite')
    put(47,16,47,'supplementaries:globe',facing='south')
    put(49,16,49,'supplementaries:book_pile_horizontal')
    for x,z,facing in [(43,45,'east'),(43,50,'east'),(52,45,'west'),(52,50,'west')]:chair(x,15,z,facing)
    # Mirrored ground-floor dining groups about x=47.5. The former isolated
    # northern table and offset reading suite are removed rather than duplicated.
    for z in [36,42]:
        for x in [35,54]:table(x,9,z,7)
        for x in [36,39,41]:
            for xx in [x,95-x]:chair(xx,9,z-1,'south');chair(xx,9,z+1,'north')
        for x in [38,57]:put(x,10,z,'kaleidoscope_cookery:empty_cup',cup_count=3,facing='south')
    floor(33,33,42,45,'spruce_planks',8);floor(53,33,62,45,'spruce_planks',8)
    for x in [35,56]:table(x,9,69,5)
    for x in [36,38]:
        for xx in [x,95-x]:chair(xx,9,71,'north')
    for x,z in [(38,39),(57,39),(38,70),(57,70)]:lamp(x,z)

    # Barracks: wall library, shared writing/packing bench and personal wash kit.
    box(10,9,17,10,10,21,'bookshelf')
    table(16,9,23,6)
    for x in [17,20]:chair(x,9,25,'north')
    put(17,10,23,'supplementaries:book_pile_horizontal')
    put(20,10,23,'kaleidoscope_cookery:empty_cup',cup_count=2,facing='south')
    cabinet(19,9,30,'personal','north')
    put(19,10,30,'kaleidoscope_cookery:enamel_basin',has_lid=False,oil_count=0,waterlogged=False)
    # Mess: two complete dining groups, food displays and an orderly dry-store end.
    for z in [73,79]:
        table(16,9,z,7)
        for x in [17,20,22]:chair(x,9,z-1,'south');chair(x,9,z+1,'north')
        put(19,10,z,'kaleidoscope_cookery:bamboo_tray',stand=False)
        put(21,10,z,'kaleidoscope_cookery:empty_cup',cup_count=3,facing='south')
    put(19,10,65,'kaleidoscope_cookery:fruit_basket',facing='south',waterlogged=False)
    put(29,9,67,'farmersdelight:rice_bag');put(29,9,69,'farmersdelight:potato_crate')
    put(28,9,78,'kaleidoscope_cookery:trash_can')
    # Aid room: a central clean treatment/sterilization counter and stools.
    table(72,9,72,8)
    for x in [73,77]:
        put(x,10,72,'kaleidoscope_cookery:enamel_basin',has_lid=False,oil_count=0,waterlogged=False)
        put(x,9,74,'kaleidoscope_tavern:gray_bar_stool',facing='north',waterlogged=False)
    put(79,10,72,'supplementaries:book_pile_horizontal');put(80,9,72,'water_cauldron',level=3)
    # Reset plant room: dry inspection aisle and compact wall-side spares.
    for x,profile in [(74,'plumbing'),(75,'transmission'),(76,'tools'),(77,'metals'),(78,'protective')]:cabinet(x,9,30,profile,'north')
    for x,z in [(70,25),(77,25)]:lamp(x,z)
    # Three manual copper door leaves and a complete lintel at every corner entrance.
    for spec in walls:
        for a,b,c,d in spec['doors']:
            vertical=a==c
            facing=('east'if a<48 else'west')if vertical else('south'if b<48 else'north')
            for i in range(3):
                x,z=(a,b+i)if vertical else(a+i,b)
                put(x,11,z,'polished_deepslate')
                for y,half in [(9,'lower'),(10,'upper')]:
                    put(x,y,z,'waxed_copper_door',facing=facing,half=half,hinge='left'if i==0 else'right',open=False,powered=False)
    return ledger,walls
