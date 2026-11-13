"""Grow authored amethyst formations through the concrete ruin, including baked sections.

The main formation is taken from abandoned_facility, with smaller resampled
offshoots at the other damaged zones. Existing inventories and encounters stay.
"""
from pathlib import Path
from copy import deepcopy
from collections import Counter
import json,shutil
import nbtlib as n

ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'src/main/resources/data/ccq_testground/structure/engineering'
SNAPSHOTS=ROOT/'tools/engineering/ruined_crystal_sources_132'

def main():
    paths=[FOLDER/'engineering_base_ruined.nbt']+sorted((FOLDER/'ruined_base').glob('section_*.nbt'))
    for path in paths:
        backup=SNAPSHOTS/path.relative_to(FOLDER);backup.parent.mkdir(parents=True,exist_ok=True)
        if not backup.exists():shutil.copy2(path,backup)
    master=n.load(SNAPSHOTS/'engineering_base_ruined.nbt');palette=master['palette']
    blocks={tuple(map(int,b['pos'])):b for b in master['blocks']}
    def name(pos):return str(palette[int(blocks[pos]['state'])]['Name'])if pos in blocks else'minecraft:air'
    source=n.load(FOLDER/'abandoned_facility.nbt')
    crystal={tuple(map(int,b['pos'])):deepcopy(source['palette'][int(b['state'])])for b in source['blocks']
             if 'amethyst'in str(source['palette'][int(b['state'])]['Name'])}
    left=set(crystal);groups=[]
    while left:
        start=left.pop();group={start};queue=[start]
        for x,y,z in queue:
            for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:
                q=x+dx,y+dy,z+dz
                if q in left:left.remove(q);group.add(q);queue.append(q)
        groups.append(group)
    original=max(groups,key=len)
    protected=set()
    for p,b in blocks.items():
        tag=b.get('nbt',{});kind=str(tag.get('id',''))
        if tag and kind not in ['copycats:copycat','copycats:multistate_copycat']:
            for dx in range(-1,2):
                for dy in range(0,3):
                    for dz in range(-1,2):protected.add((p[0]+dx,p[1]+dy,p[2]+dz))
    for entity in master['entities']:
        x,y,z=map(int,entity['blockPos'])
        for dx in range(-2,3):
            for dy in range(-1,3):
                for dz in range(-2,3):protected.add((x+dx,y+dy,z+dz))
    allowed={'minecraft:air','minecraft:amethyst_block','minecraft:stone','minecraft:andesite','minecraft:polished_andesite',
             'minecraft:stone_bricks','minecraft:cracked_stone_bricks','minecraft:cobblestone','minecraft:gravel',
             'minecraft:tuff','minecraft:deepslate','minecraft:cobbled_deepslate','minecraft:blackstone',
             'minecraft:spruce_planks','minecraft:spruce_log','minecraft:stripped_dark_oak_log','minecraft:glass',
             'createdeco:dean_bricks','createdeco:cracked_dean_bricks','createdeco:industrial_iron_sheet_metal'}
    def replaceable(p):
        x,y,z=p;nm=name(p)
        return 1<=x<119 and 0<=y<42 and 1<=z<299 and not(43<=x<=62)and p not in protected and (
                nm in allowed or nm.endswith('_concrete')or nm.startswith('copycats:'))
    formations=[('primary_range',(84,4,80),(1.0,.82,1.0)),
                ('warehouse',(26,4,53),(.57,.60,.57)),
                ('maintenance',(25,7,97),(.45,.49,.45)),
                ('materials',(25,10,209),(.57,.57,.57))]
    added={};report=[]
    for label,(cx,cy,cz),(sx,sy,sz)in formations:
        count=0
        for x,y,z in sorted(original):
            state=crystal[x,y,z]
            if str(state['Name'])!='minecraft:amethyst_block':continue
            p=(cx+round((x-30)*sx),cy+round((y-1)*sy),cz+round((z-54)*sz))
            if replaceable(p):added[p]=deepcopy(state);count+=1
        report.append({'zone':label,'root':[cx,cy,cz],'scale':[sx,sy,sz],'source_voxels_placed':count})
    # Small crystals grow only on exposed, backed surfaces of the new formations.
    faces={'up':(0,1,0),'north':(0,0,-1),'south':(0,0,1),'east':(1,0,0),'west':(-1,0,0)}
    buds={}
    for (x,y,z),state in sorted(added.items()):
        code=(x*73856093)^(y*83492791)^(z*19349663)
        if code%23:continue
        facing=list(faces)[code%len(faces)];dx,dy,dz=faces[facing];p=x+dx,y+dy,z+dz
        if p in added or not replaceable(p)or name(p)!='minecraft:air':continue
        buds[p]=n.Compound({'Name':n.String('minecraft:amethyst_cluster'),'Properties':n.Compound({
            'facing':n.String(facing),'waterlogged':n.String('false')})})
    added.update(buds)
    for path in paths:
        relative=path.relative_to(FOLDER);data=n.load(SNAPSHOTS/relative);pal=data['palette']
        entries={tuple(map(int,b['pos'])):b for b in data['blocks']};indices={str(s):i for i,s in enumerate(pal)}
        section=None if path.parent==FOLDER else int(path.stem.split('_')[1])
        for (x,y,z),state in sorted(added.items()):
            if section is not None and z//100!=section:continue
            pos=(x,y,z if section is None else z-section*100)
            sig=str(state)
            if sig not in indices:indices[sig]=len(pal);pal.append(deepcopy(state))
            entries[pos]=n.Compound({'pos':n.List[n.Int](pos),'state':n.Int(indices[sig])})
        data['blocks']=n.List[n.Compound]([entries[p]for p in sorted(entries)]);data.save(path)
        print(relative,flush=True)
    manifest={'formations':report,'amethyst_blocks':len(added)-len(buds),'surface_clusters':len(buds),
              'source':'engineering/abandoned_facility','protected_main_corridor_x':[43,62],
              'preserved':'inventories, machines, encounters, damage, baked rotation states'}
    (ROOT/'tools/engineering/ruined_crystals_132.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(manifest),flush=True)

if __name__=='__main__':main()
