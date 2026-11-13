"""Group existing storage locally without rebuilding facilities or their damaged variants.

Run after authoring/baking: edits masters and every already-baked section identically.
Whole block NBT is moved, preserving fixed stock, loot tables and native connection states.
"""
from pathlib import Path
from copy import deepcopy
from collections import Counter
import json
import nbtlib as n
ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'src/main/resources/data/ccq_testground/structure/engineering'
STORAGE={'farmersdelight:spruce_cabinet','minecraft:barrel','minecraft:chest'}
SAFE={'minecraft:air','createdeco:industrial_iron_sheet_metal'}
# Each row belongs to one room or workstation; categorized warehouse racks stay intact.
ROWS=[('canteen',[(80,2,z)for z in [23,26,28]]),
 ('west_workshop',[(90,2,z)for z in [49,52,57]]),
 ('east_workshop',[(111,2,z)for z in [48,51,54,57,60]]),
 ('workshop_rear',[(x,2,60)for x in [96,99,102,105,108]]),
 ('warehouse_counter',[(x,2,55)for x in [25,28,29]]),
 ('waterworks',[(39,2,z)for z in [233,236,239,242,245,246]]),
 ('rear_office',[(41,2,z)for z in [267,270,273,276,278,279]]),
 ('rear_stores',[(x,2,281)for x in [9,12,15,18]]),
 *[(f'range_{z}',[(59,2,z+2),(59,2,z+5)])for z in [121,181,241]]]

def inventory(data):
    return Counter(str(b.get('nbt',{})) for b in data['blocks'] if str(data['palette'][int(b['state'])]['Name']) in STORAGE)

def apply(data, plans, offset=(0,0,0)):
    palette=data['palette'];blocks={tuple(map(int,b['pos'])):b for b in data['blocks']};report=[]
    def local(p):return tuple(p[i]-offset[i]for i in range(3))
    def name(p):return str(palette[int(blocks[p]['state'])]['Name'])if p in blocks else'minecraft:air'
    before=inventory(data)
    def setblock(p,b):
        if b is None:blocks.pop(p,None)
        else:blocks[p]=deepcopy(b);blocks[p]['pos']=n.List[n.Int](p)
    for label,sources,targets in plans:
        sources=list(map(local,sources));targets=list(map(local,targets))
        assert len(sources)==len(targets) and len(set(targets))==len(targets)
        if not all(name(p)in STORAGE for p in sources):continue
        changed=set(sources)^set(targets)
        if not all(name(p)in SAFE for p in set(targets)-set(sources)):continue
        original={p:deepcopy(blocks.get(p))for p in set(sources)|set(targets)}
        displaced=[original[p]for p in sorted(set(targets)-set(sources))]
        for p,b in zip(sorted(set(sources)-set(targets)),displaced):setblock(p,b)
        for src,dst in zip(sources,targets):setblock(dst,original[src])
        report.append({'row':label,'moves':[[list(a),list(b)]for a,b in zip(sources,targets)if a!=b]})
    data['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)])
    assert before==inventory(data),'Storage or contents changed'
    return report

def make_plans(data,dy=0):
    blocks={tuple(map(int,b['pos'])):b for b in data['blocks']};palette=data['palette']
    def name(p):return str(palette[int(blocks[p]['state'])]['Name'])if p in blocks else'minecraft:air'
    plans=[]
    for label,row in ROWS:
        row=[(x,y+dy,z)for x,y,z in row]
        axis=0 if row[0][0]!=row[-1][0]else 2
        candidates=[]
        for v in range(row[0][axis],row[-1][axis]+1):
            q=list(row[0]);q[axis]=v;candidates.append(tuple(q))
        sources=[p for p in candidates if name(p)in STORAGE]
        if len(sources)<2:continue
        for start in range(min(p[axis]for p in row),max(p[axis]for p in row)-len(sources)+2):
            targets=[]
            for i in range(len(sources)):
                q=list(row[0]);q[axis]=start+i;targets.append(tuple(q))
            if not all(name(p)in SAFE|STORAGE and (name(p)not in STORAGE or p in sources)for p in targets):continue
            if not all(name((p[0],p[1]-1,p[2]))not in {'minecraft:air','minecraft:water','minecraft:lava'} for p in targets):continue
            # Never leave a workpiece/decor floating when replacing its cabinet with air.
            empty=[p for p in targets if p not in sources and name(p)=='minecraft:air']
            if empty and any(name((p[0],p[1]+1,p[2]))!='minecraft:air' for p in sources if p not in targets):continue
            plans.append((label,sources,targets));break
    return plans

def group_kitchen(data,offset=(0,0,0),dy=0):
    blocks={tuple(map(int,b['pos'])):b for b in data['blocks']};palette=data['palette']
    def p(x,y):return (x-offset[0],y+dy-offset[1],15-offset[2])
    def name(q):return str(palette[int(blocks[q]['state'])]['Name'])if q in blocks else 'minecraft:air'
    if name(p(41,2))!='farmersdelight:spruce_cabinet' or name(p(38,2))!='minecraft:water_cauldron':return False
    before=inventory(data)
    for y in [2,3]:
        a,b=p(38,y),p(41,y);A,B=deepcopy(blocks.get(a)),deepcopy(blocks.get(b))
        for q,block in [(a,B),(b,A)]:
            if block is None:blocks.pop(q,None)
            else:block['pos']=n.List[n.Int](q);blocks[q]=block
    data['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)])
    assert before==inventory(data)
    return True

def main():
    result={}
    for master,folder,dy in [('engineering_base','base',0),('engineering_base_ruined','ruined_base',12)]:
        path=FOLDER/(master+'.nbt');data=n.load(path);plans=make_plans(data,dy)
        report=apply(data,plans)
        if group_kitchen(data,dy=dy):report.append({'row':'lodge_kitchen','moves':[[[41,2+dy,15],[38,2+dy,15]]]})
        data.save(path);result[master]=report
        for part in sorted((FOLDER/folder).glob('section_*.nbt')):
            index=int(part.stem.split('_')[1]);section=n.load(part)
            relevant=[p for p in plans if all(index*100<=v[2]<(index+1)*100 for v in p[1]+p[2])]
            applied=apply(section,relevant,(0,0,index*100));assert len(applied)==len(relevant),(part,len(applied),len(relevant));
            if index==0:group_kitchen(section,dy=dy)
            section.save(part)
    lodge=n.load(FOLDER/'duty_lodge.nbt');group_kitchen(lodge,(29,0,14));lodge.save(FOLDER/'duty_lodge.nbt')
    (ROOT/'tools/engineering/storage_groups.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:{'rows':len(v),'moved':sum(len(row['moves'])for row in v)}for k,v in result.items()}))
if __name__=='__main__':main()
