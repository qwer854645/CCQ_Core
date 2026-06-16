import json
import os

BASE = r"M:\others\ae2-ponder-fix\src\main\resources\assets\ccq_core"

SIDES = ["north", "south", "east", "west", "up", "down"]
ARM_ROT = {
    "north": {},
    "south": {"y": 180},
    "east": {"y": 90},
    "west": {"y": 270},
    "up": {"x": 270},
    "down": {"x": 90},
}

CABLE_TYPES = {
    "ponder_glass_cable": "glass",
    "ponder_covered_cable": "covered",
    "ponder_smart_cable": "smart",
    "ponder_dense_cable": "dense",
    "ponder_dense_smart_cable": "dense_smart",
    "ponder_quartz_fiber": "quartz",
}

AE2_CABLE = {
    "glass": "glass",
    "covered": "covered",
    "smart": "smart",
    "dense": "dense_covered",
    "dense_smart": "dense_smart",
}

PART_LAYERS = {
    "ponder_terminal": ["part"],
    "ponder_crafting_terminal": ["part"],
    "ponder_pattern_encoding_terminal": ["part"],
    "ponder_pattern_access_terminal": ["part"],
    "ponder_conversion_monitor": ["part"],
    "ponder_storage_bus": ["layer0", "layer1"],
    "ponder_toggle_bus": ["layer0", "layer1"],
    "ponder_inverted_toggle_bus": ["layer0", "layer1"],
    "ponder_import_bus": ["layer0", "layer1"],
    "ponder_export_bus": ["layer0", "layer1"],
    "ponder_level_emitter": ["layer0", "layer1"],
    "ponder_storage_monitor": ["layer0", "layer1"],
    "ponder_me_p2p_tunnel": ["layer0", "layer1", "layer2"],
    "ponder_source_acceptor": ["layer0"],
    "ponder_source_p2p_tunnel": ["layer0", "layer1", "layer2"],
    "ponder_spell_p2p_tunnel": ["layer0", "layer1", "layer2"],
    "ponder_annihilation_plane": ["layer0"],
    "ponder_formation_plane": ["layer0"],
}


def bf(n, s, e, w, u, d):
    return {"north": n, "south": s, "east": e, "west": w, "up": u, "down": d}


def state_key(state, part_face=None):
    parts = []
    if part_face is not None:
        parts.append(f"part_face={part_face}")
    for side in SIDES:
        parts.append(f"{side}={'true' if state[side] else 'false'}")
    return ",".join(parts)


def apply_entry(model, rot=None):
    entry = {"model": model}
    if rot:
        entry.update(rot)
    return entry


def is_straight_pair(state):
    if state["north"] and state["south"] and not any(state[s] for s in ["east", "west", "up", "down"]):
        return {}
    if state["east"] and state["west"] and not any(state[s] for s in ["north", "south", "up", "down"]):
        return {"y": 90}
    if state["up"] and state["down"] and not any(state[s] for s in ["north", "south", "east", "west"]):
        return {"x": 90}
    return None


def cable_models(cable_type, state):
    if cable_type == "quartz":
        prefix = "ccq_core:block/ponder_quartz_fiber"
        active = [s for s in SIDES if state[s]]
        if not active:
            return [apply_entry(f"{prefix}/core")]
        models = [apply_entry(f"{prefix}/core")]
        for side in active:
            models.append(apply_entry(f"{prefix}/arm", ARM_ROT[side]))
        return models

    prefix = f"ccq_core:block/ponder_cable/{cable_type}"
    active = [s for s in SIDES if state[s]]
    count = len(active)

    if count == 0:
        return [apply_entry(f"{prefix}/core")]

    if count == 1:
        return [
            apply_entry(f"{prefix}/core"),
            apply_entry(f"{prefix}/arm", ARM_ROT[active[0]]),
        ]

    straight_rot = is_straight_pair(state)
    if count == 2 and straight_rot is not None:
        return [apply_entry(f"{prefix}/straight", straight_rot)]

    models = [apply_entry(f"{prefix}/core")]
    for side in active:
        models.append(apply_entry(f"{prefix}/arm", ARM_ROT[side]))
    return models


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def all_bool_states():
    for n in (False, True):
        for s in (False, True):
            for e in (False, True):
                for w in (False, True):
                    for u in (False, True):
                        for d in (False, True):
                            yield bf(n, s, e, w, u, d)


def main():
    for block, cable_type in CABLE_TYPES.items():
        variants = {}
        for state in all_bool_states():
            models = cable_models(cable_type, state)
            variants[state_key(state)] = models if len(models) > 1 else models[0]
        write_json(os.path.join(BASE, "blockstates", f"{block}.json"), {"variants": variants})

        if cable_type == "quartz":
            continue

        ae2 = AE2_CABLE[cable_type]
        write_json(
            os.path.join(BASE, "models", "block", "ponder_cable", cable_type, "straight.json"),
            {
                "parent": f"ae2:part/cable/{ae2}/straight",
                "textures": {
                    "base": f"ae2:part/cable/{ae2}/transparent",
                    **(
                        {"channels": f"ae2:part/cable/{ae2}/channels_04"}
                        if cable_type in ("smart", "dense_smart")
                        else {}
                    ),
                },
            },
        )

    for block, layers in PART_LAYERS.items():
        variants = {}
        for part_face in SIDES:
            for state in all_bool_states():
                models = cable_models("covered", state)
                rot = ARM_ROT[part_face]
                for layer in layers:
                    models.append(
                        apply_entry(f"ccq_core:block/ponder_part/{block}/{layer}", rot)
                    )
                key = state_key(state, part_face)
                variants[key] = models if len(models) > 1 else models[0]
        write_json(os.path.join(BASE, "blockstates", f"{block}.json"), {"variants": variants})

    print("OK: generated cable + part blockstates")


if __name__ == "__main__":
    main()
