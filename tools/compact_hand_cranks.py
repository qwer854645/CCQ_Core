"""Remove a single redundant shaft between a crank and its machine interface.

Long transmission lines remain authored functional runs. A one-block adapter is
compacted only when the machine beyond exposes a shaft on that exact axis.
"""
from pathlib import Path
from copy import deepcopy
import json
import nbtlib as n
ROOT=Path(__file__).resolve().parents[1]
BACK={'north':(0,0,1),'south':(0,0,-1),'west':(1,0,0),'east':(-1,0,0),'up':(0,-1,0),'down':(0,1,0)}
def compact(data):
    palette=data['palette'];blocks={tuple(map(int,b['pos'])):b for b in data['blocks']};changes=[]
    def state(p):return palette[int(blocks[p]['state'])]if p in blocks else {'Name':'minecraft:air'}
    air=n.Compound({'Name':n.String('minecraft:air')})
    if air not in palette:palette.append(air)
    for p,b in list(blocks.items()):
        s=state(p)
        if str(s['Name'])!='create:hand_crank':continue
        facing=str(s.get('Properties',{}).get('facing','north'));delta=BACK[facing]
        axis='x'if delta[0]else'y'if delta[1]else'z'
        q=tuple(p[i]+delta[i]for i in range(3));r=tuple(p[i]+2*delta[i]for i in range(3))
        shaft=state(q);machine=state(r);props=machine.get('Properties',{});name=str(machine['Name'])
        if str(shaft['Name'])!='create:shaft'or str(shaft.get('Properties',{}).get('axis'))!=axis:continue
        connected=(name in ['create:cogwheel','create:large_cogwheel','create:clutch','create:gearshift','create:encased_chain_drive']and str(props.get('axis','y'))==axis)
        if name=='create:mechanical_press':
            connected=('x'if str(props.get('facing','north'))in ['east','west']else'z')==axis
        connected |= name=='create:gearbox'and str(props.get('axis','y'))!=axis
        connected |= name=='create:rotation_speed_controller'and str(props.get('axis','x'))==axis
        if not connected:continue
        moved=deepcopy(b);moved['pos']=n.List[n.Int](q);blocks[q]=moved
        blocks[p]=n.Compound({'pos':n.List[n.Int](p),'state':n.Int(palette.index(air))})
        changes.append({'from':list(p),'to':list(q),'machine':name})
    data['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)])
    return changes

def main():
    folder=ROOT/'src/main/resources/data/ccq_testground/structure/engineering';report={}
    for path in sorted(folder.rglob('*.nbt')):
        data=n.load(path);changes=compact(data)
        if changes:data.save(path);report[str(path.relative_to(folder))]=changes
    # A crank and its one-block adapter may straddle a 100-block section seam.
    # Reconcile from the complete master rather than treating a missing neighbor
    # in a local section as a missing machine.
    for master_name,subfolder in [('engineering_base','base'),('engineering_base_ruined','ruined_base')]:
        master=n.load(folder/(master_name+'.nbt'))
        states={tuple(map(int,b['pos'])):master['palette'][int(b['state'])]for b in master['blocks']}
        for path in sorted((folder/subfolder).glob('section_*.nbt')):
            index=int(path.stem.split('_')[1]);data=n.load(path);palette=data['palette']
            blocks={tuple(map(int,b['pos'])):b for b in data['blocks']};count=0
            for p,s in states.items():
                if str(s['Name'])!='create:hand_crank':continue
                facing=str(s['Properties']['facing']);back=BACK[facing]
                old=tuple(p[i]-back[i]for i in range(3))
                if str(states.get(old,{}).get('Name'))!='minecraft:air':continue
                if not index*100<=old[2]<(index+1)*100:continue
                local=(old[0],old[1],old[2]-index*100)
                current=blocks.get(local)
                if current is None or str(palette[int(current['state'])]['Name'])!='create:hand_crank':continue
                for q,desired in [(old,states[old]),(p,s)]:
                    if not index*100<=q[2]<(index+1)*100:continue
                    coord=(q[0],q[1],q[2]-index*100)
                    if desired not in palette:palette.append(deepcopy(desired))
                    blocks[coord]=n.Compound({'pos':n.List[n.Int](coord),'state':n.Int(palette.index(desired))})
                count+=1
            if count:
                data['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)]);data.save(path)
                report[str(path.relative_to(folder))]=[{'cross_section_master_reconciliations':count}]
    (ROOT/'tools/engineering/compact_cranks_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:len(v)for k,v in report.items()}),flush=True)
if __name__=='__main__':main()
