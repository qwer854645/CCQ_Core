"""Import statues unchanged on the outside; hide six treasure blocks in sealed solids."""
from pathlib import Path
from copy import deepcopy
import json,hashlib
import nbtlib as n
ROOT=Path(__file__).resolve().parents[1]
DIRS=[(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]
def build():
    out=ROOT/'src/main/resources/data/ccq_testground/structure/statues';out.mkdir(parents=True,exist_ok=True);report={}
    for id in ['azure_maid_seated','azure_maid_standing']:
        source=ROOT/'tools/statue_sources'/(id+'.nbt');data=n.load(source);before=deepcopy(data)
        blocks={tuple(map(int,b['pos'])):b for b in data['blocks']};palette=data['palette'];size=list(map(int,data['size']))
        def solid(p):return p in blocks and not str(palette[int(blocks[p]['state'])]['Name']).endswith('trapdoor')
        candidates=[p for p in blocks if p[1]>2 and solid(p)and all(solid(tuple(p[i]+d[i]for i in range(3)))for d in DIRS)]
        # Prefer the enclosed body core, avoiding the pedestal and exposed clothes/hair.
        center=[(size[0]-1)/2,size[1]*.45,(size[2]-1)/2]
        candidates.sort(key=lambda p:(sum((p[i]-center[i])**2 for i in range(3)),p))
        assert len(candidates)>=6
        hidden=[]
        for p,material in zip(candidates[:6],['diamond_block']*4+['netherite_block']*2):
            state=n.Compound({'Name':n.String('minecraft:'+material)})
            if state not in palette:palette.append(state)
            blocks[p]['state']=n.Int(palette.index(state));hidden.append({'pos':list(p),'block':material})
        old={tuple(map(int,b['pos'])):b for b in before['blocks']}
        for p,b in blocks.items():
            if str(palette[int(b['state'])]['Name'])!=str(before['palette'][int(old[p]['state'])]['Name']):
                assert p in candidates[:6]and all(solid(tuple(p[i]+d[i]for i in range(3)))for d in DIRS)
        data.save(out/(id+'.nbt'));report[id]={'size':size,'hidden':hidden,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest()}
    (ROOT/'tools/statue_sources/manifest.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report))
if __name__=='__main__':build()
