# Portal Brews

A Fabric mod for Minecraft 26.2 that adds a chain of brewable, throwable **portal potions**. Each one encodes a destination from a lodestone-linked compass and does something different with it — teleport, or raise a standing portal "frame" that shows where it leads.

| Potion | Thrown effect |
|---|---|
| **Portal Potion** | Splash teleports everything in range to the destination (cross-dimension too). |
| **Portal Frame Potion** | Raises an animated portal frame at the impact point for **30 seconds**, labelled with the destination. |
| **Permanent Portal Frame Potion** | Same frame, but it never expires. |

The recipe chain brews each tier from the one before it:

```
splash water bottle  + lodestone compass  ->  Portal Potion
Portal Potion        + ender pearl        ->  Portal Frame Potion
Portal Frame Potion  + ghast tear         ->  Permanent Portal Frame Potion
```

Run **`/portal`** in-game to receive a written-book manual covering all of this.

## Requirements

- Minecraft **26.2** (Java Edition)
- Fabric Loader **>= 0.19.3**
- Fabric API **0.155.2+26.2** or newer
- Java **21+**

## How to use it

### 1. Link a compass to your destination

- Craft or `/give @s minecraft:compass` and `/give @s minecraft:lodestone`.
- Place the lodestone at the location you want to travel to.
- Right-click the lodestone with the compass. The compass is now a **Lodestone Compass** — it stores that block's coordinates *and dimension*.

### 2. Get a splash water bottle

- Fill a `Glass Bottle` at a water source → `Water Bottle`.
- Put it in a **Brewing Stand** with `Gunpowder` in the ingredient slot and `Blaze Powder` for fuel → `Splash Water Bottle`.

### 3. Brew the Portal Potion

- Put the splash water bottle in the brewing stand.
- Put your **lodestone-linked compass** in the ingredient slot.
- Add blaze powder if needed.
- After brewing, you get a **Portal Potion** with the compass's coordinates and dimension baked in.

> A compass that has *not* been linked to a lodestone still brews into a Portal Potion, but the potion has no destination and will do nothing when thrown.

### 4. Throw it

- Right-click the Portal Potion to throw it, exactly like a splash potion.
- Everything caught in the splash radius (~4 blocks) is teleported to the stored coordinates.
- Throw it at your own feet to teleport yourself.
- Throw it at a mob to send them (permanently) to the destination.
- Cross-dimension works: Overworld ↔ Nether ↔ End, so long as the destination dimension exists.

## Portal frames

Brew a **Portal Frame Potion** (add an ender pearl to a Portal Potion) or a **Permanent Portal Frame Potion** (add a ghast tear to a Portal Frame Potion), then throw it like any splash potion.

- Where it lands, a translucent, animated portal quad rises out of the ground, oriented to face the way you threw it.
- Floating above it is the **destination it points to** — coordinates and dimension, read straight from the encoded lodestone.
- A plain Portal Frame Potion's frame fades after **30 seconds**; the permanent variant stays until the chunk unloads (and is re-shown on reload). Both survive `/kill`-style damage — they ignore it.
- The two tiers are colour-coded: the timed frame glows magenta, the permanent one gold.

> **On destination "snapshots":** the frame shows a live text readout of the destination rather than a rendered image of it. A Minecraft *server* is headless — it has no renderer or GPU — so it cannot screenshot a faraway location to send to clients. The unique per-compass `lodestone_id` component is still stored on every linked compass and carried through the whole brew chain, so a future client-side snapshot feature has a stable key to hang an image on; the server simply can't be the thing that produces that image.

## The manual

`/portal` puts a written book titled **Portal Brews Manual** into your inventory, walking through linking a compass, making splash water, brewing, and throwing.

## Troubleshooting

| Symptom | Cause |
|---|---|
| Portal Potion does nothing on splash | The compass wasn't lodestone-linked at brew time. Re-link and re-brew. |
| Brewing gives you nothing | Wrong bottle — you need **splash** water bottle (with gunpowder added first), not a plain water bottle. |
| Teleport lands you inside a block | The destination is `lodestone position + 1 block up`. Place lodestones on a floor, not in a wall. |

## Build

```powershell
.\gradlew.bat build       # produces build/libs/portalbrews-*.jar
.\gradlew.bat runClient   # dev client with the mod loaded
```