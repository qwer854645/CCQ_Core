"""Repair detached furnishing/structural pieces without changing native cannon mounts."""
from pathlib import Path
from copy import deepcopy
from collections import Counter
import json,shutil
import nbtlib as n

ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'src/main/resources/data/ccq_testground/structure/engineering'
BACKUP=ROOT/'tools/engineering/floating_sources_133'
AIR={'minecraft:air','minecraft:cave_air','minecraft:structure_void','minecraft:water'}
DIRS=[(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]

def detached(blocks,palette):
    names={p:str(palette[int(b['state'])]['Name'])for p,b in blocks.items()}
    left={p for p,nm in names.items()if nm not in AIR};groups=[]
    while left:
        root=left.pop();queue=[root];group={root};ground=root[1]<=1
        for x,y,z in queue:
            neighbors=[(x+dx,y+dy,z+dz)for dx,dy,dz in DIRS]
            # CBC's native mounting point is two blocks above the mount block.
            if names.get((x,y,z))=='createbigcannons:cannon_mount':neighbors.append((x,y+2,z))
            if names.get((x,y-2,z))=='createbigcannons:cannon_mount':neighbors.append((x,y-2,z))
            for q in neighbors:
                if q in left:left.remove(q);queue.append(q);group.add(q);ground|=q[1]<=1
        if not ground:groups.append(group)
    return groups

def main():
    paths=[FOLDER/'engineering_base.nbt',FOLDER/'engineering_base_ruined.nbt',FOLDER/'duty_lodge.nbt']
    paths+=sorted((FOLDER/'base').glob('section_*.nbt'))+sorted((FOLDER/'ruined_base').glob('section_*.nbt'))
    for path in paths:
        snapshot=BACKUP/path.relative_to(FOLDER);snapshot.parent.mkdir(parents=True,exist_ok=True)
        if not snapshot.exists():shutil.copy2(path,snapshot)
    results={};changes={}
    for key in ['engineering_base','engineering_base_ruined']:
        data=n.load(BACKUP/(key+'.nbt'));pal=data['palette'];blocks={tuple(map(int,b['pos'])):b for b in data['blocks']}
        old=deepcopy(blocks);ruined=key.endswith('ruined');shift=12 if ruined else 0
        indices={str(s):i for i,s in enumerate(pal)}
        def nm(p):return str(pal[int(blocks[p]['state'])]['Name'])if p in blocks else'minecraft:air'
        def put(p,name,**props):
            if ':'not in name:name='minecraft:'+name
            state=n.Compound({'Name':n.String(name)})
            if props:state['Properties']=n.Compound({k:n.String(str(v).lower())for k,v in props.items()})
            sig=str(state)
            if sig not in indices:indices[sig]=len(pal);pal.append(state)
            blocks[p]=n.Compound({'pos':n.List[n.Int](p),'state':n.Int(indices[sig])})
        before=detached(blocks,pal)
        # Put short stringers beneath the existing wooden stair flights.
        for x in [31,32]:
            for y,z in [(3,26),(4,25),(5,24)]:
                p=x,y+shift-1,z
                if nm((x,y+shift,z))=='minecraft:spruce_stairs'and nm(p)in AIR:put(p,'spruce_planks')
        # Step-like steel blocks were joined only along edges; add the missing risers.
        for group in sorted(detached(blocks,pal),key=lambda g:min(p[1]for p in g)):
            for x,y,z in sorted(group):
                if nm((x,y,z))!='create:industrial_iron_block':continue
                p=x,y-1,z
                if nm(p)in AIR and any(nm((p[0]+dx,p[1]+dy,p[2]+dz))not in AIR
                        for dx,dy,dz in DIRS if (dx,dy,dz)!=(0,1,0)):
                    put(p,'create:industrial_iron_block')
        # Raised cabinets, cutters and lamp uprights receive a one-block plinth.
        for x,z in [(12,82),(12,88),(38,82),(38,106),(37,85),(37,109),(39,85),(39,101),(39,109)]:
            p=x,4+shift,z
            if nm((x,5+shift,z))not in AIR and nm(p)in AIR and nm((x,3+shift,z))not in AIR:put(p,'white_concrete')
        # Support both ends of the long cantilevered workshop awnings.
        for x in [97,101,106,110]:
            if nm((x,4+shift,34))=='createdeco:industrial_iron_sheet_metal':
                for y in [2,3]:
                    if nm((x,y+shift,34))in AIR:put((x,y+shift,34),'white_concrete')
        for z in [48,51,57,60]:
            if nm((89,4+shift,z))=='createdeco:industrial_iron_sheet_metal':
                for y in [2,3]:
                    if nm((89,y+shift,z))in AIR:put((89,y+shift,z),'white_concrete')
        # Hang the front work lights directly below their original supports.
        for x in [90,99,108]:
            oldp=x,6+shift,34;newp=x,5+shift,34
            if nm(oldp)=='createdeco:yellow_industrial_iron_lamp'and nm(newp)in AIR:
                if nm((x,4+shift,34))not in AIR:
                    blocks[newp]=deepcopy(blocks[oldp]);blocks[newp]['pos']=n.List[n.Int](newp);put(oldp,'air')
        # Complete half-slab attachment faces where a block rests above the lower half.
        half_fixes=0
        for p,b in list(blocks.items()):
            s=pal[int(b['state'])];props=s.get('Properties',{})
            if str(s['Name'])!='copycats:copycat_slab'or str(props.get('type'))!='bottom':continue
            above=(p[0],p[1]+1,p[2])
            if nm(above)in AIR:continue
            material=b.get('nbt',{}).get('material_data',{}).get('bottom',{}).get('material',{})
            if 'Name'in material:put(p,str(material['Name']));half_fixes+=1
        remaining=detached(blocks,pal)
        removed=[]
        if ruined:
            for group in remaining:
                # Detached debris is removed; never discard inventory or machinery.
                safe=all(nm(p)in {'minecraft:gray_concrete','minecraft:light_gray_concrete','minecraft:white_concrete',
                    'createdeco:cracked_dean_bricks','createdeco:industrial_iron_sheet_metal','create:industrial_iron_block'}
                    and 'nbt'not in blocks[p]for p in group)
                if safe:
                    for p in group:removed.append(list(p));put(p,'air')
        remaining=detached(blocks,pal)
        if remaining:raise RuntimeError((key,'Unresolved detached pieces',[[list(p)for p in sorted(g)]for g in remaining]))
        edits={p:deepcopy(b)for p,b in blocks.items()if p not in old or b!=old[p]}
        changes[key]=(edits,pal);data['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)]);data.save(FOLDER/(key+'.nbt'))
        results[key]={'detached_groups_before':len(before),'detached_cells_before':sum(map(len,before)),
            'changed_positions':len(edits),'removed_debris':removed,'half_slab_attachments':half_fixes,'detached_groups_after':len(remaining)}
        print(key,results[key],flush=True)
    for path in paths:
        if path.name in ['engineering_base.nbt','engineering_base_ruined.nbt']:continue
        relative=path.relative_to(FOLDER);data=n.load(BACKUP/relative);pal=data['palette'];entries={tuple(map(int,b['pos'])):b for b in data['blocks']}
        lodge=path.name=='duty_lodge.nbt';ruined=path.parent.name=='ruined_base'
        edits,source_palette=changes['engineering_base_ruined'if ruined else'engineering_base']
        index=None if lodge else int(path.stem.split('_')[1]);indices={str(s):i for i,s in enumerate(pal)}
        for (x,y,z),b in edits.items():
            if lodge:
                if not(29<=x<47 and 0<=y<17 and 14<=z<33):continue
                p=x-29,y,z-14
            else:
                if z//100!=index:continue
                p=x,y,z-index*100
            state=source_palette[int(b['state'])];sig=str(state)
            if sig not in indices:indices[sig]=len(pal);pal.append(deepcopy(state))
            block=deepcopy(b);block['pos']=n.List[n.Int](p);block['state']=n.Int(indices[sig]);entries[p]=block
        data['blocks']=n.List[n.Compound]([entries[p]for p in sorted(entries)]);data.save(path)
    (ROOT/'tools/engineering/floating_fixes_133.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':main()
