"""Repair solid supports and exterior lamp placement in master templates."""
from pathlib import Path
import json,nbtlib as n
from engineering_template_cleanup import fix_support_geometry
ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'src/main/resources/data/ccq_testground/structure/engineering'
def main():
    report={}
    for name in ['engineering_base','duty_lodge','survey_tower','calibration_range','assembly_pad']:
        path=FOLDER/(name+'.nbt');data=n.load(path)
        blocks={tuple(map(int,b['pos'])):b for b in data['blocks']}
        report[name]=fix_support_geometry(blocks,data['palette'])
        data['blocks']=n.List[n.Compound]([blocks[p]for p in sorted(blocks)]);data.save(path)
    (ROOT/'tools/engineering/geometry_fixes_090.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print({key:{'slabs':len(v['support_slabs']),'lamps':len(v['relocated_lamps'])}for key,v in report.items()},flush=True)
if __name__=='__main__':main()
