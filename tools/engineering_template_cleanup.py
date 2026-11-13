"""Shared finishing rules for authored CCQ engineering structure templates."""
import nbtlib as n
from collections import Counter
from copy import deepcopy


def fix_support_geometry(blocks,palette):
    """Fill load-bearing slab gaps and move inset lamps outside the envelope."""
    def state(p):return palette[int(blocks[p]['state'])]if p in blocks else None
    def name(p):return str(state(p)['Name'])if state(p)is not None else'minecraft:air'
    def put(p,s):
        if s not in palette:palette.append(s)
        blocks[p]=n.Compound({'pos':n.List[n.Int](p),'state':n.Int(palette.index(s))})
    slabs=[];lamps=[]
    for p in list(blocks):
        s=state(p);x,y,z=p
        if name(p).endswith('_slab')and str(s.get('Properties',{}).get('type','bottom'))=='bottom'and name((x,y+1,z))!='minecraft:air':
            s=deepcopy(s);s['Properties']=deepcopy(s['Properties']);s['Properties']['type']=n.String('double');put(p,s);slabs.append(list(p))
    for p in list(blocks):
        if 'industrial_iron_lamp'not in name(p):continue
        x,y,z=p;adj=[(x+1,y,z),(x-1,y,z),(x,y,z+1),(x,y,z-1)]
        structural=lambda q:name(q).endswith(('_bricks','_slab'))or name(q)in ['minecraft:stone_bricks','minecraft:cracked_stone_bricks']
        if not((structural(adj[0])and structural(adj[1]))or(structural(adj[2])and structural(adj[3]))):continue
        choices=[(x,y-1,z),(x,y-1,z-1),(x,y-1,z+1),(x-1,y-1,z),(x+1,y-1,z),(x,y,z-1),(x,y,z+1),(x-1,y,z),(x+1,y,z)]
        target=next((q for q in choices if q[1]>=1 and name(q)=='minecraft:air'and name((q[0],q[1]+1,q[2]))!='minecraft:air'),None)
        assert target is not None,('No safe exterior lamp position',p)
        lamp=deepcopy(state(p));lamp['Properties']=deepcopy(lamp['Properties']);lamp['Properties']['facing']=n.String('down')
        candidates=[state(q)for q in adj if structural(q)]
        material=Counter(str(s['Name'])for s in candidates).most_common(1)[0][0]
        replacement=next(deepcopy(s)for s in candidates if str(s['Name'])==material)
        if material.endswith('_slab'):
            replacement['Properties']=deepcopy(replacement['Properties']);replacement['Properties']['type']=n.String('double'if name((x,y+1,z))!='minecraft:air'else'bottom')
        put(p,replacement);put(target,lamp);lamps.append({'from':list(p),'to':list(target),'wall':material})
    return {'support_slabs':slabs,'relocated_lamps':lamps}


def remove_names_and_signs(blocks,palette):
    for pos in list(blocks):
        block=blocks[pos];name=str(palette[int(block["state"])]["Name"])
        if name.endswith(("_sign","_hanging_sign")) or name in {
                "supplementaries:notice_board","supplementaries:blackboard","kaleidoscope_tavern:chalkboard"}:
            del blocks[pos]
            continue
        if "nbt" in block:block["nbt"].pop("CustomName",None)


def complete_range_corner(blocks,palette,offset=(60,0,68)):
    state=n.Compound({"Name":n.String("minecraft:stone_bricks")})
    if state not in palette:palette.append(state)
    for y in range(2,6):
        pos=(offset[0]+5,offset[1]+y,offset[2]+4)
        blocks[pos]=n.Compound({"pos":n.List[n.Int](pos),"state":n.Int(palette.index(state))})
