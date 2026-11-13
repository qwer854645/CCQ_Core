"""Export chunk-safe sections from the approved complete base (pip install nbtlib)."""
from copy import deepcopy
from pathlib import Path
import json
import nbtlib as nbt


def main():
    folder = Path(__file__).resolve().parents[1] / "src/main/resources/data/ccq_testground/structure/engineering"
    master = nbt.load(folder / "engineering_base.nbt")
    authored_path = Path(__file__).resolve().parent / "engineering/connection_states.json"
    authored = json.loads(authored_path.read_text(encoding="utf-8")) if authored_path.exists() else {}

    def apply_authored(data, rotation="NONE", offset=(0,0,0)):
        blocks={tuple(map(int,b["pos"])):b for b in data["blocks"]}
        palette=data["palette"]
        def signature(state):return (str(state["Name"]),tuple(sorted((str(k),str(v)) for k,v in state.get("Properties",{}).items())))
        indices={signature(state):i for i,state in enumerate(palette)}
        def replace(position,state):
            block=blocks.get(position)
            if block is None:return
            state=nbt.Compound({"Name":nbt.String(state["Name"]),**({"Properties":nbt.Compound({k:nbt.String(v)for k,v in state["Properties"].items()})}if state.get("Properties")else{})})
            if str(palette[int(block["state"])]["Name"])!=str(state["Name"]):return
            key=signature(state)
            if key not in indices:indices[key]=len(palette);palette.append(state)
            block["state"]=nbt.Int(indices[key])
        for light in authored.get("lighting",[]):
            pos=tuple(v-o for v,o in zip(light["pos"],offset));block=blocks.get(pos)
            if block is not None and str(palette[int(block["state"])]["Name"])==light["from"]:
                key=("minecraft:sea_lantern",())
                if key not in indices:indices[key]=len(palette);palette.append(nbt.Compound({"Name":nbt.String(key[0])}))
                block["state"]=nbt.Int(indices[key])
        for entry in authored.get("variants",{}).get(rotation,[]):
            state=entry["state"]
            if isinstance(state,int):state=authored["palette"][state]
            replace(tuple(v-o for v,o in zip(entry["pos"],offset)),state)

    apply_authored(master)
    master.save(folder / "engineering_base.nbt")
    lodge_path=folder/"duty_lodge.nbt"
    if lodge_path.exists():
        lodge=nbt.load(lodge_path);apply_authored(lodge,offset=(29,0,14));lodge.save(lodge_path)
    assert list(master["size"]) == [120, 30, 300]
    assert not master["entities"], "Entity partitioning must be implemented before adding entities"
    destination = folder / "base"
    destination.mkdir(exist_ok=True)
    count = 0
    for rotation in ["NONE","CLOCKWISE_90","CLOCKWISE_180","COUNTERCLOCKWISE_90"]:
      variant=deepcopy(master)
      apply_authored(variant,rotation)
      for section in range(3):
        data = deepcopy(variant)
        data["size"] = nbt.List[nbt.Int]([120, 30, 100])
        palette = data["palette"]
        air = next((i for i, state in enumerate(palette) if str(state["Name"]) == "minecraft:air"), None)
        if air is None:
            air = len(palette)
            palette.append(nbt.Compound({"Name": nbt.String("minecraft:air")}))
        blocks = nbt.List[nbt.Compound]()
        occupied = set()
        for block in variant["blocks"]:
            x, y, z = map(int, block["pos"])
            if section * 100 <= z < (section + 1) * 100:
                local = deepcopy(block)
                local["pos"] = nbt.List[nbt.Int]([x, y, z - section * 100])
                blocks.append(local)
                occupied.add((x, y, z - section * 100))
                if rotation=="NONE":count += 1
        # Explicitly clear shallow terrain intrusions above the continuous slab.
        for x in range(120):
            for z in range(100):
                for y in range(1, 4):
                    if (x, y, z) not in occupied:
                        blocks.append(nbt.Compound({"pos": nbt.List[nbt.Int]([x, y, z]), "state": nbt.Int(air)}))
        data["blocks"] = blocks
        suffix=""if rotation=="NONE"else"_"+rotation.lower()
        data.save(destination / f"section_{section}{suffix}.nbt")
        print(f"section_{section}{suffix}: {len(blocks)} block entries")
    assert count == len(master["blocks"]), "Every master block must belong to exactly one section"


if __name__ == "__main__":
    main()
