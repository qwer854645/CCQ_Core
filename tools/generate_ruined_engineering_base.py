"""Author the registered accident-damaged base templates from the current base.

No instance writes, worldgen changes or edits to the intact template. Requires nbtlib.
"""
from pathlib import Path
from copy import deepcopy
import collections, hashlib, json, math
import nbtlib as n
from engineering_template_cleanup import fix_support_geometry
from ruined_base_mobs import populate, partition_entities

ROOT = Path(__file__).resolve().parents[1]
FOLDER = ROOT/'src/main/resources/data/ccq_testground/structure/engineering'
DESIGN = ROOT/'tools/engineering/ruined_base_v5'
SOURCE = FOLDER/'engineering_base.nbt'

def main():
    before = hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    template = n.load(SOURCE)
    palette = template['palette']
    original = {tuple(map(int,b['pos'])):deepcopy(b) for b in template['blocks']}
    blocks = deepcopy(original)
    limits=[120,30,300]
    def signature(state):
        return str(state['Name']),tuple(sorted((str(k),str(v))for k,v in state.get('Properties',{}).items()))
    indices = {signature(s):i for i,s in enumerate(palette)}
    def put(p, name, **props):
        if ':' not in name:name='minecraft:'+name
        assert all(0<=v<limit for v,limit in zip(p,limits)),p
        state=n.Compound({'Name':n.String(name)})
        if props:state['Properties']=n.Compound({k:n.String(str(v).lower())for k,v in props.items()})
        key=signature(state)
        if key not in indices:indices[key]=len(palette);palette.append(state)
        blocks[p]=n.Compound({'pos':n.List[n.Int](p),'state':n.Int(indices[key])})
    def name(p):
        return str(palette[int(blocks[p]['state'])]['Name'])if p in blocks else 'minecraft:air'
    def props(p):
        return {str(k):str(v)for k,v in palette[int(blocks[p]['state'])].get('Properties',{}).items()}
    def erase(p):put(p,'air')
    def noise(x,z,salt=0):return ((x*73856093)^(z*19349663)^(salt*83492791))%1000/1000
    def ellipse(x,z,cx,cz,rx,rz):return ((x-cx)/rx)**2+((z-cz)/rz)**2
    def coherent(x,z,scale,salt):
        xx,zz=x/scale,z/scale;ix,iz=math.floor(xx),math.floor(zz)
        a,b=xx-ix,zz-iz;a=a*a*(3-2*a);b=b*b*(3-2*b)
        return 2*((noise(ix,iz,salt)*(1-a)+noise(ix+1,iz,salt)*a)*(1-b)
                  +(noise(ix,iz+1,salt)*(1-a)+noise(ix+1,iz+1,salt)*a)*b)-1
    def blast_distance(x,z,index,blast):
        _,cx,cz,rx,rz=blast;salt=73+index*19
        # Warped coordinates change the whole rim silhouette; a handful of
        # angular sine lobes alone still produced conspicuous nested ellipses.
        wx=x+5.5*(coherent(x,z,18,salt)-coherent(cx,cz,18,salt))+2*coherent(x,z,7,salt+2)
        wz=z+5.0*(coherent(x,z,17,salt+1)-coherent(cx,cz,17,salt+1))+2*coherent(x,z,6,salt+3)
        theta=math.atan2((wz-cz)/rz,(wx-cx)/rx)
        edge=max(.5,1+.20*math.sin(theta*3+index)+.10*math.sin(theta*7-index)+.22*coherent(x,z,11,salt+5))
        return ellipse(wx,wz,cx,cz,rx,rz)/edge
    # Roof damage is grouped into large, intelligible holes with jagged edges.
    roof_zones=[(99,57,12,12,6),(74,52,10,9,6),
                (13,48,9,8,6),(14,207,12,11,7),
                (14,275,8,8,5),(43,29,5,5,11)]
    for p in list(blocks):
        x,y,z=p
        for cx,cz,rx,rz,lowest in roof_zones:
            if y>=lowest and ellipse(x,z,cx,cz,rx,rz)<.84+.30*noise(x,z,17):
                erase(p);break
    # The central cannon failed at the breech; a neighboring steel gun survives.
    for p in list(blocks):
        x,y,z=p
        distance=ellipse(x,z,85,77,10,14)
        if y>=2 and distance<1+.14*noise(x,z,4):erase(p)
        elif y==1 and distance<.76+.15*noise(x,z,3):erase(p)
        elif y==0 and distance<.85:
            put(p,'polished_basalt'if noise(x,z)>.72 else'basalt',axis='y')
        elif y==1 and distance<1.8 and noise(x//2,z//2,8)>.36:
            put(p,'cobbled_deepslate'if distance<1.2 else'gravel')
    # A clear blowout through the range's service building and its outer wall.
    for p in list(blocks):
        x,y,z=p
        if 89<=x<=111 and 61<=z<=70 and y>=3 and ellipse(x,z,99,66,12,7)<1.12:
            erase(p)
        # Breached perimeter: a bounded opening, never a missing rectangular chunk.
        if x>=115 and 67<=z<=87 and y>=2:
            crest=2+int(abs(z-77)/3)
            if y>=crest:erase(p)
    # Smaller secondary incidents: a warehouse wall and one material-bay corner.
    for p in list(blocks):
        x,y,z=p
        if y>=2 and (ellipse(x,z,8,48,4,6)<1 or ellipse(x,z,8,204,5,7)<1):
            erase(p)
        # Exit damage and a two-block-wide pedestrian way remain distinct.
        if z<=4 and 64<=x<=73 and y>=2:erase(p)
    # V2: stored ammunition propagated the accident across multiple independent
    # buildings. Overlapping broad scars distinguish this from scattered holes.
    detonations=[
        ('primary_range',85,80,27,34),
        ('ready_ammunition',73,53,17,19),
        ('warehouse_ammunition',26,53,23,19),
        ('maintenance_stocks',25,96,22,25),
        ('loading_apron',29,154,27,28),
        ('bulk_stores',24,207,21,20),
        ('range_spill',97,173,18,18),
        ('rear_supply_cache',23,275,16,16),
    ]
    for p in list(blocks):
        x,y,z=p
        closest=100
        for i,blast in enumerate(detonations):
            closest=min(closest,blast_distance(x,z,i,blast))
        if closest>1.75:continue
        if y>=2:
            # Core completely demolished; irregular low wall stubs and stripped
            # outer frames survive around the edge of each detonation.
            crest=2+int(max(0,closest-.67)*8)+int(noise(x//2,z//2,53)*2)
            if closest<.67 or closest<1.42 and y>=crest:erase(p)
        elif y==1:
            if closest<.69:erase(p)
            elif closest<1.30:
                put(p,'cobbled_deepslate'if closest<.91 else'gravel'if noise(x//3,z//3,6)>.34 else'andesite')
        elif y==0 and closest<.74:
            # Solid scorched crater floors prevent accidental holes into the void.
            put(p,'basalt'if noise(x//2,z//2,36)>.52 else'polished_basalt',axis='y')
    # Shockwave damage reaches the surviving buildings beyond the blast cores.
    # Clustered roof losses and window failures keep recognizable, damaged shells.
    peripheral_roofs=[(40,26,8,7,10),(25,26,6,5,17),
                      (69,20,9,8,7),(104,24,10,8,7),
                      (26,241,11,7,7)]
    peripheral_removed=0
    for p in list(blocks):
        x,y,z=p;nm=name(p)
        if nm=='minecraft:air':continue
        roof=any(y>=bottom and ellipse(x,z,cx,cz,rx,rz)<.8+.3*coherent(x,z,4,301)
                 for cx,cz,rx,rz,bottom in peripheral_roofs)
        windows=('glass'in nm or'window'in nm)and coherent(x+y*2,z,5,311)>.02
        torn_wall=(29<=x<=46 and z>=30 and z<=32 and 8<=y<=11 and 38<=x<=45)
        torn_wall|=(x>=110 and x<=113 and 14<=z<=22 and y>=3)
        breach=(z<=4 and 12<=x<=19 and y>=2)or(z>=295 and 14<=x<=25 and y>=2)
        broken_railing=nm=='minecraft:iron_bars'and coherent(x,z,7,317)>.35
        if roof or windows or torn_wall or breach or broken_railing:
            erase(p);peripheral_removed+=1
        elif nm=='createdeco:dean_bricks'and coherent(x,z,5,331)>.10:
            put(p,'createdeco:cracked_dean_bricks')
        elif nm=='minecraft:waxed_weathered_cut_copper_slab':
            put(p,'oxidized_cut_copper_slab',**props(p))
    # Surface aging follows broad patches; no checkerboard random block deletion.
    weathered=0
    for p in list(blocks):
        x,y,z=p;nm=name(p)
        patch=noise(x//4,z//5,11)
        if nm=='minecraft:stone_bricks' and patch>.64:
            put(p,'cracked_stone_bricks');weathered+=1
        elif nm=='minecraft:polished_andesite' and y==1 and patch>.90:
            put(p,'andesite');weathered+=1
        elif nm in ['minecraft:cut_copper_slab','minecraft:weathered_cut_copper_slab']:
            put(p,'oxidized_cut_copper_slab',**props(p));weathered+=1
        elif nm=='minecraft:glass' and (x<20 and 40<z<61 or 29<=x<=46 and 14<=z<=32) and noise(x,z,y)>.76:
            erase(p)
    # Failed windows are removed in clusters, while walls and the lodge stair survive.
    for p in list(blocks):
        x,y,z=p
        if name(p)=='createdeco:industrial_iron_window' and (
                88<=x<=113 and 50<=z<=64 or 6<=x<=17 and 42<=z<=57):
            if noise(x//2,z//2,y//2)>.40:erase(p)
    # Collapsed sheet-metal roof sections rest on masonry rubble, rather than hover.
    rubble_centres=[(96,58,10,10),(76,55,7,7),(10,48,7,8),
                    (15,207,10,9),(13,275,7,7),(110,78,8,10),(69,6,5,4),
                    (13,70,10,7),(41,42,8,6),(9,80,7,9),(39,113,8,8),
                    (11,135,8,8),(41,174,9,7),(7,189,7,7),(37,226,8,7),
                    (85,155,7,8),(111,190,6,7),(13,260,7,6),(36,291,7,5),
                    (39,31,5,4),(69,23,8,6),(103,23,8,6),(25,244,8,5),(18,14,5,6)]
    rubble=0
    for cx,cz,rx,rz in rubble_centres:
        for x in range(max(4,cx-rx),min(115,cx+rx)+1):
            for z in range(max(5,cz-rz),min(294,cz+rz)+1):
                if ellipse(x,z,cx,cz,rx,rz)>1 or noise(x,z,25)<.57:continue
                # Keep workstations/containers and the central evacuation road clear.
                if 48<=x<=58 or name((x,2,z))!='minecraft:air' or name((x,1,z))=='minecraft:air':continue
                height=2 if ellipse(x,z,cx,cz,rx,rz)<.22 and noise(x,z,5)>.67 else 1
                for y in range(2,2+height):
                    if name((x,y,z))=='minecraft:air':
                        put((x,y,z),'cobbled_deepslate'if cx>60 else'cracked_stone_bricks');rubble+=1
                if noise(x,z,32)>.64 and name((x,2+height,z))=='minecraft:air':
                    put((x,2+height,z),'oxidized_cut_copper_slab',type='bottom',waterlogged=False);rubble+=1
    # Recognizable grounded fragments of the failed gun, not a falsely functional gun.
    for x,z,length in [(91,88,4),(79,91,3),(102,107,3),(65,116,2)]:
        for zz in range(z,z+length):
            floor=0 if name((x,1,zz))=='minecraft:air'else 1
            put((x,floor+1,zz),'createbigcannons:cast_iron_cannon_barrel',facing='south')
    # Pools are contained by a continuous bottom and retain access around them.
    for cx,cz,rx,rz in [(106,86,4,5),(11,211,3,4)]:
        for x in range(cx-rx,cx+rx+1):
            for z in range(cz-rz,cz+rz+1):
                if ellipse(x,z,cx,cz,rx,rz)<1 and name((x,2,z))=='minecraft:air':
                    put((x,0,z),'andesite');put((x,1,z),'water',level=0)
    # Sparse old cobwebs, attached to retained corners; no grass carpet or live fire.
    for p in [(8,4,57),(40,9,16),(31,9,29),(7,4,218),(39,4,278),(91,5,47)]:
        if name(p)=='minecraft:air' and any(name((p[0]+dx,p[1],p[2]+dz))!='minecraft:air'for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]):
            put(p,'cobweb')
    # Heavy pieces landed inside the craters as well as on their rims. Keep their
    # footprints clustered and recognizable instead of leaving empty stamped disks.
    wreckage=0
    wall_runs=[(11,57,7,'x'),(34,47,6,'z'),(16,92,6,'z'),
               (36,106,5,'x'),(12,164,7,'z'),(31,146,5,'x'),
               (12,204,7,'x'),(30,215,5,'z'),(17,273,6,'z'),
               (77,71,7,'z'),(97,94,5,'x'),(91,170,6,'z')]
    for index,(x,z,length,axis)in enumerate(wall_runs):
        for i in range(length):
            xx=x+(i if axis=='x'else 0);zz=z+(i if axis=='z'else 0)
            floor=0 if name((xx,1,zz))=='minecraft:air'else 1
            height=1+(i+index)%3
            for yy in range(floor+1,floor+1+height):
                if name((xx,yy,zz))!='minecraft:air':break
                put((xx,yy,zz),'cracked_stone_bricks'if index%2 else'createdeco:dean_bricks');wreckage+=1
    # Wide roof panels and the broken loading gantry lie across their foundations.
    for cx,cz,wide,long in [(16,60,5,3),(33,89,4,3),(18,157,7,2),
                            (29,210,5,3),(23,282,4,3),(78,62,6,3)]:
        for dx in range(wide):
            for dz in range(long):
                xx,zz=cx+dx,cz+dz
                floor=0 if name((xx,1,zz))=='minecraft:air'else 1
                if name((xx,floor+1,zz))!='minecraft:air':continue
                put((xx,floor+1,zz),'cobbled_deepslate');wreckage+=1
                if dx==0 and dz==0:continue
                put((xx,floor+2,zz),'oxidized_cut_copper_slab',type='bottom',waterlogged=False);wreckage+=1
    for x in range(19,33):
        z=151 if x<25 else 153 if x<29 else 154
        floor=0 if name((x,1,z))=='minecraft:air'else 1
        if name((x,floor+1,z))=='minecraft:air':
            put((x,floor+1,z),'create:industrial_iron_block');wreckage+=1
    # Two shielded storage corners survived outside the warehouse blast core.
    # Give the wreck a small salvage reward without restoring its original racks.
    for p in [(7,2,65),(43,2,64)]:
        x,y,z=p
        if name((x,1,z))=='minecraft:air':put((x,1,z),'cobbled_deepslate')
        put(p,'barrel',facing='south',open=False)
        blocks[p]['nbt']=n.Compound({'id':n.String('minecraft:barrel'),'Items':n.List[n.Compound]([
            n.Compound({'Slot':n.Byte(0),'id':n.String('minecraft:iron_ingot'),'count':n.Int(12)}),
            n.Compound({'Slot':n.Byte(4),'id':n.String('create:iron_sheet'),'count':n.Int(8)})])})
    # Thinned, still categorized warehouse stock; preserve useful lodge provisions.
    stock=collections.Counter();rare=[]
    for p,b in blocks.items():
        be=b.get('nbt')
        if be is None:continue
        be.pop('CustomName',None)
        if 'Items'not in be:continue
        lodge=29<=p[0]<=46 and 14<=p[2]<=32
        warehouse=5<=p[0]<=44 and 42<=p[2]<=68
        for item in be['Items']:
            if 'count'in item:
                amount=int(item['count']);factor=.65 if lodge else .35 if warehouse else .45
                item['count']=n.Int(max(1,math.ceil(amount*factor)))
        stock['lodge'if lodge else'warehouse'if warehouse else'workshop']+=1
        if warehouse and len(rare)<2 and p[1]==2:
            rare.append(p)
    for p,items in zip(rare,[[('create:precision_mechanism',3),('create:sturdy_sheet',6)],
                            [('minecraft:diamond',2),('minecraft:netherite_scrap',1)]]):
        be=blocks[p]['nbt'];used={int(i['Slot'])for i in be['Items']}
        for item,count in items:
            slot=next(i for i in range(27)if i not in used);used.add(slot)
            be['Items'].append(n.Compound({'Slot':n.Byte(slot),'id':n.String(item),'count':n.Int(count)}))
    # Remove fragile remnants whose supports were destroyed, and paired block halves.
    for iteration in range(3):
        for p in list(blocks):
            nm=name(p);pr=props(p);x,y,z=p
            if nm.endswith('_door'):
                other=(x,y+(-1 if pr.get('half')=='upper'else 1),z)
                if name(other)!=nm:erase(p)
            if nm in ['minecraft:rail','minecraft:lantern','minecraft:redstone_wire']:
                support=(x,y+(1 if pr.get('hanging')=='true'else-1),z)
                if name(support)=='minecraft:air':erase(p)
            if nm.endswith('_bed'):
                dx,dz={'north':(0,-1),'south':(0,1),'east':(1,0),'west':(-1,0)}[pr['facing']]
                if pr.get('part')=='head':dx,dz=-dx,-dz
                if name((x+dx,y,z+dz))!=nm:erase(p)
    # Prune fragments newly disconnected by this accident, while leaving the
    # intact model's deliberately separated cannon/kinetic parts untouched.
    def grounded(source):
        remaining={p for p,b in source.items()if str(palette[int(b['state'])]['Name'])!='minecraft:air'}
        supported=set();islands=[]
        while remaining:
            seed=remaining.pop();queue=[seed];component={seed};ground=seed[1]<=1
            for x,y,z in queue:
                for q in [(x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)]:
                    if q in remaining:
                        remaining.remove(q);component.add(q);queue.append(q);ground|=q[1]<=1
            if ground:supported.update(component)
            else:islands.append(component)
        return supported,islands
    was_supported,_=grounded(original)
    _,islands=grounded(blocks);loose=0
    for component in islands:
        if component & was_supported:
            for p in component:erase(p);loose+=1
    # Broken railings must no longer visually connect across destroyed wall sections.
    for p in list(blocks):
        nm=name(p)
        if nm!='minecraft:iron_bars':continue
        pr=props(p);x,y,z=p
        for direction,(dx,dz)in {'north':(0,-1),'south':(0,1),'east':(1,0),'west':(-1,0)}.items():
            if name((x+dx,y,z+dz))=='minecraft:air':pr[direction]='false'
        old_nbt=deepcopy(blocks[p].get('nbt'))
        put(p,nm,**pr)
        if old_nbt is not None:blocks[p]['nbt']=old_nbt
    surface_changed=sum(p not in original or blocks[p]!=original[p]for p in blocks)
    surface_removed=sum(name(p)=='minecraft:air'and str(palette[int(b['state'])]['Name'])!='minecraft:air'for p,b in original.items())
    # V3: provide genuine underground volume. World placement must lower the
    # template origin by this offset, keeping every surviving building at grade.
    ground_offset=12
    limits=[120,42,300]
    surface=blocks
    blocks={}
    moved={}
    for (x,y,z),b in surface.items():
        position=(x,y+ground_offset,z);copied=deepcopy(b)
        copied['pos']=n.List[n.Int](position);blocks[position]=copied
        moved[x,y,z]=position
    depths=[11,8,10,7,9,6,8,5]
    satellites=[]
    for index,(_,cx,cz,rx,rz)in enumerate(detonations):
        for side in range(2):
            angle=index*1.73+side*2.4
            satellites.append((cx+math.cos(angle)*rx*.62,cz+math.sin(angle)*rz*.62,
                               5+index%4,5+(index+side)%5,3+(index+side)%3))
    crater_depths={};deep_columns=0;relocated=0
    def old_name(position):
        b=surface.get(position)
        return str(palette[int(b['state'])]['Name'])if b is not None else'minecraft:air'
    for x in range(120):
        for z in range(300):
            influences=[]
            for i,blast in enumerate(detonations):influences.append(blast_distance(x,z,i,blast))
            if min(influences)>1.75:continue
            # Layered substrate is authored only around the blast footprints;
            # the rest remains the dimension's naturally generated underground.
            for y in range(ground_offset):
                geology=coherent(x+y*1.3,z-y*1.8,9,127)
                rock=('deepslate'if y<3+coherent(x,z,13,92)*2 else'tuff'if geology<-.45 else'stone'if geology>.32 else'andesite')
                put((x,y,z),rock,**({'axis':'y'}if rock=='deepslate'else{}))
            profiles=[]
            for i,(maximum,distance)in enumerate(zip(depths,influences)):
                t=max(0,1-distance/.99)
                if not t:profiles.append(0);continue
                _,cx,cz,rx,rz=detonations[i]
                angle=math.atan2(z-cz,x-cx)
                # One side is steep while the other is a slumped ramp. Local
                # shelves, ridges and gullies interrupt the closed level rings.
                power=.48+.62*(.5+.5*math.sin(angle+i*1.8))
                envelope=math.sin(math.pi*t)**.65
                relief=2.1*coherent(x,z,8,143+i)+1.0*coherent(x,z,3.6,179+i)
                cliff=1.6 if coherent(x,z,13,219+i)>.10 else-.6
                profiles.append(min(maximum,max(0,round(maximum*t**power+envelope*(relief+cliff)))))
            depth=max(profiles)
            if min(influences)<1.3:
                for cx,cz,rx,rz,maximum in satellites:
                    local=ellipse(x,z,cx,cz,rx,rz)
                    if local<1:depth=max(depth,round(maximum*(1-local)**.7))
            if depth<=1:continue
            floor=ground_offset+1-depth
            previous_floor=0 if old_name((x,1,z))in ['minecraft:air','minecraft:water']else 1
            # Capture the low surviving debris and foundations before excavating.
            debris=[]
            for yy in range(previous_floor+1,30):
                prior=surface.get((x,yy,z))
                if prior is None:continue
                nm=str(palette[int(prior['state'])]['Name'])
                if nm not in ['minecraft:air','minecraft:water']:
                    debris.append((yy,prior))
                erase((x,yy+ground_offset,z))
            for y in range(floor+1,ground_offset+previous_floor+1):erase((x,y,z))
            # A solid base and irregular exposed geology give actual depth; the
            # scorched material is concentrated near the detonation centre.
            scorch=min(influences)<.35 and coherent(x,z,7,61)>.12
            geology=coherent(x,z,8,233)
            bottom=('cobbled_deepslate'if scorch else'tuff'if geology<-.35 else'deepslate'if floor<5 and geology>.1 else'andesite'if geology<.3 else'stone')
            put((x,floor,z),bottom,**({'axis':'y'}if bottom=='deepslate'else{}))
            for yy,b in debris:
                destination=(x,floor+yy-previous_floor,z)
                assert 0<=destination[1]<limits[1]
                copied=deepcopy(b);copied['pos']=n.List[n.Int](destination)
                blocks[destination]=copied;moved[x,yy,z]=destination;relocated+=1
            crater_depths[x,z]=depth;deep_columns+=1
    # Asymmetric landslide fans bridge several height levels. The gravel always
    # rests on solid rubble; pieces with inventory are never buried or replaced.
    talus=0
    for i,(_,cx,cz,rx,rz)in enumerate(detonations):
        angle=i*1.9+.8
        fx,fz=cx+math.cos(angle)*rx*.48,cz+math.sin(angle)*rz*.48
        for x in range(max(0,int(fx)-7),min(119,int(fx)+7)+1):
            for z in range(max(0,int(fz)-9),min(299,int(fz)+9)+1):
                if (x,z)not in crater_depths:continue
                radius=ellipse(x,z,fx,fz,6,8)
                if radius>1+.25*coherent(x,z,5,269+i):continue
                floor=ground_offset+1-crater_depths[x,z]
                if any(name((x,y,z))!='minecraft:air'for y in range(floor+1,floor+4)):continue
                height=1+int(radius<.4)+int(radius<.13 and i%2==0)
                for yy in range(floor+1,floor+height+1):
                    put((x,yy,z),'gravel'if yy==floor+height else'cobbled_deepslate'if i%2 else'cobblestone');talus+=1
    # Connection flags follow the relocated wall pieces rather than their old
    # positions. Remove rail/lantern remnants which have lost their support.
    for p in list(blocks):
        nm=name(p)
        if nm=='minecraft:iron_bars':
            pr=props(p);x,y,z=p
            for direction,(dx,dz)in {'north':(0,-1),'south':(0,1),'east':(1,0),'west':(-1,0)}.items():
                if name((x+dx,y,z+dz))=='minecraft:air':pr[direction]='false'
            put(p,nm,**pr)
        elif nm in ['minecraft:rail','minecraft:lantern','minecraft:redstone_wire']:
            pr=props(p);x,y,z=p
            if name((x,y+(1 if pr.get('hanging')=='true'else-1),z))=='minecraft:air':erase(p)
    geometry=fix_support_geometry(blocks,palette)
    template['size']=n.List[n.Int](limits)
    template['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)])
    encounters=populate(template)
    template.save(FOLDER/'engineering_base_ruined.nbt')
    sections=FOLDER/'ruined_base';sections.mkdir(exist_ok=True)
    # Each section has its own local origin, matching the intact base's 100-block slices.
    occupied=0
    for index in range(3):
        part=deepcopy(template);part['size']=n.List[n.Int]([120,limits[1],100]);part_blocks=[]
        for p,b in sorted(blocks.items()):
            if index*100<=p[2]<(index+1)*100:
                local=deepcopy(b);local['pos']=n.List[n.Int]([p[0],p[1],p[2]-index*100]);part_blocks.append(local);occupied+=1
        part['blocks']=n.List[n.Compound](part_blocks);part['entities']=partition_entities(template['entities'],index);part.save(sections/f'section_{index}.nbt')
    assert occupied==len(blocks)
    assert before==hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'Intact template changed'
    assert len(template['entities'])==16
    for p,b in blocks.items():
        assert all(0<=v<limit for v,limit in zip(p,limits))
        if name(p)=='minecraft:air':assert'nbt'not in b
    # The apron explosion reaches the road. A lower crater floor can be crossed
    # with one-block steps; avoid leaving an implausibly untouched strip of road.
    def walking_height(x,z):
        for floor in range(ground_offset+4,-1,-1):
            if name((x,floor,z)) not in ['minecraft:air','minecraft:water'] and all(name((x,y,z))=='minecraft:air'for y in [floor+1,floor+2]):return floor
        return None
    start=(52,8);goal=(52,290);route={start};queue=[start]
    for x,z in queue:
        height=walking_height(x,z)
        assert height is not None
        for xx,zz in [(x+1,z),(x-1,z),(x,z+1),(x,z-1)]:
            if not(46<=xx<=60 and 6<=zz<=293)or(xx,zz)in route:continue
            other=walking_height(xx,zz)
            if other is not None and abs(other-height)<=1:route.add((xx,zz));queue.append((xx,zz))
    assert goal in route,'No traversable route through the damaged road'
    DESIGN.mkdir(exist_ok=True,parents=True)
    result={'size':limits,'ground_offset':ground_offset,'original_ground_y':ground_offset+1,
            'source_sha256':before,'changed_surface_block_entries':surface_changed,
            'template_entries':len(blocks),'removed_surface_blocks':surface_removed,
            'weathered_blocks':weathered,'rubble_blocks':rubble,'heavy_wreckage_blocks':wreckage,'newly_unsupported_fragments_removed':loose,'stocked_fixed_containers':dict(stock),
            'rare_cache_positions':[list(moved[p])for p in rare],'sections':3,'intact_template_unchanged':True,
            'deepened_columns':deep_columns,'relocated_debris_blocks':relocated,'maximum_crater_depth':max(crater_depths.values()),
            'secondary_crater_lobes':len(satellites),'landslide_rubble_blocks':talus,
            'peripheral_shockwave_blocks_removed':peripheral_removed,
            'geometry_support_fixes':len(geometry['support_slabs']),'geometry_lamp_fixes':len(geometry['relocated_lamps']),
            'detonations':[{'id':key,'center':[cx,cz],'radius':[rx,rz],'maximum_depth':depths[i]}for i,(key,cx,cz,rx,rz)in enumerate(detonations)],
            'evacuation_route':'Traversable from (52,8) to (52,290), including crater detours and one-block steps',
            'worldgen':'registered ruined-base variant; worldgen and /place apply the 12-block underground offset'}
    (DESIGN/'manifest.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False),flush=True)

if __name__=='__main__':main()
