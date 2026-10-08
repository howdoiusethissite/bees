# Hivemind

A Fabric mod for Minecraft 26.3 that lets you shrink down and go inside a beehive.

## How to play

1. **Brew Shrinking Honey.** Shapeless recipe: honey bottle + fermented spider eye + glowstone dust.
   Drinking it gives you the *Shrunk* effect for 5 minutes and makes you a quarter of your normal size.
2. **Right-click a beehive or bee nest while shrunk.** You squeeze inside. Each hive in the world has
   its own interior, which is generated the first time anyone enters it.
3. **Explore the hive.** It's a big honeycomb dome with comb pillars, hanging combs and a raised
   dais in the middle where the **Queen Bee** lives. Worker bees fly around, and **Guard Bees**
   (bigger bees with helmets and little spears) patrol.
4. **Don't break anything.** The bees are neutral as long as you behave. Building isn't allowed. If you try to
   break a block, or hit any bee (the queen included), the whole hive turns on you. Guard bees fight with their
   spears and can sting over and over without dying.
5. **Bring the queen flowers.** Right-click her with any flower. Each flower raises her favor toward
   you, and every third flower she hands you a gift (bone meal, honey, honeycomb, emeralds, beenades,
   royal jelly...). At higher favor her gifts get bigger. She can give a gift at most once every 30 seconds.
   Right-click her with an empty hand to say hi and see your favor.
   She talks back, Animal Crossing style: her lines type out above your hotbar while she babbles them in
   little buzzy syllables. She also chats on her own now and then if you hang around the throne.
6. **Help in the nursery.** Four patches of brood cells are set into the floor between the pillars, each
   with a baby bee growing inside. Every so often one gets **hungry** (a honey drop shows on the cell;
   feed it a honey bottle or royal jelly) or **lonely** (a blue heart; give it a pat with an empty hand).
   Each bit of care makes it grow, from egg to little larva to big larva. When it's big enough the nurse
   bees seal the cell and leave you a **Royal Jelly**, and soon after a baby bee hatches. Looking after
   the little ones also raises the queen's favor.
7. **Defend the hive.** While you're inside, spiders, cave spiders and silverfish raid the hive every
   few minutes and go after the queen. Guards fight them, and you should too. Clear the raid and the
   queen rewards everyone present. If she drops below a quarter of her health, the raiders escape.
8. **Leave** through the glowing honey door at the south end. You'll be back where you came in, at normal size.

## Bee gear and food

- **Beenade** (stacks to 16). Throw it and it bursts into a swarm of 3 to 5 ordinary vanilla bees that
  go after the nearest monsters. Like real bees, each one dies after it stings, and any that are left
  fly off after about 25 seconds. Craft 2 from string, 2 honeycomb, gunpowder and a honey bottle,
  get them from queen gifts, or earn a handful for defending the hive. Dispensers can fire them.
- **Bee armor** (Headgear, Breastplate, Greaves, Boots). Iron-level protection, very easy to enchant,
  repaired with honeycomb. Each piece takes honeycomb plus one Royal Jelly. Wear the full set and wild
  bees won't sting you, and anything that hurts you gets two bees sent after it (once every 4 seconds).
- **Bee Multitool.** Mines anything a diamond pickaxe, axe, shovel or hoe can. Press the swap-hands key
  (**F** by default) while holding it to switch between **sword** (sweeping attacks), **axe** (stripping
  logs, scraping copper), **shovel** (making paths) and **hoe** (tilling). Attack damage and speed change
  to match. Shapeless recipe: one of each diamond tool, three Royal Jelly and a honey block.
  Repaired with Royal Jelly.
- **Food.** Royal Jelly (Regeneration), Honey Toast (bread + honey), Honeyed Apple (apple + honey; cures
  poison, handy against cave spiders), Honeycomb Candy (honeycomb + sugar + honey makes 4; a quick snack
  with a burst of Speed) and Honey-Glazed Ham (cooked porkchop + honey + sugar).

## Commands and data

Ops can use `/hivemind raid` to start a raid in the hive they're in and `/hivemind leave` to exit.

Gift contents live in data-pack loot tables, so they're easy to change:
`data/hivemind/loot_table/gameplay/queen_gift.json` and `queen_defense_reward.json`.

## Building

Requires Java 25.

```
./gradlew build                 # jar in build/libs
./gradlew runClient             # play in a dev client
./gradlew runClientGameTest     # scripted play-through with screenshots (use xvfb-run when headless)
python3 tools/gen_textures.py   # regenerate all textures
python3 tools/gen_sounds.py     # regenerate the queen's babble syllables (needs numpy and ffmpeg)
```

All textures are generated from `tools/gen_textures.py` as original pixel art, and the queen's voice is
synthesized by `tools/gen_sounds.py`. The guard bees reuse the vanilla
bee texture for their bodies, loaded from the game at runtime, with their own helmet-and-spear texture layered on top.

## Screenshots

From the scripted client test (`runClientGameTest`):

| | |
|---|---|
| ![Inside the hive](docs/screenshots/hive_interior.png) | ![The queen](docs/screenshots/queen_front.png) |
| ![Queen close-up](docs/screenshots/queen_face.png) | ![Queen from behind](docs/screenshots/queen_back.png) |
| ![Guard bee](docs/screenshots/guard_bee.png) | ![A raid](docs/screenshots/raid.png) |
