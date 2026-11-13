"""Concrete bunker with independent roof guns and a connected underground vault."""
from pathlib import Path
from copy import deepcopy
from collections import Counter
from nuclear_bunker_interiors import build as build_interiors, STAIRS, POWER
from compact_hand_cranks import compact
from nuclear_bunker_entrance import build as build_entrance, build_vault
import json, math
import nbtlib as n
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'tools/engineering/nuclear_bunker_v25'
SIZE=[96,40,96]

def main():
    palette=[];indices={};blocks={};stocked=[];loot_positions=[];entities=[]
    def put(x,y,z,name,**props):
        assert all(0<=v<s for v,s in zip([x,y,z],SIZE)),(x,y,z)
        if ':'not in name:name='minecraft:'+name
        if name=='create:shaft':name='create:andesite_encased_shaft'
        if name=='minecraft:andesite':name='minecraft:gray_concrete'
        key=(name,tuple(sorted((k,str(v).lower())for k,v in props.items())))
        if key not in indices:
            state=n.Compound({'Name':n.String(name)})
            if props:state['Properties']=n.Compound({k:n.String(str(v).lower())for k,v in props.items()})
            indices[key]=len(palette);palette.append(state)
        blocks[x,y,z]=n.Compound({'pos':n.List[n.Int]([x,y,z]),'state':n.Int(indices[key])})
    def box(x,y,z,X,Y,Z,name,**props):
        for xx in range(x,X+1):
            for yy in range(y,Y+1):
                for zz in range(z,Z+1):put(xx,yy,zz,name,**props)
    def clear(x,y,z,X,Y,Z):box(x,y,z,X,Y,Z,'air')
    def name(p):return str(palette[int(blocks[p]['state'])]['Name'])if p in blocks else'minecraft:air'
    def stock(x,y,z,items,facing='south',cabinet=False):
        put(x,y,z,'farmersdelight:spruce_cabinet'if cabinet else'barrel',facing=facing,open=False)
        def tag(value):
            if isinstance(value,dict):return n.Compound({k:tag(v)for k,v in value.items()})
            if isinstance(value,str):return n.String(value)
            if isinstance(value,int):return n.Int(value)
            raise TypeError(value)
        slots=[0,2,4,6,8,9,11,13,15,17,18,20,22,24,26,1,3,5,7,10,12,14,16,19,21,23,25]
        assert len(items)<=27
        entries=[]
        for slot,item in zip(slots,items):
            entry=n.Compound({'Slot':n.Byte(slot),'id':n.String(item[0]if ':'in item[0]else'minecraft:'+item[0]),'count':n.Int(item[1])})
            if len(item)>2:entry['components']=tag(item[2])
            entries.append(entry)
        blocks[x,y,z]['nbt']=n.Compound({'id':n.String('farmersdelight:cabinet'if cabinet else'minecraft:barrel'),'Items':n.List[n.Compound](entries)})
        stocked.append([x,y,z])
    def window_supply(x,y,z,facing):
        put(x,y,z,'barrel',facing=facing,open=False)
        blocks[x,y,z]['nbt']=n.Compound({'id':n.String('minecraft:barrel'),'LootTable':n.String('ccq_testground:chests/engineering/bunker_observation')})
        loot_positions.append([x,y,z])
    def footprint(x,z):
        central=16<=x<=79 and 16<=z<=79 and abs(x-47.5)+abs(z-47.5)<=53
        caps=any(abs(x-cx)<=12 and abs(z-cz)<=12 and abs(x-cx)+abs(z-cz)<=18 for cx,cz in [(19,19),(76,19),(19,76),(76,76)])
        return central or caps
    cells={(x,z)for x in range(4,92)for z in range(4,92)if footprint(x,z)}
    edge={(x,z):min(max(abs(dx),abs(dz))for dx in range(-4,5)for dz in range(-4,5)if not footprint(x+dx,z+dz))
          for x,z in cells if any(not footprint(x+dx,z+dz)for dx in range(-4,5)for dz in range(-4,5))}
    # Foundation/roof form one connected mass, rather than separate factory sheds.
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        clear(cx-5,9,cz-14,cx+5,20,cz+14)
    for x,z in sorted(cells):
        box(x,0,z,x,8,z,'andesite')
        put(x,8,z,'polished_andesite')
        clear(x,9,z,x,23,z)
        if edge.get((x,z),10)<=3:box(x,9,z,x,22,z,'light_gray_concrete')
        put(x,13,z,'gray_concrete');put(x,14,z,'polished_andesite')
        put(x,22,z,'gray_concrete');put(x,23,z,'polished_andesite')
        if edge.get((x,z),10)==1:put(x,23,z,'polished_deepslate')
    # Low sloping armor skirts visually tie the thick walls into the ground.
    for x in range(4,92):
        for z in range(4,92):
            if (x,z)in cells:continue
            distance=min((max(abs(dx),abs(dz))for dx in range(-3,4)for dz in range(-3,4)if(x+dx,z+dz)in cells),default=99)
            if distance<=3:box(x,7,z,x,11-distance,z,'andesite')
    # Glazed observation recesses; the separate gun bores are authored below.
    def observation(x,z,X,Z):
        clear(x,10,z,X,10,Z)
        for xx in range(x,X+1):
            for zz in range(z,Z+1):
                if(xx,zz)in cells and edge.get((xx,zz),10)<=3:put(xx,10,zz,'createnuclear:reinforced_glass')
    ports=[]
    for z in [37,47,59]:
        for x,X in [(12,22),(73,83)]:observation(x,z,X,z+1);ports.append([x,z])
    for x in [35,47,59]:observation(x,12,x+1,22)
    for cx,cz in [(19,19),(76,19)]:
        observation(cx-1,cz-14,cx+1,cz-7)
        observation(cx-14 if cx<48 else cx+7,cz-1,cx-7 if cx<48 else cx+14,cz+1)
    # Open operations deck: no transverse, longitudinal or glazed partitions.
    # The basement likewise uses an open service gallery and four loading plinths.
    clear(29,2,17,66,7,50)
    box(28,1,16,67,7,16,'createnuclear:lead_block')
    box(28,1,17,28,7,50,'createnuclear:lead_block');box(67,1,17,67,7,50,'createnuclear:lead_block')
    box(29,1,17,66,1,50,'deepslate_tiles');box(29,7,17,66,7,50,'createnuclear:steel_block')
    charges=[]
    for row,z in enumerate([21,27,45]):
        for col,x in enumerate([32,38,44,50,56,62]):
            tier=[1,1,2,2,3,3][col]
            box(x-1,2,z-1,x+1,2,z+1,'createnuclear:steel_block')
            put(x,3,z,f'createnucleararmaments:nuclear_charge_t{tier}')
            charges.append({'pos':[x,3,z],'tier':tier})
            for zz in [z-2,z+2]:put(x,1,zz,'sea_lantern')
    # An internal broad stair descends from the low central hallway to the vault.
    clear(43,2,48,52,12,61)
    for step in range(8):
        z=61-step;y=8-step
        if step<6:box(44,2,z,51,y-1,z,'gray_concrete')
        box(44,y,z,51,y,z,'stone_brick_stairs',facing='south',half='bottom',shape='straight',waterlogged=False)
    clear(44,2,48,51,5,53)
    for x in [42,53]:box(x,8,49,x,9,61,'gray_concrete')
    # Roof guns include the complete floor-level yaw transmission, not only the upper assembly.
    source=n.load(ROOT/'src/main/resources/data/ccq_testground/structure/engineering/engineering_base.nbt')
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        rear=cz==19
        # Independent roof gun: the lower room is sealed, with no firing hole.
        for b in source['blocks']:
            x,y,z=map(int,b['pos']);s=source['palette'][int(b['state'])];nm=str(s['Name'])
            if not(68<=x<=73 and 1<=y<=5 and 71<=z<=84)or nm=='minecraft:air':continue
            p=(cx+(-1 if rear else 1)*(x-70),y+13,cz+(-1 if rear else 1)*(z-76))
            opposite={'north':'south','south':'north','east':'west','west':'east','up':'up','down':'down'}
            if nm.startswith('create:') or nm=='minecraft:lever':continue
            props={str(k):str(v)for k,v in s.get('Properties',{}).items()}
            if rear and'facing'in props:props['facing']=opposite[props['facing']]
            put(*p,nm,**props)
            if'nbt'in b:
                tag=deepcopy(b['nbt'])
                if rear:
                    if'Facing'in tag:tag['Facing']=n.String(opposite[str(tag['Facing'])])
                    for key in ['Connections','Welds']:
                        if key in tag:tag[key]=n.List[n.String]([opposite[str(v)]for v in tag[key]])
                blocks[p]['nbt']=tag
                if nm=='create:rotation_speed_controller':blocks[p]['nbt']['ScrollValue']=n.Int(8)
    ledger,room_walls=build_interiors(put,box,clear,stock,window_supply,name)
    blocks[tuple(POWER['master_controller'])]['nbt']=n.Compound({'id':n.String('dndesires:omni_speed_controller'),'ScrollValue':n.Int(8)})
    # CBC adds log-cutting mould recipes. Pin this woodworking saw to stripping
    # instead of silently selecting whichever compatible recipe loads first.
    for pos in POWER['saws']:
        blocks[tuple(pos)]['nbt']=n.Compound({'id':n.String('create:mechanical_saw'),
            'Filter':n.Compound({'id':n.String('minecraft:stripped_oak_log'),'count':n.Int(1)}),'FilterAmount':n.Int(1)})
    # Basement-only handling hall: four configured arms in two reciprocal loops.
    # Floor-level transmission is encased, so the loading aisle remains level.
    for y in range(2,10):put(64,y,19,'create:shaft',axis='y')
    # Independent default-off basement drive; the upper workshop keeps running.
    put(64,6,19,'create:clutch',axis='y',powered=True)
    put(65,6,19,'lever',face='wall',facing='east',powered=True)
    put(64,1,19,'create:gearbox',axis='x')
    for z in range(20,36):put(64,1,z,'create:shaft',axis='z')
    put(64,1,36,'create:gearbox',axis='y')
    for x in range(36,64):put(x,1,36,'create:shaft',axis='x')
    arm_configs=[]
    for left,right,dx in [(36,41,38),(54,59,56)]:
        A=(dx,2,33);B=(dx,2,39)
        for pos in [A,B]:put(*pos,'create:depot')
        blocks[A]['nbt']=n.Compound({'id':n.String('create:depot'),'HeldItem':n.Compound({
            'Item':n.Compound({'id':n.String('createnucleararmaments:nuclear_charge_t1'),'count':n.Int(1)}),
            'Pos':n.Float(.5),'PrevPos':n.Float(.5),'Offset':n.Float(0),'PrevOffset':n.Float(0),'InDirection':n.Int(2),'Angle':n.Int(0)})})
        for x,source,dest in [(left,A,B),(right,B,A)]:
            pos=(x,2,36)
            put(x,1,36,'create:gearbox',axis='y')
            put(x,1,35,'create:gearbox',axis='x')
            put(x,2,35,'create:cogwheel',axis='y')
            put(*pos,'create:mechanical_arm',ceiling=False)
            points=[]
            for target,mode in [(source,'TAKE'),(dest,'DEPOSIT')]:
                points.append(n.Compound({'Type':n.String('create:depot'),'Mode':n.String(mode),
                    'Pos':n.IntArray([target[i]-pos[i]for i in range(3)])}))
            blocks[pos]['nbt']=n.Compound({'id':n.String('create:mechanical_arm'),'InteractionPoints':n.List[n.Compound](points),
                'Phase':n.String('SEARCH_INPUTS'),'TargetPointIndex':n.Int(0),'MovementProgress':n.Float(0)})
            arm_configs.append({'pos':list(pos),'input':list(source),'output':list(dest)})
    # Set the narrower firing screen closer to the muzzle; eliminate front
    # corner piers. Rear supports carry the cantilever outside the firing fan.
    firing_ports=[]
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        sx=-1 if cx==19 else 1;sz=-1 if cz==19 else 1
        for x,z in sorted(cells):
            dx=x-cx;dz=z-cz
            if abs(dx)>12 or abs(dz)>12 or abs(dx)+abs(dz)>18:continue
            if edge.get((x,z),10)<=3 and (sx*dx>=7 or sz*dz>=7):clear(x,15,z,x,21,z)
        def screen(dx,dz):return abs(dx)<=7 and abs(dz)<=7 and abs(dx)+abs(dz)<=11
        for dx in range(-7,8):
            for dz in range(-7,8):
                if not screen(dx,dz) or not any(not screen(dx+a,dz+b)for a in [-1,0,1]for b in [-1,0,1]):continue
                if sx*dx < -4 or sz*dz < -4:continue
                x,z=cx+dx,cz+dz
                box(x,15,z,x,21,z,'light_gray_concrete')
                put(x,16,z,'polished_andesite_slab',type='bottom',waterlogged=False)
                clear(x,17,z,x,19,z)
                put(x,20,z,'polished_andesite_slab',type='top',waterlogged=False)
                firing_ports.append([x,z])
        # Open the rear of every gallery to the hall; the outward firing screen
        # and exterior hull remain, without an internal concrete partition.
        for dx,dz in [(-5,-5),(-5,5),(5,-5),(5,5)]:put(cx+dx,22,cz+dz,'sea_lantern')
    # Fill blind exterior rear recesses, not the protected access corridors or
    # the outward firing fan. Flat masses replace pockets an attacker could use.
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        sx=-1 if cx==19 else 1;sz=-1 if cz==19 else 1
        for u in range(-12,13):
            for v in range(-12,13):
                if abs(u)+abs(v)>18 or(abs(u)<=7 and abs(v)<=7 and abs(u)+abs(v)<=11):continue
                if (u<=-6 and -1<=v<=1)or(v<=-6 and -1<=u<=1):continue
                if u<3 and v<3:continue  # hall-facing rear stays completely open
                angle=math.degrees(math.atan2(v,u))
                if -30<=angle<=120:continue
                x,z=cx+sx*u,cz+sz*v
                box(x,15,z,x,21,z,'light_gray_concrete')
    # Shared water supply below the gun deck, clear of the independent yaw loop.
    put(64,13,19,'create:gearbox',axis='x')
    for z in range(16,74):
        if z==19:continue
        put(64,13,z,'create:gearbox',axis='y')if z in [16,73]else put(64,13,z,'create:shaft',axis='z')
    for z,lo,hi in [(16,16,73),(73,22,79)]:
        for x in range(lo,hi+1):
            if x!=64:put(x,13,z,'create:shaft',axis='x')
    cannon_drives=[]
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        sign=-1 if cz==19 else 1
        def gp(dx,y,dz):return(cx+sign*dx,y,cz+sign*dz)
        def gput(dx,y,dz,block,**props):put(*gp(dx,y,dz),block,**props)
        gx,_,gz=gp(3,15,-1);bus=16 if cz==19 else 73
        put(gx,13,bus,'create:gearbox',axis='z');put(gx,14,bus,'create:gearbox',axis='x')
        for zz in range(min(bus,gz)+1,max(bus,gz)):
            put(gx,14,zz,'create:shaft',axis='z')
        put(gx,14,gz,'create:gearbox',axis='x');put(gx,15,gz,'create:gearbox',axis='x')
        gput(3,15,0,'create:rotation_speed_controller',axis='z')
        blocks[gp(3,15,0)]['nbt']=n.Compound({'id':n.String('create:rotation_speed_controller'),'ScrollValue':n.Int(8)})
        gput(3,16,0,'create:large_cogwheel',axis='x')
        gput(2,16,0,'create:gearshift',axis='x',powered=False)
        gput(1,16,0,'create:clutch',axis='x',powered=True)
        gput(4,16,0,'create:gearbox',axis='z');gput(4,15,0,'create:clutch',axis='y',powered=True)
        gput(4,14,0,'create:gearbox',axis='x')
        for zz in [1,2]:gput(4,14,zz,'create:shaft',axis='z')
        gput(4,14,3,'create:gearshift',axis='z',powered=False);gput(4,14,4,'create:gearbox',axis='y')
        for xx in [1,2,3]:gput(xx,14,4,'create:shaft',axis='x')
        gput(0,14,4,'create:gearbox',axis='y')
        for zz in [1,2,3]:gput(0,14,zz,'create:shaft',axis='z')
        gput(0,14,0,'create:gearbox',axis='x');gput(0,15,0,'create:shaft',axis='y')
        front='south'if cz==19 else'north'
        # Eight adjacent controls beside the gun, each with its own rear transmitter.
        # Strong power stays in its own support; adjacent solid blocks do not relay it.
        panel_sign=(-1 if cx==19 else 1)*-sign
        station=(0 if cx==19 else 1)+(0 if cz==19 else 2)
        colour=['red_dye','blue_dye','yellow_dye','green_dye'][station]
        channels=['lever','compass','redstone_torch','clock','quartz','gunpowder','iron_nugget','paper']
        controls={}
        def link(p,channel,receiver,facing,powered=False):
            put(*p,'create:redstone_link',facing=facing,receiver=receiver,powered=powered)
            item=lambda i:n.Compound({'id':n.String('minecraft:'+i),'count':n.Int(1)})
            blocks[p]['nbt']=n.Compound({'id':n.String('create:redstone_link'),
                'FrequencyFirst':item(colour),'FrequencyLast':item(channels[channel]),
                'Transmitter':n.Byte(not receiver),'Transmit':n.Int(15 if powered and not receiver else 0),
                'Receive':n.Int(15 if powered and receiver else 0),'ReceivedChanged':n.Byte(receiver)})
        names=['assembly','yaw_stop','yaw_reverse','pitch_stop','pitch_reverse','fire','breech_dock','breech_open']
        for i,key in enumerate(names):
            dx=panel_sign*(i+2);stop=key in ['yaw_stop','pitch_stop']
            gput(dx,15,-3,'create:andesite_casing')
            gput(dx,15,-4,'polished_blackstone_button'if key=='fire'else'lever',
                face='wall',facing=front,powered=stop)
            link(gp(dx,15,-2),i,False,'north'if front=='south'else'south',stop)
            controls[key]=list(gp(dx,15,-4))
        for i,p,facing,stop in [
            (0,gp(0,16,-1),front,False),
            (1,gp(4,15,-1),front,True),
            (2,gp(5,15,3),'up',False),
            (3,gp(1,16,-1),front,True),
            (4,gp(2,16,-1),front,False),
            (5,gp(0,16,1),'north'if front=='south'else'south',False)]:
            link(p,i,True,facing,stop)
        # Retractable overhead drive. The fixed feed stays outside the gun sweep;
        # a glued shaft bridge lowers three blocks onto the vertical breech port.
        def bp(x,y,z):return gp(panel_sign*x,y,z)
        def bput(x,y,z,block,**props):put(*bp(x,y,z),block,**props)
        # Hide the fixed riser in the existing ladder backing, away from the aisle.
        fx,_,fz=bp(8,13,-7)
        for xx in range(min(gx,fx)+1,max(gx,fx)):
            if name((xx,13,bus)) in ['minecraft:air','minecraft:gray_concrete','minecraft:polished_andesite']:
                put(xx,13,bus,'create:shaft',axis='x')
        put(fx,13,bus,'create:gearbox',axis='y'if bus!=fz else'z')
        if bus!=fz:
            for zz in range(min(bus,fz)+1,max(bus,fz)):put(fx,13,zz,'create:shaft',axis='z')
            put(fx,13,fz,'create:gearbox',axis='x')
        for y in range(14,25):bput(8,y,-7,'create:shaft',axis='y')
        bput(8,18,-7,'create:gearbox',axis='x')
        bput(8,18,-6,'create:gearbox',axis='y')
        bput(7,18,-6,'create:gearbox',axis='y')
        for zz in [-5,-4]:bput(7,18,zz,'create:shaft',axis='z')
        bput(7,18,-3,'create:gearbox',axis='x')
        bput(7,19,-3,'dndesires:omni_speed_controller',axis='y')
        blocks[bp(7,19,-3)]['nbt']=n.Compound({'id':n.String('dndesires:omni_speed_controller'),'ScrollValue':n.Int(8)})
        bput(7,20,-3,'create:gearshift',axis='y',powered=False)
        bput(7,21,-3,'create:gearbox',axis='z')
        # Lift branch leaves the concealed riser before the breech reversing drive.
        bput(8,25,-7,'create:gearbox',axis='z')
        bput(7,25,-7,'create:shaft',axis='x')
        bput(6,25,-7,'create:gearbox',axis='y')
        bput(6,25,-6,'create:shaft',axis='z')
        bput(6,25,-5,'dndesires:omni_speed_controller',axis='z')
        blocks[bp(6,25,-5)]['nbt']=n.Compound({'id':n.String('dndesires:omni_speed_controller'),'ScrollValue':n.Int(-32)})
        bput(6,25,-4,'create:gearshift',axis='z',powered=False)
        for x in range(7):
            for y in range(19,25):bput(x,y,-3,'air')
            bput(x,25,-3,'light_gray_concrete')
            bput(x,24,-3,'create:gearbox'if x==0 else'create:shaft',axis='z'if x==0 else'x')
        for y in [22,23]:bput(0,y,-3,'create:shaft',axis='y')
        bput(6,25,-3,'create:sticky_mechanical_piston',facing='down',axis_along_first=False,state='retracted')
        blocks[bp(6,25,-3)]['nbt']=n.Compound({'id':n.String('create:mechanical_piston'),'MovementMode':n.Int(0)})
        for y in range(26,29):bput(6,y,-3,'create:piston_extension_pole',facing='down',axis_along_first=False)
        # gp mirrors X/Z around the gun: use block-inclusive bounds for a one-wide bridge.
        x0=min(bp(0,22,-3)[0],bp(6,22,-3)[0]);x1=max(bp(0,22,-3)[0],bp(6,22,-3)[0])+1
        zz=bp(0,22,-3)[2];center=[(x0+x1)/2,22.,zz+.5]
        entities.append(n.Compound({'pos':n.List[n.Double](center),'blockPos':n.List[n.Int]([int(center[0]),22,zz]),
            'nbt':n.Compound({'id':n.String('create:super_glue'),'Pos':n.List[n.Double](center),
                'Motion':n.List[n.Double]([0.,0.,0.]),'Rotation':n.List[n.Float]([0.,0.]),
                'From':n.List[n.Double]([x0-center[0],0.,-.5]),'To':n.List[n.Double]([x1-center[0],3.,.5])})}))
        right='east'if cx==19 else'west'
        link(bp(7,25,-4),6,True,right)
        link(bp(8,20,-3),7,True,right)
        cannon_drives.append({'mount':list(gp(0,16,0)),'controller':list(gp(3,15,0)),
            **controls,'radio_station_colour':colour,'panel_order':names,'panel_height':1,'panel_back_offset':3,'panel_spacing':1,'breech_riser':list(bp(8,18,-7)),'breech_piston':list(bp(6,25,-3)),'breech_drive':list(bp(7,20,-3)),'breech_dock_travel':3,'breech_dock_rpm':32})
    # Bypass the north-west bridge sweep without breaking the workshop ceiling main.
    for x,z in [(19,21),(18,21),(18,23),(19,23)]:put(x,22,z,'create:gearbox',axis='y')
    put(18,22,22,'create:shaft',axis='z')
    # CCA perimeter coils on supported outer ledges; keep the entrance clear.
    wire=[]
    for x in range(4,92):
        for z in range(4,92):
            if (x,z)in cells or (37<=x<=63 and z>=75):continue
            distance=min((max(abs(dx),abs(dz))for dx in range(-2,3)for dz in range(-2,3)if(x+dx,z+dz)in cells),default=99)
            if distance==2 and name((x,9,z))!='minecraft:air':
                dx,dz=min(((dx,dz)for dx in range(-2,3)for dz in range(-2,3)if(x+dx,z+dz)in cells),key=lambda v:(abs(v[0])+abs(v[1]),abs(v[1])))
                facing='north'if abs(dx)>=abs(dz)else'east'
                put(x,10,z,'createaddition:barbed_wire',facing=facing,vertical=False,waterlogged=False);wire.append([x,10,z])
    # Roof-edge coils protect the casemates without crossing any bore line.
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        for dx in range(-12,13):
            for dz in range(-12,13):
                if abs(dx)>12 or abs(dz)>12 or abs(dx)+abs(dz)>18:continue
                if abs(dx)==12 or abs(dz)==12 or abs(dx)+abs(dz)==18:
                    x,z=cx+dx,cz+dz
                    put(x,24,z,'createaddition:barbed_wire',facing='north'if abs(dx)>=abs(dz)else'east',vertical=False,waterlogged=False);wire.append([x,24,z])
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        sx=-1 if cx==19 else 1;sz=-1 if cz==19 else 1
        for dx in range(-12,13):
            for dz in range(-12,13):
                if abs(dx)+abs(dz)>18 or not(abs(dx)==12 or abs(dz)==12 or abs(dx)+abs(dz)==18):continue
                if sx*dx<6 and sz*dz<6:continue
                x,z=cx+dx,cz+dz
                put(x,15,z,'createaddition:barbed_wire',facing='north'if abs(dx)>=abs(dz)else'east',vertical=False,waterlogged=False);wire.append([x,15,z])
    doors=build_entrance(put,box,clear,entities)
    doors.append(build_vault(put,box,clear,entities))
    box(39,8,48,59,8,57,'polished_andesite')
    for x in [42,53]:clear(x,9,49,x,9,61)
    for x in [43,52]:
        box(x,8,58,x,8,61,'polished_andesite')
        box(x,9,57,x,9,61,'iron_bars',north=False,south=False,east=False,west=False,waterlogged=False)
    box(43,9,57,52,9,57,'iron_bars',north=False,south=False,east=False,west=False,waterlogged=False)
    # Armoured hemispherical observation rooms over the four breech bridges.
    for cx,cz in [(19,19),(76,19),(19,76),(76,76)]:
        ix=1 if cx==19 else-1;sz=-1 if cz==19 else 1
        ox,oz=cx+ix*2,cz-sz
        for x in range(ox-10,ox+11):
            for z in range(oz-10,oz+11):
                radius=math.hypot(x-ox,z-oz)
                if radius<=9.5 and name((x,23,z))=='minecraft:air':put(x,23,z,'polished_andesite')
                for y in range(24,34):
                    rr=math.sqrt(max(0,100-(y-23)**2))
                    if radius>rr or radius<max(0,rr-1.35):continue
                    material='light_gray_concrete'if y%3 else'polished_deepslate'
                    angle=math.degrees(math.atan2(z-oz,x-ox))%45
                    window=min(angle,45-angle)<8 and radius>8
                    if window and y==25:material='air'
                    if window and y==26:material='createnuclear:reinforced_glass'
                    if not name((x,y,z)).startswith(('create:','dndesires:')):put(x,y,z,material)
        put(ox,32,oz,'sea_lantern')
        # Rear access avoids the shaft bridge, live riser and cannon sweep.
        lx,lz=cx+ix*9,cz-sz*7
        for dz in [-7,-6,-5]:clear(lx,24,cz+sz*dz,lx,25,cz+sz*dz)
        for y in range(15,25):
            put(lx,y,lz,'ladder',facing='east'if ix>0 else'west',waterlogged=False)
            if not name((lx-ix,y,lz)).startswith('create:'):put(lx-ix,y,lz,'polished_deepslate')
        # Compact observation instruments on the hall side, outside moving hardware.
        for d,block in [(0,'radiologistics:main_computer'),(2,'radiologistics:radio_transmitter'),(4,'radiologistics:memory_module')]:
            x,z=ox+ix*(d-2),oz-sz*6
            put(x,24,z,'create:andesite_casing');put(x,25,z,block,facing='up')
        put(ox-ix*5,24,oz,'create:andesite_casing')
        put(ox-ix*5,25,oz,'simulated:gimbal_sensor',axis='z')
    # Recessed lighting on both occupied floors. Water machinery and stair
    # openings keep their authored floors instead of being patched by lighting.
    for x,z in sorted(cells):
        for floor,ceiling in [(8,13),(14,22)]:
            if ((x in [9,15,21,27,33,39,45,50,56,62,68,74,80,86])if floor==8 else x%6==3) and z%6==3 and name((x,floor+1,z))=='minecraft:air' and name((x,floor,z)) in ['minecraft:polished_andesite','minecraft:deepslate_tiles','minecraft:spruce_planks','createdeco:tiled_pearl_bricks','createdeco:tiled_blue_bricks','createdeco:industrial_iron_sheet_metal']:
                put(x,floor,z,'sea_lantern')
                if name((x,ceiling,z)) in ['minecraft:gray_concrete','minecraft:light_gray_concrete']:put(x,ceiling,z,'sea_lantern')
    # Always-on ceiling panels light the first floor even under furniture;
    # ceiling openings and the water/drive plant keep their authored geometry.
    light_grid=[12,17,22,27,32,37,42,46,49,53,58,63,68,73,78,83]
    for x in light_grid:
        for z in light_grid:
            if name((x,13,z)) in ['minecraft:gray_concrete','minecraft:light_gray_concrete'] and name((x,12,z))=='minecraft:air':
                put(x,13,z,'sea_lantern')
    # Flush, always-on lighting for every storage row and the handling aisles.
    # Keep the live shaft riser, door rail plane and arm interfaces intact.
    for x in [31,36,41,46,49,54,59,64]:
        for z in [19,24,29,34,39,44,48]:
            if name((x,7,z)) in ['createnuclear:steel_block','minecraft:polished_andesite','minecraft:gray_concrete','minecraft:light_gray_concrete']:
                put(x,7,z,'sea_lantern')
    for z in [30,42]:
        for x in range(30,66):
            if name((x,7,z))=='createnuclear:steel_block':put(x,7,z,'sea_lantern')
    for z in [48,49,50,52,53,54]:
        for x in [47,48]:
            if name((x,5,z)) in ['minecraft:light_gray_concrete','minecraft:polished_deepslate']:
                put(x,5,z,'sea_lantern')
    # Copy full-height material into any support slab that carries a block.
    gaps=0
    for p,b in list(blocks.items()):
        s=palette[int(b['state'])];x,y,z=p
        if str(s['Name']).endswith('_slab')and str(s.get('Properties',{}).get('type','bottom'))=='bottom'and name((x,y+1,z))!='minecraft:air':
            props={str(k):str(v)for k,v in s['Properties'].items()};props['type']='double';put(x,y,z,str(s['Name']),**props);gaps+=1
    # Later entrance/shell passes must never silently replace stocked furniture.
    for row in ledger:
        assert 'Items' in blocks[tuple(row['pos'])].get('nbt',{}), ('Overwritten stock',row['pos'])
    data=n.File(n.Compound({'DataVersion':n.Int(3955),'size':n.List[n.Int](SIZE),'palette':n.List[n.Compound](palette),
                           'blocks':n.List[n.Compound]([blocks[p]for p in sorted(blocks)]),'entities':n.List[n.Compound](entities)}),gzipped=True)
    crank_changes=compact(data)
    DEST.mkdir(parents=True,exist_ok=True);data.save(DEST/'nuclear_bunker.nbt')
    report={'size':SIZE,'ground_y':8,'highest_block':max(p[1]for p in blocks if name(p)!='minecraft:air'),
            'main_roof_y':23,'pillbox_roof_y':23,'upper_floor_y':14,'stairs':STAIRS,'automatic_power':POWER,'firing_port_cells':firing_ports,'template_entries':len(blocks),'charges':charges,
            'stocked_containers':sum('Items'in b.get('nbt',{})for b in blocks.values()),'observation_loot_containers':loot_positions,'gun_chance_per_station':0.45,
            'cannon_mounts':4,'cannon_drives':cannon_drives,'basement_arms':arm_configs,'command_post_center':[47.5,14,47.5],'barbed_wire':wire,'work_zones':{'metalworking':[33,21,47,46],'electrical':[53,21,63,30],'warehouse':[53,32,73,65],'milling_woodworking':[23,49,42,72],'command':[45,58,50,67]},'interior_layout':'lower_corner_rooms_upper_workshops','roof_gun_cover':{'underside_y':22,'top_y':23,'firing_gallery':'half_open'},'doors':doors,'room_walls':room_walls,
            'support_slab_fixes':gaps,'compacted_cranks':crank_changes,'prototype_only':False,'instance_files_changed':False}
    (DEST/'manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (DEST/'stock.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False),flush=True)
if __name__=='__main__':main()
