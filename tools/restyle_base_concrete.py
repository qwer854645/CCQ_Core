"""Apply concrete finishes to intact/ruined bases and baked orientation sections.

Source snapshots make this finishing pass repeatable without changing geometry,
inventories, machinery states, authored connections or existing ruin damage.
"""
from pathlib import Path
from copy import deepcopy
from collections import Counter
import json,shutil
import nbtlib as n

ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'src/main/resources/data/ccq_testground/structure/engineering'
BACKUP=ROOT/'tools/engineering/concrete_sources_132'

def main():
    files=[FOLDER/'engineering_base.nbt',FOLDER/'engineering_base_ruined.nbt',FOLDER/'duty_lodge.nbt']
    files+=sorted((FOLDER/'base').glob('section_*.nbt'))+sorted((FOLDER/'ruined_base').glob('section_*.nbt'))
    report={}
    for path in files:
        relative=path.relative_to(FOLDER);snapshot=BACKUP/relative
        snapshot.parent.mkdir(parents=True,exist_ok=True)
        if not snapshot.exists():shutil.copy2(path,snapshot)
        data=n.load(snapshot);palette=data['palette'];old=deepcopy(palette)
        existing={tuple(map(int,b['pos'])):str(old[int(b['state'])]['Name'])for b in data['blocks']}
        ruined='ruined'in str(relative);ground_offset=12 if ruined else 0
        lodge=path.stem=='duty_lodge';offset=(29,0,14)if lodge else(0,0,0)
        if path.parent.name in ['base','ruined_base']:
            offset=(0,0,int(path.stem.split('_')[1])*100)
        indices={str(s):i for i,s in enumerate(palette)};changes=Counter()
        for b in data['blocks']:
            x,yy,z=map(int,b['pos']);x+=offset[0];z+=offset[2];y=yy-ground_offset
            s=old[int(b['state'])];name=str(s['Name']);props=deepcopy(s.get('Properties',n.Compound()))
            material=None;shape=None
            if 'nbt'in b:continue
            if name=='minecraft:andesite'and y==0:material='gray_concrete'
            elif name=='minecraft:polished_andesite':
                material='gray_concrete'if y<=1 and 46<=x<=60 else'light_gray_concrete'if y<=1 else'white_concrete'
            elif name in ['minecraft:stone_bricks','minecraft:cracked_stone_bricks','createdeco:dean_bricks']:
                material='gray_concrete'if y<=2 else'white_concrete'if y in [5,7]else'light_gray_concrete'
            elif name=='minecraft:calcite':material='light_gray_concrete'
            elif name in ['minecraft:stripped_dark_oak_log','minecraft:spruce_log']:material='white_concrete'
            elif name.endswith('_cut_copper_slab')or name=='minecraft:cut_copper_slab':
                xx,ly,zz=map(int,b['pos'])
                edge=any('cut_copper_slab'not in existing.get((xx+dx,ly,zz+dz),'')for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)])
                material='white_concrete'if edge else'gray_concrete';shape='slab'
            elif name in ['minecraft:stone_slab','minecraft:smooth_stone_slab','minecraft:polished_andesite_slab','minecraft:stone_brick_slab']:
                material='light_gray_concrete';shape='slab'
            elif name in ['minecraft:stone_brick_stairs','minecraft:polished_andesite_stairs']:
                material='white_concrete';shape='stairs'
            if material is None:continue
            mat=n.Compound({'Name':n.String('minecraft:'+material)})
            if shape=='slab'and str(props.get('type'))=='double':shape=None
            if shape=='slab':props['axis']=n.String('y')
            target=mat if shape is None else n.Compound({'Name':n.String('copycats:copycat_'+shape),'Properties':props})
            sig=str(target)
            if sig not in indices:indices[sig]=len(palette);palette.append(target)
            b['state']=n.Int(indices[sig])
            if shape:
                b['nbt']=n.Compound({'id':n.String('copycats:copycat'),'Material':deepcopy(mat),'EnableCT':n.Byte(1),
                    'Item':n.Compound({'id':n.String('minecraft:'+material),'count':n.Int(1)})})
                if shape=='slab':
                    half=n.Compound({'enableCT':n.Byte(1),'material':deepcopy(mat),
                        'consumedItem':n.Compound({'id':n.String('minecraft:'+material),'count':n.Int(1)})})
                    b['nbt']=n.Compound({'id':n.String('copycats:multistate_copycat'),
                        'material_data':n.Compound({'top':deepcopy(half),'bottom':deepcopy(half)})})
            changes[name]+=1
        data.save(path);report[str(relative)]={'changed':sum(changes.values()),'materials':dict(changes)}
        print(relative,sum(changes.values()),flush=True)
    (ROOT/'tools/engineering/concrete_style_132.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':main()
