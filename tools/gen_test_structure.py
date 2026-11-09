#!/usr/bin/env python3
"""Writes the GameTest arena structure (data/onepiece/structures/arena.nbt): a 16x8x16 box with a stone floor."""
import gzip, struct

def tag_string(s): b = s.encode(); return struct.pack(">H", len(b)) + b
def named(tid, name, payload): return bytes([tid]) + tag_string(name) + payload
def t_int(v): return struct.pack(">i", v)
def t_list(tid, items): return bytes([tid]) + struct.pack(">i", len(items)) + b"".join(items)
def t_compound(entries): return b"".join(entries) + b"\x00"

SX, SY, SZ = 16, 8, 16
palette = [t_compound([named(8, "Name", tag_string("minecraft:stone"))])]
blocks = []
for x in range(SX):
    for z in range(SZ):
        blocks.append(t_compound([named(9, "pos", t_list(3, [t_int(x), t_int(0), t_int(z)])), named(3, "state", t_int(0))]))
root = t_compound([
    named(3, "DataVersion", t_int(3465)),
    named(9, "size", t_list(3, [t_int(SX), t_int(SY), t_int(SZ)])),
    named(9, "palette", t_list(10, palette)),
    named(9, "blocks", t_list(10, blocks)),
    named(9, "entities", t_list(10, [])),
])
data = named(10, "", root)
with gzip.open("src/main/resources/data/onepiece/structures/arena.nbt", "wb") as f:
    f.write(data)
print("arena.nbt written", len(data), "bytes")
