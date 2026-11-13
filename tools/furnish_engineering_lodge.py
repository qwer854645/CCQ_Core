"""Rebuild the base's lodge interior with the CCQ pack's actual furniture blocks."""
from pathlib import Path
from copy import deepcopy
import json
import nbtlib as n
from split_engineering_base import main as split
from engineering_template_cleanup import remove_names_and_signs, complete_range_corner

ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/"src/main/resources/data/ccq_testground/structure/engineering"


def main():
    master=n.load(FOLDER/"engineering_base.nbt")
    palette=master["palette"]
    blocks={tuple(map(int,b["pos"])):b for b in master["blocks"]}
    supplies={p:deepcopy(blocks[p]) for p in [(35,2,15),(30,2,21),(30,7,30),(34,7,30),(41,7,16)]}
    # Preserve the shell, floor/stairwell, staircase, doors, balcony and loot.
    for pos in list(blocks):
        x,y,z=pos
        if 30<=x<=42 and 15<=z<=31 and (2<=y<=5 or 7<=y<=10):
            if 31<=x<=32 and 23<=z<=27 and 2<=y<=6:continue
            if y==7 and ((x in [30,33] and 24<=z<=28)or(31<=x<=32 and z==28)):continue
            del blocks[pos]

    def put(x,y,z,name,**props):
        if ":"not in name:name="minecraft:"+name
        state=n.Compound({"Name":n.String(name)})
        if props:state["Properties"]=n.Compound({k:n.String(str(v).lower()) for k,v in props.items()})
        if state not in palette:palette.append(state)
        blocks[x,y,z]=n.Compound({"pos":n.List[n.Int]([x,y,z]),"state":n.Int(palette.index(state))})

    def area(x,y,z,X,Z,name,**props):
        for xx in range(x,X+1):
            for zz in range(z,Z+1):put(xx,y,zz,name,**props)

    # Kitchen: pantry, preparation, water, cutting, lit stove and two real pots.
    for x in [33,34,36,37,39,41]:put(x,2,15,"farmersdelight:spruce_cabinet",facing="south",open=False)
    put(38,2,15,"water_cauldron",level=3)
    put(40,2,15,"farmersdelight:stove",facing="south",lit=True)
    put(40,3,15,"farmersdelight:cooking_pot",facing="south",support="none")
    put(41,3,15,"farmersdelight:cutting_board",facing="south")
    put(42,2,15,"kaleidoscope_cookery:stove",facing="south",lit=True)
    put(42,3,15,"kaleidoscope_cookery:stockpot",facing="south",has_base=False,has_chains=False,has_lid=True,waterlogged=False)
    put(36,3,15,"kaleidoscope_cookery:fruit_basket",facing="south",waterlogged=False)
    put(37,3,15,"supplementaries:jar")
    put(33,3,15,"farmersrespite:potted_tea_bush")
    for x in [34,36]:put(x,4,15,"kaleidoscope_cookery:kitchenware_racks",facing="south",waterlogged=False)
    put(34,5,15,"createdeco:yellow_brass_lamp",facing="south",inverted=True,lit=True,waterlogged=False)
    put(42,4,16,"kaleidoscope_cookery:kitchenware_racks",facing="west",waterlogged=False)

    # Dining table with four usable chairs, served food, tea and warm pendant.
    for x,part in [(37,1),(38,2),(39,3)]:put(x,2,20,"kaleidoscope_cookery:table_spruce",axis="x",position=part,has_carpet=False,waterlogged=False)
    for x in [37,39]:
        put(x,2,19,"kaleidoscope_cookery:chair_spruce",facing="south",has_carpet=False,waterlogged=False)
        put(x,2,21,"kaleidoscope_cookery:chair_spruce",facing="north",has_carpet=False,waterlogged=False)
    put(37,3,20,"kaleidoscope_cookery:baozi_plate",facing="south",servings=5)
    put(38,3,20,"kaleidoscope_cookery:teapot")
    put(39,3,20,"kaleidoscope_cookery:empty_cup",cup_count=2,facing="south")
    for y,half in [(4,"lower"),(5,"upper")]:put(38,y,20,"kaleidoscope_tavern:bell_pendant_lamp",facing="south",half=half)
    area(34,2,23,41,25,"light_gray_carpet")
    area(35,2,24,40,24,"brown_carpet")

    # Duty desk uses actual CC:Tweaked appliances. The notice board faces indoors.
    for z in [16,17,18,19]:put(30,2,z,"farmersdelight:spruce_cabinet",facing="east",open=False)
    put(30,3,17,'radiologistics:main_computer',facing='up')
    put(30,3,19,"supplementaries:book_pile_horizontal")
    put(31,2,17,"kaleidoscope_cookery:chair_spruce",facing="west",has_carpet=False,waterlogged=False)
    put(30,3,16,"supplementaries:globe",facing="east")
    put(30,5,21,"createdeco:yellow_brass_lamp",facing="east",inverted=True,lit=True,waterlogged=False)
    put(42,4,23,"supplementaries:clock_block",facing="west",two_faced=False)
    put(42,2,25,"supplementaries:sack",open=False)

    # South-side lounge: connected sofa, tea/coffee table and reading corner.
    area(35,2,28,41,30,"brown_carpet")
    for x,connection in [(36,"left"),(37,"middle"),(38,"middle"),(39,"right")]:
        put(x,2,31,"kaleidoscope_tavern:brown_sofa",facing="north",connection=connection,waterlogged=False)
    for x,part in [(37,1),(38,3)]:put(x,2,29,"kaleidoscope_tavern:table",axis="x",position=part,waterlogged=False)
    put(37,3,29,"farmersrespite:coffee_cake",bites=0)
    put(38,3,29,"kaleidoscope_cookery:empty_cup",cup_count=2,facing="north")
    for z in [29,30,31]:put(42,2,z,"bookshelf")
    put(42,3,29,"potted_fern")
    put(42,3,31,"supplementaries:globe",facing="north")
    put(41,4,31,"createdeco:yellow_brass_lamp",facing="north",inverted=True,lit=True,waterlogged=False)

    # One aligned row of four identical beds, matching bedside storage and lights.
    area(33,7,15,41,15,"spruce_planks")
    for x in [34,36,38,40]:
        put(x,7,16,"blue_bed",facing="north",occupied=False,part="head")
        put(x,7,17,"blue_bed",facing="north",occupied=False,part="foot")
    for x in [33,35,37,39,41]:put(x,7,16,"barrel",facing="south",open=False)
    for x in [33,37,41]:put(x,8,16,"lantern",hanging=False,waterlogged=False)
    for x in [35,39]:put(x,8,16,"farmersrespite:potted_tea_bush")
    # Wardrobes align along one wall; the south side is a continuous storage/desk row.
    for y in [7,8]:
        for z in [16,17,18,19]:put(30,y,z,"farmersdelight:spruce_cabinet",facing="east",open=False)
    for x in [31,32,33]:put(x,7,30,"farmersdelight:spruce_cabinet",facing="north",open=False)
    for x in [30,34]:put(x,7,30,"chest",facing="north",type="single",waterlogged=False)
    for x in range(36,42):put(x,7,30,"farmersdelight:spruce_cabinet",facing="north",open=False)
    put(36,8,30,"farmersrespite:potted_tea_bush")
    put(37,8,30,"kaleidoscope_cookery:teapot")
    put(38,8,30,"kaleidoscope_cookery:empty_cup",cup_count=2,facing="north")
    put(40,8,30,"supplementaries:globe",facing="north")
    for x in [37,39]:put(x,7,29,"kaleidoscope_cookery:chair_spruce",facing="south",has_carpet=False,waterlogged=False)
    area(34,7,21,40,26,"light_gray_carpet")
    area(35,7,22,39,25,"blue_carpet")
    put(42,9,21,"supplementaries:clock_block",facing="west",two_faced=False)
    for x in [35,39]:
        for y,half in [(9,"lower"),(10,"upper")]:put(x,y,23,"kaleidoscope_tavern:bell_pendant_lamp",facing="north",half=half)
    area(35,11,23,41,23,"spruce_planks")
    put(30,9,22,"createdeco:yellow_brass_lamp",facing="east",inverted=True,lit=True,waterlogged=False)
    for pos,block in supplies.items():
        if pos in blocks:block["state"]=blocks[pos]["state"]
        blocks[pos]=block
    remove_names_and_signs(blocks,palette)
    complete_range_corner(blocks,palette)

    master["blocks"]=n.List[n.Compound]([blocks[p] for p in sorted(blocks)])
    master.save(FOLDER/"engineering_base.nbt")
    # An independent lodge template also makes visual inspection straightforward.
    lodge=deepcopy(master);lodge["size"]=n.List[n.Int]([18,17,19])
    selected=[]
    for (x,y,z),block in blocks.items():
        if 29<=x<=46 and 0<=y<=16 and 14<=z<=32:
            item=deepcopy(block);item["pos"]=n.List[n.Int]([x-29,y,z-14]);selected.append(item)
    lodge["blocks"]=n.List[n.Compound](selected);lodge.save(FOLDER/"duty_lodge.nbt")
    split()
    print("Furnished lodge exported; original supply contents and other facilities retained")


if __name__=="__main__":main()
