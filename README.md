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
   you, and every third flower she hands you a gift (bone meal, honey, honeycomb, emeralds...). At
   higher favor her gifts get bigger. She can give a gift at most once every 30 seconds.
   Right-click her with an empty hand to see your favor.
6. **Defend the hive.** While you're inside, spiders, cave spiders and silverfish raid the hive every
   few minutes and go after the queen. Guards fight them, and you should too. Clear the raid and the
   queen rewards everyone present. If she drops below a quarter of her health, the raiders escape.
7. **Leave** through the glowing honey door at the south end. You'll be back where you came in, at normal size.

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
```

All textures are generated from `tools/gen_textures.py` as original pixel art. The guard bees reuse the vanilla
bee texture for their bodies, loaded from the game at runtime, with their own helmet-and-spear texture layered on top.

## Screenshots

From the scripted client test (`runClientGameTest`):

| | |
|---|---|
| ![Inside the hive](docs/screenshots/hive_interior.png) | ![The queen](docs/screenshots/queen_front.png) |
| ![Queen close-up](docs/screenshots/queen_face.png) | ![Queen from behind](docs/screenshots/queen_back.png) |
| ![Guard bee](docs/screenshots/guard_bee.png) | ![A raid](docs/screenshots/raid.png) |
