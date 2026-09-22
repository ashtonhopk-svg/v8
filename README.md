# Wild Enchants (Fabric, Minecraft 26.1.2)

Adds twenty-one enchantments. Requires **Minecraft 26.1.2**, **Fabric Loader 0.19+**, **Fabric API 0.155.2+26.1.2** and **Java 25**.

| Enchantment | Max | Applies to | Effect |
|---|---|---|---|
| Exploding | 5 | weapons | A real TNT-style explosion at whatever you hit (breaks blocks, hurts entities; you are not hurt). Radius = level + 1 (2, 3, 4, 5, 6); level 3 is TNT-sized. |
| Surge | 4 | weapons | Extra damage = fraction of durability lost x (1 + (level-1)/6). At 50% durability: +50% / +58% / +67% / +75%. |
| Thunderstrike | 1 | weapons | Lightning on whatever you hit. |
| Lightning | 1 | bows | Lightning on whatever your arrow hits. |
| Carrot | 7 | weapons | On a kill, dirt-like ground around the victim becomes farmland with carrots. Radius = level + 1 (2 .. 8). |
| Tumble | 1 | maces | Only on a real mace smash (falling attack): blocks in a sphere around the hit entity become falling blocks. Radius 5 + 2 per Wind Burst level. With Wind Burst the blocks are launched up and outwards, harder per level. |
| Cloning | 2 | weapons | On a kill, 2 half-size copies (scale attribute). Players become Mannequins with their name + skin. Clones are cloned again only with Cloning II (one more stage). |
| Flight | 1 | chestplates | Wearing it lets you fly like creative mode (player data `abilities.mayfly`). While actually flying (survival), the chestplate loses 1 durability per second; when it breaks, flight ends. |
| Radius | 5 | weapons | Repeats your hit on every living entity around the one you hit: same damage, plus the weapon's other on-hit effects (Fire Aspect ignites them, Exploding blows them up, Thunderstrike zaps them, ...). Radius = level + 1 blocks (2 .. 6). Your own tamed pets and armor stands are skipped. |
| Air Jump | 5 | boots | Press jump while in the air for another jump. Level = number of air jumps before you touch the ground again. Doesn't cancel fall damage. |
| Dash | 3 | leggings | Double-tap forward in the air to be pushed forward (once per time in the air). Push 0.5 / 0.8 / 1.1 blocks per tick. |
| Fallback | 3 | leggings | Double-tap back in the air to be pushed backward (once per time in the air). Same strengths as Dash. |
| Phase | 1 | boots | Hold V to walk through thin/decorative blocks (leaves, cobwebs, vines, ferns, tall grass, kelp, ...) - see `wild_enchants:phaseable`. |
| Grapple | 3 | any tool/weapon/armor piece (`enchantable/durability`) | Right-click a block beyond your normal reach to get yanked toward it. |
| Frostbite | 3 | weapons | Hits stack a slow effect (expires after a few seconds); enough stacks and the target freezes solid for a moment. Spawns permanent (non-melting) packed ice around them. |
| Backstab | 3 | weapons | Bonus damage when the victim isn't facing you (100+ / +150% / +200%). |
| Timber | 1 | axes | Breaking one log breaks the whole connected tree (up to 256 logs) with it. |
| Telekinesis | 1 | any tool/weapon/armor piece | Hold right-click on a block or entity beyond your normal reach to drag it around; it follows your crosshair at the distance you grabbed it. On a block, releasing turns it straight back into a real block right where it is (it does NOT fall) - only for a block you're actively holding with Telekinesis, not for falling blocks in general. |
| Reach | 4 | any tool/weapon/armor piece | More block and entity interaction range per level (a real attribute change, stacks with anything else that changes reach). |
| Magnet | 3 | armor | Each piece pulls you toward the nearest magnetic block (iron block, raw iron block, lodestone) in the direction it covers: helmet up, boots down, legs/chest sideways. Surround yourself and you can hover. |
| Afterlife | 1 | armor | Die wearing it and you come back as an invisible (gear included), invulnerable ghost who can fly. While actually flying, you can also walk through any block; land and normal collision applies. Run `/respawn` to return to normal. No `/respawn` in hardcore - you're a ghost for good. |


**Falling blocks:** a falling block (sand, gravel, Tumble blocks, ...) that would break and drop because it landed awkwardly (on a slab, stair, in an occupied spot, ...) now lands on the nearest valid spot instead. Switch it to Tumble-only with `LANDING_FIX_ALL_FALLING_BLOCKS` in `Tuning.java`.

They can be found in the enchanting table (see `data/minecraft/tags/enchantment/in_enchanting_table.json`; delete that file if
you want them command-only). Test with:

```
/give @s diamond_sword[enchantments={"wild_enchants:exploding":5,"wild_enchants:surge":4}]
/give @s bow[enchantments={"wild_enchants:lightning":1}]
/give @s mace[enchantments={"wild_enchants:tumble":1,"minecraft:wind_burst":2}]
/give @s diamond_chestplate[enchantments={"wild_enchants:flight":1}]
/give @s diamond_boots[enchantments={"wild_enchants:air_jump":3}]
/give @s diamond_leggings[enchantments={"wild_enchants:dash":3,"wild_enchants:fallback":3}]
/give @s diamond_sword[enchantments={"wild_enchants:radius":3,"minecraft:fire_aspect":2}]
/give @s diamond_boots[enchantments={"wild_enchants:phase":1}]
/give @s diamond_sword[enchantments={"wild_enchants:grapple":3}]
/give @s diamond_sword[enchantments={"wild_enchants:frostbite":3}]
/give @s diamond_sword[enchantments={"wild_enchants:backstab":3}]
/give @s diamond_axe[enchantments={"wild_enchants:timber":1}]
/give @s diamond_pickaxe[enchantments={"wild_enchants:telekinesis":1}]
/give @s diamond_sword[enchantments={"wild_enchants:reach":4}]
/give @s diamond_helmet[enchantments={"wild_enchants:magnet":3}]
/give @s diamond_chestplate[enchantments={"wild_enchants:afterlife":1}]
```

## Building
1. Install JDK 25 and set it as the Gradle JVM.
2. This folder has no `gradlew` / `gradle-wrapper.jar` (binary files). Either copy `gradlew`, `gradlew.bat` and
   `gradle/wrapper/gradle-wrapper.jar` from a project made at https://fabricmc.net/develop/template/ , or run `gradle wrapper`.
3. `./gradlew build` -> `build/libs/wild-enchants-1.0.0.jar`. `./gradlew runClient` to test.

## Tuning
* Switches and numbers (blast breaks blocks, wielder immunity, Tumble block cap, clone count/scale, air jump height, dash strength, double-tap speed, ...): `Tuning.java`
* Radii, costs, weights, max levels: `data/wild_enchants/enchantment/*.json` (or override with a datapack)
