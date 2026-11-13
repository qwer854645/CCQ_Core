"""Migrate authored CC terminals and supplies in registered structures, in place."""
from pathlib import Path
import json
import nbtlib as n

ROOT=Path(__file__).resolve().parents[1]
ITEMS={
    'computercraft:computer_normal':'radiologistics:main_computer',
    'computercraft:computer_advanced':'radiologistics:main_computer',
    'computercraft:monitor_normal':'create:display_board',
    'computercraft:monitor_advanced':'create:display_board',
    'computercraft:printer':'create:clipboard',
    'computercraft:disk_drive':'create:analog_lever',
    'computercraft:disk':'create:empty_schematic',
    'computercraft:cable':'create:redstone_link',
    'computercraft:wired_modem':'create:display_link',
    'computercraft:wired_modem_full':'create:display_link',
    'computercraft:wireless_modem_normal':'create:redstone_link',
    'computercraft:wireless_modem_advanced':'create:redstone_link',
}

def replace_inventory(tag):
    changed=0
    if isinstance(tag,n.Compound):
        item=str(tag.get('id',''))
        if item.startswith('computercraft:') and ('count'in tag or 'Count'in tag):
            tag['id']=n.String(ITEMS.get(item,'create:clipboard'));changed+=1
            if 'components'in tag:
                for key in list(tag['components']):
                    if str(key).startswith('computercraft:'):del tag['components'][key]
        for value in tag.values():changed+=replace_inventory(value)
    elif isinstance(tag,n.List):
        for value in tag:changed+=replace_inventory(value)
    return changed

def main():
    changes=[]
    for path in (ROOT/'src/main/resources/data/ccq_testground/structure').rglob('*.nbt'):
        data=n.load(path);replaced=set();stock=0
        for index,state in enumerate(data['palette']):
            old=str(state['Name'])
            if not old.startswith('computercraft:'):continue
            facing=str(state.get('Properties',{}).get('facing','south'))
            if facing not in ['north','south','east','west']:facing='south'
            if old.endswith(':printer'):
                replacement=n.Compound({'Name':n.String('supplementaries:book_pile_horizontal')})
            else:
                replacement=n.Compound({'Name':n.String('radiologistics:main_computer'),'Properties':n.Compound({
                    'facing':n.String('up')})})
            data['palette'][index]=replacement;replaced.add(index)
        placed=0
        for block in data['blocks']:
            if int(block['state'])in replaced:
                if 'nbt'in block:del block['nbt']
                placed+=1
            if 'nbt'in block:stock+=replace_inventory(block['nbt'])
        if replaced or stock:
            data.save(path)
            changes.append({'file':str(path.relative_to(ROOT)),'terminal_blocks_replaced':placed,'stock_entries_replaced':stock})
    print(json.dumps(changes,ensure_ascii=False),flush=True)

if __name__=='__main__':main()
