"""Assign native-container loot markers and author six reusable loot tables.

The facility structure processor rolls these tables at placement time using the
world seed and position; player visits and chunk reloads do not refill inventory.
"""
from pathlib import Path
from collections import Counter
import json
import nbtlib as n

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/ccq_testground'

def entry(item,low=1,high=1,weight=1):
    if ':'not in item:item='minecraft:'+item
    return {'type':'minecraft:item','name':item,'weight':weight,'functions':[
        {'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':low,'max':high}}]}

def pool(low,high,entries,chance=None):
    result={'rolls':{'type':'minecraft:uniform','min':low,'max':high},'entries':entries}
    if chance is not None:result['conditions']=[{'condition':'minecraft:random_chance','chance':chance}]
    return result

def main():
    tools=[entry('create:wrench'),entry('iron_pickaxe'),entry('shears'),entry('create:goggles')]
    tables={
        'wild_field_supplies':[
            pool(5,8,[entry('bread',2,5,5),entry('cooked_beef',1,3,3),entry('torch',8,20,5),entry('iron_ingot',3,8,5),
                entry('copper_ingot',4,12,5),entry('create:andesite_alloy',2,6,4),entry('create:shaft',2,6,3),
                entry('redstone',3,8,4),entry('amethyst_shard',4,12,5),entry('quartz',2,6,3)]),
            pool(1,1,tools,0.40),pool(1,1,[entry('diamond',1,2),entry('emerald',2,4)],0.10)],
        'wild_research_cargo':[
            pool(3,5,[entry('amethyst_shard',6,16,6),entry('quartz',4,10,5),entry('redstone',4,12,5),
                entry('create:copper_sheet',2,6,5),entry('create:iron_sheet',2,5,4),entry('create:electron_tube',1,3,3),
                entry('glass',4,12,3),entry('create:brass_ingot',2,5,3)]),
            pool(1,1,[entry('create:precision_mechanism'),entry('diamond'),entry('gold_ingot',2,4)],0.18)],
        'abandoned_salvage':[
            pool(2,4,[entry('iron_nugget',6,24,8),entry('copper_ingot',1,4,7),entry('iron_ingot',1,3,5),
                entry('create:andesite_alloy',1,3,4),entry('create:shaft',1,3,4),entry('create:cogwheel',1,2,3),
                entry('string',1,4,4),entry('coal',1,4,4),entry('redstone',1,4,4),entry('flint',1,3,3)]),
            pool(1,1,tools,0.04),pool(1,1,[entry('diamond'),entry('emerald',1,2)],0.025)],
        'abandoned_electronics':[
            pool(2,4,[entry('redstone',2,6,7),entry('quartz',1,4,5),entry('create:copper_sheet',1,3,5),
                entry('create:iron_sheet',1,3,5),entry('iron_nugget',4,12,6),entry('glass_pane',2,6,4),
                entry('paper',2,6,4),entry('create:electron_tube',1,1,2),entry('comparator',1,1,1)]),
            pool(1,1,[entry('create:precision_mechanism'),entry('diamond'),entry('gold_ingot',1,2)],0.035)],
        'abandoned_research':[
            pool(2,4,[entry('amethyst_shard',2,8,7),entry('quartz',2,6,6),entry('redstone',2,6,5),entry('glass_bottle',1,3,4),
                entry('book',1,2,3),entry('create:electron_tube',1,2,2),entry('lapis_lazuli',2,5,4)]),
            pool(1,1,[entry('diamond',1,2),entry('create:precision_mechanism'),entry('gold_ingot',1,3)],0.06)],
        'abandoned_metals':[
            pool(5,8,[entry('iron_ingot',4,12,6),entry('copper_ingot',6,16,6),entry('create:zinc_ingot',3,8,4),
                entry('create:brass_ingot',2,6,3),entry('create:iron_sheet',2,6,4),entry('create:andesite_alloy',3,8,4)]),
            pool(1,1,[entry('gold_ingot',2,5),entry('diamond',1,2)],0.25)]
    }
    dest=DATA/'loot_table/chests/facilities';dest.mkdir(parents=True,exist_ok=True)
    for key,pools in tables.items():
        (dest/(key+'.json')).write_text(json.dumps({'type':'minecraft:chest','pools':pools},indent=2)+'\n',encoding='utf-8')
    manifest={}
    for key in ['wild_amethyst_facility','abandoned_facility']:
        path=DATA/'structure/engineering'/(key+'.nbt')
        template=n.load(path);wild=key.startswith('wild_');counts=Counter();positions=[]
        for block in template['blocks']:
            name=str(template['palette'][int(block['state'])]['Name']);tag=block.get('nbt')
            if tag is None:continue
            table=None
            if name=='fxntstorage:storage_box':table='wild_field_supplies'
            elif name=='createdeco:white_shipping_container':
                table='wild_research_cargo'if wild else('abandoned_electronics'if int(block['pos'][0])>=36 else'abandoned_salvage')
            elif name=='createdeco:light_gray_shipping_container':table='abandoned_research'
            elif name=='create:item_vault':table='abandoned_metals'
            if table:
                tag['ccq_facility_loot']=n.String('ccq_testground:chests/facilities/'+table)
                counts[table]+=1;positions.append({'pos':list(map(int,block['pos'])),'table':table})
            elif name=='createdieselgenerators:canister':
                tag['ccq_facility_fuel']=n.String('field'if wild else'salvage');counts['fuel_canisters']+=1
        template.save(path)
        manifest[key]={'counts':dict(counts),'containers':positions}
        print(key,dict(counts))
    (ROOT/'tools/facility_loot_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':main()
