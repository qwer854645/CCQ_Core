"""Export four native-baked ruin rotations and three chunk-safe sections each."""
from pathlib import Path
from copy import deepcopy
import json
import nbtlib as n
from ruined_base_mobs import partition_entities
ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'src/main/resources/data/ccq_testground/structure/engineering'
def main():
    source=n.load(FOLDER/'engineering_base_ruined.nbt')
    baked=json.loads((ROOT/'tools/engineering/ruined_connection_states.json').read_text(encoding='utf-8'))
    for rotation,entries in baked['variants'].items():
        data=deepcopy(source);palette=data['palette'];blocks={tuple(map(int,b['pos'])):b for b in data['blocks']}
        for entry in entries:
            p=tuple(entry['pos']);state=baked['palette'][entry['state']]
            tag=n.Compound({'Name':n.String(state['Name'])})
            if state.get('Properties'):tag['Properties']=n.Compound({k:n.String(v)for k,v in state['Properties'].items()})
            if tag not in palette:palette.append(tag)
            block=blocks[p];block['state']=n.Int(palette.index(tag))
            if state['Name']=='minecraft:air':block.pop('nbt',None)
        if rotation=='NONE':data.save(FOLDER/'engineering_base_ruined.nbt')
        count=0
        for index in range(3):
            part=deepcopy(data);part['size']=n.List[n.Int]([120,42,100]);subset=[]
            for b in data['blocks']:
                x,y,z=map(int,b['pos'])
                if index*100<=z<(index+1)*100:
                    local=deepcopy(b);local['pos']=n.List[n.Int]([x,y,z-index*100]);subset.append(local);count+=1
            part['blocks']=n.List[n.Compound](subset)
            part['entities']=partition_entities(data['entities'],index)
            suffix=''if rotation=='NONE'else'_'+rotation.lower()
            part.save(FOLDER/f'ruined_base/section_{index}{suffix}.nbt')
        assert count==len(data['blocks'])
        print(rotation,'exported',count,'entries',flush=True)
if __name__=='__main__':main()
