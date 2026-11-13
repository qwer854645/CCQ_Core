"""Observation station loot: real installed Create Armorer guns and matching ammo."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
guns=[('sniper_semi_clockwork','SEMI',2),('rifle_assult_crane','AUTO',3),('pistol_auto_stress','SEMI',1)]
entries=[]
for gun,mode,weight in guns:
    entries.append({'type':'minecraft:item','name':'tacz:modern_kinetic_gun','weight':weight,
        'functions':[{'function':'minecraft:set_custom_data','tag':{'GunId':'create_armorer:'+gun,'GunFireMode':mode}}]})
pools=[{'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':0.45}],'entries':entries}]
for ammo,count in [('gas_pistol_ammo',16),('slap',16),('rbapb',8)]:
    pools.append({'rolls':1,'entries':[{'type':'minecraft:item','name':'tacz:ammo','functions':[
        {'function':'minecraft:set_custom_data','tag':{'AmmoId':'create_armorer:'+ammo}},
        {'function':'minecraft:set_count','count':count}]}]})
path=ROOT/'src/main/resources/data/ccq_testground/loot_table/chests/engineering/bunker_observation.json'
path.parent.mkdir(parents=True,exist_ok=True)
path.write_text(json.dumps({'type':'minecraft:chest','pools':pools},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(path)
