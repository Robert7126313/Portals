# Portal Brews

A Fabric mod for Minecraft 26.2 that adds a brewable, throwable potion which teleports anyone it splashes to a location encoded from a lodestone compass — across dimensions too.

## Requirements

- Minecraft **26.2** (Java Edition)
- Fabric Loader **>= 0.19.3**
- Fabric API **0.155.2+26.2** or newer
- Java **21+**

## How to use it

### 1. Link a compass to your destination

- Craft or `/give @s minecraft:compass` and `/give @s minecraft:lodestone`.
- Place the lodestone at 
- the location you want to travel to.
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