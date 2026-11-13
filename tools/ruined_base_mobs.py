"""Author sparse, one-time vanilla encounters in traversable ruin areas."""
from pathlib import Path
from copy import deepcopy
import json, math
import nbtlib as n
ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'src/main/resources/data/ccq_testground/structure/engineering'

def partition_entities(entities,index):
    result=[]
    for entity in entities:
        if index*100<=float(entity['pos'][2])<(index+1)*100:
            entry=deepcopy(entity)
            entry['pos'][2]=n.Double(float(entry['pos'][2])-index*100)
            entry['blockPos'][2]=n.Int(int(entry['blockPos'][2])-index*100)
            result.append(entry)
    return n.List[n.Compound](result)

def populate(data):
    palette=data['palette'];blocks={tuple(map(int,b['pos'])):b for b in data['blocks']}
    def state(p):return palette[int(blocks[p]['state'])]if p in blocks else None
    def name(p):return str(state(p)['Name'])if state(p)is not None else'minecraft:air'
    def sturdy(p):
        s=state(p);nm=name(p)
        return nm in {'minecraft:andesite','minecraft:polished_andesite','minecraft:stone','minecraft:stone_bricks',
                      'minecraft:cracked_stone_bricks','minecraft:gray_concrete','minecraft:light_gray_concrete',
                      'minecraft:cobbled_deepslate','minecraft:deepslate','minecraft:tuff','minecraft:gravel',
                      'minecraft:spruce_planks','createdeco:dean_bricks','createdeco:cracked_dean_bricks'}
    # Bounds, floor range and desired anchors keep encounters in distinct rooms
    # and on crater rims, rather than concentrating the whole group in a pit.
    zones=[('lodge',(29,46,14,32),(13,18),[(32,24,'zombie'),(42,24,'skeleton')]),
           ('forecourt',(61,113,11,33),(13,13),[(66,16,'zombie'),(78,27,'skeleton'),(92,16,'zombie'),(108,27,'spider')]),
           ('warehouse',(5,44,40,69),(10,13),[(8,62,'zombie'),(41,63,'skeleton')]),
           ('waterworks',(6,43,230,252),(13,13),[(9,248,'zombie'),(36,247,'zombie'),(31,235,'spider')]),
           ('rear',(5,44,263,290),(10,13),[(8,281,'zombie'),(40,280,'skeleton'),(32,287,'spider')]),
           ('loading',(5,45,125,183),(10,13),[(10,130,'zombie'),(40,180,'skeleton')])]
    occupied=[];entities=[];report=[]
    for zone,(x0,x1,z0,z1),(y0,y1),anchors in zones:
        for tx,tz,kind in anchors:
            candidates=[]
            for x in range(x0,x1+1):
                for z in range(z0,z1+1):
                    for floor in range(y0,y1+1):
                        if any((x-a)**2+(z-c)**2<25 and abs(floor+1-b)<4 for a,b,c in occupied):continue
                        radius=1 if kind=='spider'else 0
                        if not all(sturdy((x+dx,floor,z+dz))and all(name((x+dx,y,z+dz))=='minecraft:air'for y in [floor+1,floor+2])
                                   for dx in range(-radius,radius+1)for dz in range(-radius,radius+1)):continue
                        covered=any(name((x,y,z))!='minecraft:air'for y in range(floor+3,min(42,floor+12)))
                        candidates.append(((x-tx)**2+(z-tz)**2+(0 if covered else 25),(x,floor+1,z)))
            assert candidates,('No safe encounter position',zone,kind)
            _,p=min(candidates);x,y,z=p;occupied.append(p)
            health={'zombie':40,'skeleton':36,'spider':28}[kind]
            tag=n.Compound({'id':n.String('minecraft:'+kind),'PersistenceRequired':n.Byte(1),'Health':n.Float(health),
                            'attributes':n.List[n.Compound]([n.Compound({'id':n.String('minecraft:generic.max_health'),'base':n.Double(health)})]),
                            'Rotation':n.List[n.Float]([n.Float((x*37+z*11)%360),n.Float(0)])})
            if kind!='spider':
                armor=['iron_boots','chainmail_leggings','iron_chestplate'if kind=='zombie'else'chainmail_chestplate','iron_helmet']
                tag['ArmorItems']=n.List[n.Compound]([n.Compound({'id':n.String('minecraft:'+item),'count':n.Int(1)})for item in armor])
                weapon='bow'if kind=='skeleton'else'iron_axe'if sum(row['type']=='zombie'for row in report)%3==0 else'iron_sword'
                tag['HandItems']=n.List[n.Compound]([n.Compound({'id':n.String('minecraft:'+weapon),'count':n.Int(1)}),n.Compound()])
                tag['ArmorDropChances']=n.List[n.Float]([.085,.085,.085,.085])
                tag['HandDropChances']=n.List[n.Float]([.085,0])
            entities.append(n.Compound({'pos':n.List[n.Double]([x+.5,float(y),z+.5]),'blockPos':n.List[n.Int](p),'nbt':tag}))
            report.append({'zone':zone,'type':kind,'health':health,'pos':list(p)})
    assert len(entities)==16
    data['entities']=n.List[n.Compound](entities)
    return report

def main():
    path=FOLDER/'engineering_base_ruined.nbt';data=n.load(path);report=populate(data);data.save(path)
    for index in range(3):
        for p in (FOLDER/'ruined_base').glob(f'section_{index}*.nbt'):
            part=n.load(p);part['entities']=partition_entities(data['entities'],index);part.save(p)
    target=ROOT/'tools/engineering/ruined_encounters.json';target.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print('Authored',len(report),'encounters:',{kind:sum(r['type']==kind for r in report)for kind in ['zombie','skeleton','spider']},flush=True)
if __name__=='__main__':main()
