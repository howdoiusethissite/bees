package com.hivemind.test;

import com.hivemind.Hivemind;
import com.hivemind.block.BroodCellBlock;
import com.hivemind.entity.GuardBee;
import com.hivemind.entity.QueenBee;
import com.hivemind.item.BeeMultitoolItem;
import com.hivemind.item.BeeToolMode;
import com.hivemind.registry.ModBlocks;
import com.hivemind.registry.ModEffects;
import com.hivemind.registry.ModItems;
import com.hivemind.world.BeeArmor;
import com.hivemind.world.BeeSwarms;
import com.hivemind.world.HiveLayout;
import com.hivemind.world.HiveRaids;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Plays through the mod in a real client: shrink, enter a bee nest, look at the queen and guards,
 * give flowers, try to break the hive, fight off a raid, and leave. Screenshots land in the
 * game-test run directory.
 */
public class HiveClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext ctx) {
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			world.getConnection().waitForChunksRender();
			server.runCommand("gamemode survival @a");
			server.runCommand("difficulty normal");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");

			// Put a bee nest in front of the player and drink the shrinking honey.
			BlockPos nest = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos pos = player.blockPosition().relative(Direction.NORTH, 2);
				player.level().setBlock(pos, Blocks.BEE_NEST.defaultBlockState(), 3);
				player.addEffect(new MobEffectInstance(ModEffects.SHRUNK, 20 * 60));
				return pos;
			});
			ctx.waitTicks(10);
			ctx.getInput().lookAt(nest);
			ctx.waitTicks(5);
			ctx.takeScreenshot("hivemind_0_shrunk_at_nest");

			// Right-click the nest to go inside.
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitFor(mc -> mc.level != null && mc.level.dimension().equals(HiveLayout.HIVE_LEVEL), 200);
			world.getConnection().waitForChunksRender();
			ctx.waitTicks(40);
			check(server.computeOnServer(s -> !player(s).hasEffect(ModEffects.SHRUNK)), "shrunk effect should be removed inside the hive");
			ctx.takeScreenshot("hivemind_1_arrival");

			BlockPos throne = HiveLayout.throne(0);
			int residents = server.computeOnServer(s -> hiveLevel(s).getEntitiesOfClass(Mob.class, HiveLayout.bounds(0)).size());
			check(residents >= 15, "expected queen, guards and workers inside, found " + residents);
			check(server.computeOnServer(s -> !hiveLevel(s).getEntitiesOfClass(QueenBee.class, HiveLayout.bounds(0)).isEmpty()), "queen should exist");
			check(
				server.computeOnServer(s -> hiveLevel(s).getBlockState(HiveLayout.exitPos(0)).is(ModBlocks.HIVE_EXIT)), "exit block should be placed"
			);

			// Breaking the hive is not allowed, and it angers the guards.
			BlockPos floor = BlockPos.containing(HiveLayout.arrivalPos(0)).below();
			boolean broke = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				player.teleportTo(hiveLevel(s), floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5, java.util.Set.of(), 180.0F, 0.0F, false);
				return player.gameMode.destroyBlock(floor);
			});
			check(!broke, "breaking blocks inside the hive should be blocked");
			check(server.computeOnServer(s -> !hiveLevel(s).getBlockState(floor).isAir()), "floor block should still be there");
			long angry = server.computeOnServer(
				s -> hiveLevel(s).getEntitiesOfClass(GuardBee.class, HiveLayout.bounds(0)).stream().filter(GuardBee::isAngry).count()
			);
			check(angry > 0, "guards should be angry after trying to break the hive");
			ctx.waitTicks(40);
			ctx.takeScreenshot("hivemind_1b_angry_guards");

			// Calm everyone down (and keep the player alive), then fend off a raid.
			server.runOnServer(s -> {
				for (GuardBee guard : hiveLevel(s).getEntitiesOfClass(GuardBee.class, HiveLayout.bounds(0))) {
					guard.stopBeingAngry();
				}
				for (net.minecraft.world.entity.animal.bee.Bee bee : hiveLevel(s).getEntitiesOfClass(net.minecraft.world.entity.animal.bee.Bee.class, HiveLayout.bounds(0))) {
					bee.stopBeingAngry();
				}
			});
			server.runCommand("effect give @p minecraft:resistance 600 4");
			server.runCommand("effect give @p minecraft:regeneration 600 2");
			teleport(server, throne.getX() + 0.5, throne.getY() - 1, throne.getZ() + 6.5, 180.0F);
			ctx.waitTicks(10);
			int emeraldsBefore = server.computeOnServer(s -> player(s).getInventory().countItem(Items.EMERALD));
			server.runCommand("execute as @p at @p run hivemind raid");
			ctx.waitTicks(60);
			int invaders = server.computeOnServer(
				s -> hiveLevel(s).getEntitiesOfClass(Mob.class, HiveLayout.bounds(0), m -> m.entityTags().contains(HiveRaids.INVADER_TAG)).size()
			);
			check(invaders > 0, "a raid should spawn invaders");
			check(HiveRaids.isRaidActive(0), "raid should be active");
			ctx.takeScreenshot("hivemind_1c_raid");
			server.runCommand("execute in hivemind:hive run kill @e[tag=" + HiveRaids.INVADER_TAG + "]");
			ctx.waitTicks(80);
			check(!HiveRaids.isRaidActive(0), "raid should end once invaders are gone");
			int emeraldsAfter = server.computeOnServer(s -> player(s).getInventory().countItem(Items.EMERALD));
			check(emeraldsAfter > emeraldsBefore, "defending the hive should be rewarded with emeralds");

			// Clear the bees out of the way for portraits, then take them as a spectator.
			int favorBefore = server.computeOnServer(s -> queen(s).getFavor(player(s)));
			server.runCommand("execute in hivemind:hive run kill @e[type=minecraft:bee]");
			server.runCommand("execute in hivemind:hive run kill @e[type=hivemind:guard_bee]");
			server.runCommand("execute in hivemind:hive run kill @e[type=minecraft:item]");
			server.runCommand("gamemode spectator @a");
			double tx = throne.getX() + 0.5;
			double ty = throne.getY();
			double tz = throne.getZ() + 0.5;
			photo(ctx, server, "hivemind_2_queen_front", tx, ty - 0.1, tz + 4.5, tx, ty + 1.3, tz);
			photo(ctx, server, "hivemind_3_queen_face", tx, ty + 0.5, tz + 2.0, tx, ty + 2.0, tz);
			photo(ctx, server, "hivemind_4_queen_side", tx + 4.5, ty - 0.1, tz, tx, ty + 1.3, tz);
			photo(ctx, server, "hivemind_5_queen_back", tx + 3.0, ty + 0.3, tz - 2.4, tx, ty + 1.3, tz);

			BlockPos c = HiveLayout.center(0);
			double gx = c.getX() + 0.5;
			double gy = HiveLayout.floorY() + 3;
			double gz = c.getZ() + 12.5;
			server.runCommand("execute in hivemind:hive run summon hivemind:guard_bee " + gx + " " + gy + " " + gz + " {NoAI:1b,Rotation:[0f,0f]}");
			photo(ctx, server, "hivemind_6_guard_front", gx, gy - 1.0, gz + 3.2, gx, gy + 0.5, gz);
			photo(ctx, server, "hivemind_7_guard_side", gx + 3.2, gy - 1.0, gz, gx, gy + 0.5, gz);
			photo(ctx, server, "hivemind_8_overview", c.getX() + 0.5, gy + 4, c.getZ() + 19.5, c.getX() + 0.5, gy, c.getZ() + 0.5);

			// Give the queen flowers. Every third flower earns a gift.
			server.runCommand("gamemode survival @a");
			server.runCommand("clear @p");
			server.runCommand("give @p minecraft:dandelion 6");
			aim(server, tx, ty, tz + 2.5, tx, ty + 1.3, tz);
			ctx.waitTicks(20);
			for (int i = 0; i < 6; i++) {
				ctx.getInput().pressKey(options -> options.keyUse);
				ctx.waitTicks(6);
			}
			ctx.waitTicks(40);
			int favorGained = server.computeOnServer(s -> queen(s).getFavor(player(s))) - favorBefore;
			check(favorGained == 6, "favor should rise by 6 after six flowers, rose by " + favorGained);
			int gifts = server.computeOnServer(s -> countGifts(player(s)));
			check(gifts > 0, "the queen should have given at least one gift");
			ctx.takeScreenshot("hivemind_9_gift");


			// The queen chats when you greet her; the line types itself out over the hotbar.
			server.runCommand("clear @p");
			aim(server, tx, ty, tz + 2.5, tx, ty + 1.3, tz);
			ctx.waitTicks(30);
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitTicks(14);
			ctx.takeScreenshot("hivemind_9b_queen_talks");

			// Look after a baby bee in the nursery: feed the hungry one, pat the lonely one.
			BlockPos cell = new BlockPos(c.getX() + 9, HiveLayout.floorY(), c.getZ() + 5);
			check(server.computeOnServer(s -> hiveLevel(s).getBlockState(cell).is(ModBlocks.BROOD_CELL)), "nursery should have brood cells");
			setCell(server, cell, 1, BroodCellBlock.Need.HUNGRY);
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:honey_bottle");
			aim(server, cell.getX() + 0.5, cell.getY() + 1, cell.getZ() + 3.5, cell.getX() + 0.5, cell.getY() + 1.0, cell.getZ() + 0.5);
			ctx.waitTicks(10);
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitTicks(5);
			check(cellState(server, cell).getValue(BroodCellBlock.STAGE) == 2, "feeding a hungry larva should make it grow");
			check(cellState(server, cell).getValue(BroodCellBlock.NEED) == BroodCellBlock.Need.NONE, "a fed larva shouldn't be hungry");
			check(server.computeOnServer(s -> player(s).getMainHandItem().is(Items.GLASS_BOTTLE)), "feeding should leave an empty bottle");

			setCell(server, cell, 2, BroodCellBlock.Need.LONELY);
			server.runCommand("clear @p");
			ctx.waitTicks(2);
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitTicks(5);
			check(cellState(server, cell).getValue(BroodCellBlock.STAGE) == BroodCellBlock.CAPPED, "patting a lonely larva should get it to cocoon");
			ctx.waitTicks(30);
			int jelly = server.computeOnServer(
				s -> player(s).getInventory().countItem(ModItems.ROYAL_JELLY)
					+ hiveLevel(s).getEntitiesOfClass(ItemEntity.class, new AABB(cell).inflate(4), e -> e.getItem().is(ModItems.ROYAL_JELLY)).size()
			);
			check(jelly > 0, "sealing a cell should leave royal jelly");

			int babiesBefore = babyBees(server);
			server.runOnServer(s -> {
				for (int i = 0; i < 64 && hiveLevel(s).getBlockState(cell).getValue(BroodCellBlock.STAGE) == BroodCellBlock.CAPPED; i++) {
					hiveLevel(s).getBlockState(cell).randomTick(hiveLevel(s), cell, hiveLevel(s).getRandom());
				}
			});
			check(cellState(server, cell).getValue(BroodCellBlock.STAGE) == BroodCellBlock.EGG, "a capped cell should hatch and get a new egg");
			check(babyBees(server) > babiesBefore, "hatching should release a baby bee");

			// A portrait of the nursery with cells in every state.
			int k = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == dz && dx != 0) {
						continue;
					}
					BroodCellBlock.Need need = BroodCellBlock.Need.values()[k % 3];
					int stage = k % 4;
					setCell(server, cell.offset(dx, 0, dz), stage, stage == BroodCellBlock.CAPPED ? BroodCellBlock.Need.NONE : need);
					k++;
				}
			}
			server.runCommand("gamemode spectator @a");
			photo(ctx, server, "hivemind_9c_nursery", cell.getX() + 0.5, cell.getY() + 1.6, cell.getZ() + 3.8, cell.getX() + 0.5, cell.getY() + 1.0, cell.getZ() + 0.5);
			server.runCommand("gamemode survival @a");

			// Walk out through the exit.
			teleport(server, HiveLayout.arrivalPos(0).x, HiveLayout.arrivalPos(0).y, HiveLayout.arrivalPos(0).z, 0.0F);
			ctx.waitTicks(20);
			ctx.getInput().lookAt(HiveLayout.exitPos(0));
			ctx.waitTicks(5);
			ctx.takeScreenshot("hivemind_10_exit");
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitFor(mc -> mc.level != null && mc.level.dimension().equals(Level.OVERWORLD), 200);
			world.getConnection().waitForChunksRender();
			ctx.waitTicks(20);
			ctx.takeScreenshot("hivemind_11_back_home");

			// Back outside: try out the bee multitool. The swap-hands key changes its mode.
			BlockPos home = server.computeOnServer(s -> player(s).blockPosition());
			BlockPos log = home.east(2);
			BlockPos grass = home.west(2);
			BlockPos dirt = home.south(2);
			server.runOnServer(s -> {
				ServerLevel overworld = s.overworld();
				overworld.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
				overworld.setBlock(grass, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				overworld.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
				for (BlockPos pos : List.of(log, grass, dirt)) {
					overworld.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
				}
			});
			server.runCommand("effect clear @p");
			server.runCommand("clear @p");
			server.runCommand("item replace entity @p weapon.mainhand with hivemind:bee_multitool");
			ctx.waitTicks(5);
			check(toolMode(server) == BeeToolMode.SWORD, "a new multitool starts in sword mode");
			ctx.getInput().pressKey(options -> options.keySwapOffhand);
			ctx.waitTicks(5);
			check(toolMode(server) == BeeToolMode.AXE, "pressing swap-hands should switch to axe mode");
			check(server.computeOnServer(s -> player(s).getOffhandItem().isEmpty()), "the multitool should stay in the main hand");
			ctx.takeScreenshot("hivemind_12_multitool_axe");
			ctx.getInput().lookAt(log);
			ctx.waitTicks(3);
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitTicks(5);
			check(server.computeOnServer(s -> s.overworld().getBlockState(log).is(Blocks.STRIPPED_OAK_LOG)), "axe mode should strip logs");

			ctx.getInput().pressKey(options -> options.keySwapOffhand);
			ctx.waitTicks(5);
			check(toolMode(server) == BeeToolMode.SHOVEL, "next mode should be shovel");
			ctx.getInput().lookAt(grass);
			ctx.waitTicks(3);
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitTicks(5);
			check(server.computeOnServer(s -> s.overworld().getBlockState(grass).is(Blocks.DIRT_PATH)), "shovel mode should make paths");

			ctx.getInput().pressKey(options -> options.keySwapOffhand);
			ctx.waitTicks(5);
			check(toolMode(server) == BeeToolMode.HOE, "next mode should be hoe");
			ctx.getInput().lookAt(dirt);
			ctx.waitTicks(3);
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitTicks(5);
			check(server.computeOnServer(s -> s.overworld().getBlockState(dirt).is(Blocks.FARMLAND)), "hoe mode should till");
			ctx.getInput().pressKey(options -> options.keySwapOffhand);
			ctx.waitTicks(5);
			check(toolMode(server) == BeeToolMode.SWORD, "modes should cycle back to sword");

			// Throw a beenade at a spider and watch the swarm go for it.
			BlockPos spiderPos = home.north(7);
			server.runCommand(
				"summon minecraft:spider " + (spiderPos.getX() + 0.5) + " " + spiderPos.getY() + " " + (spiderPos.getZ() + 0.5) + " {NoAI:1b,PersistenceRequired:1b,Tags:[\"target\"]}"
			);
			server.runCommand("item replace entity @p weapon.mainhand with hivemind:beenade 4");
			ctx.getInput().lookAt(spiderPos.above());
			ctx.waitTicks(5);
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitTicks(30);
			int swarm = swarmBees(server);
			check(swarm >= 3, "a beenade should release a swarm, found " + swarm);
			boolean hunting = server.computeOnServer(
				s -> s.overworld().getEntitiesOfClass(Bee.class, new AABB(home).inflate(24), b -> b.entityTags().contains(BeeSwarms.SWARM_TAG))
					.stream()
					.anyMatch(b -> b.getTarget() instanceof Spider)
			);
			check(hunting, "swarm bees should go after the spider");
			ctx.takeScreenshot("hivemind_13_beenade_swarm");
			ctx.waitTicks(100);
			boolean stung = server.computeOnServer(
				s -> s.overworld().getEntitiesOfClass(Spider.class, new AABB(home).inflate(24), sp -> sp.entityTags().contains("target"))
					.stream()
					.allMatch(sp -> sp.getHealth() < sp.getMaxHealth())
			);
			check(stung, "the swarm should have stung the spider");
			server.runCommand("kill @e[type=minecraft:spider]");
			server.runCommand("kill @e[type=minecraft:bee]");

			// Bee armor: wild bees leave you alone, and attackers get a swarm sent at them.
			server.runCommand("item replace entity @p armor.head with hivemind:bee_headgear");
			server.runCommand("item replace entity @p armor.chest with hivemind:bee_breastplate");
			server.runCommand("item replace entity @p armor.legs with hivemind:bee_greaves");
			server.runCommand("item replace entity @p armor.feet with hivemind:bee_boots");
			server.runCommand("item replace entity @p weapon.mainhand with hivemind:bee_multitool");
			ctx.waitTicks(5);
			check(server.computeOnServer(s -> BeeArmor.hasFullSet(player(s))), "full bee armor should count as a set");
			boolean stingBlocked = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				Bee bee = EntityTypes.BEE.create(s.overworld(), EntitySpawnReason.COMMAND);
				bee.snapTo(player.getX(), player.getY() + 1, player.getZ());
				s.overworld().addFreshEntity(bee);
				float before = player.getHealth();
				player.hurtServer(s.overworld(), s.overworld().damageSources().sting(bee), 4.0F);
				bee.discard();
				return player.getHealth() == before;
			});
			check(stingBlocked, "bees shouldn't be able to sting someone in full bee armor");
			int swarmBefore = swarmBees(server);
			server.runOnServer(s -> {
				ServerPlayer player = player(s);
				Zombie zombie = EntityTypes.ZOMBIE.create(s.overworld(), EntitySpawnReason.COMMAND);
				zombie.snapTo(player.getX() + 3, player.getY(), player.getZ());
				zombie.setNoAi(true);
				s.overworld().addFreshEntity(zombie);
				player.hurtServer(s.overworld(), s.overworld().damageSources().mobAttack(zombie), 2.0F);
			});
			ctx.waitTicks(5);
			check(swarmBees(server) > swarmBefore, "getting hit in full bee armor should release bees at the attacker");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			ctx.waitTicks(20);
			ctx.takeScreenshot("hivemind_14_bee_armor");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			// Every new recipe should have loaded.
			for (String recipe : List.of(
				"beenade", "bee_headgear", "bee_breastplate", "bee_greaves", "bee_boots", "bee_multitool", "honey_toast", "honeyed_apple", "honeycomb_candy",
				"honey_glazed_ham"
			)) {
				boolean loaded = server.computeOnServer(s -> s.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Hivemind.id(recipe))).isPresent());
				check(loaded, "recipe " + recipe + " should load");
			}
			Hivemind.LOGGER.info("Hivemind client game test passed");
		}
	}

	private static void setCell(final TestServerContext server, final BlockPos pos, final int stage, final BroodCellBlock.Need need) {
		server.runOnServer(
			s -> hiveLevel(s).setBlock(pos, ModBlocks.BROOD_CELL.defaultBlockState().setValue(BroodCellBlock.STAGE, stage).setValue(BroodCellBlock.NEED, need), 3)
		);
	}

	private static BlockState cellState(final TestServerContext server, final BlockPos pos) {
		return server.computeOnServer(s -> hiveLevel(s).getBlockState(pos));
	}

	private static int babyBees(final TestServerContext server) {
		return server.computeOnServer(s -> hiveLevel(s).getEntitiesOfClass(Bee.class, HiveLayout.bounds(0), Bee::isBaby).size());
	}

	private static int swarmBees(final TestServerContext server) {
		return server.computeOnServer(
			s -> s.overworld().getEntitiesOfClass(Bee.class, new AABB(player(s).blockPosition()).inflate(32), b -> b.entityTags().contains(BeeSwarms.SWARM_TAG)).size()
		);
	}

	private static BeeToolMode toolMode(final TestServerContext server) {
		return server.computeOnServer(s -> {
			ItemStack held = player(s).getMainHandItem();
			check(held.is(ModItems.BEE_MULTITOOL), "expected the multitool in hand, found " + held);
			return BeeMultitoolItem.getMode(held);
		});
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static ServerPlayer player(final MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static ServerLevel hiveLevel(final MinecraftServer server) {
		return server.getLevel(HiveLayout.HIVE_LEVEL);
	}

	private static QueenBee queen(final MinecraftServer server) {
		List<QueenBee> queens = hiveLevel(server).getEntitiesOfClass(QueenBee.class, HiveLayout.bounds(0));
		return queens.getFirst();
	}

	private static int countGifts(final ServerPlayer player) {
		int count = 0;
		for (var item : List.of(
			Items.EMERALD, Items.BONE_MEAL, Items.HONEY_BOTTLE, Items.HONEYCOMB, Items.HONEY_BLOCK, ModItems.SHRINKING_HONEY, ModItems.BEENADE, ModItems.ROYAL_JELLY,
			ModItems.HONEYCOMB_CANDY
		)) {
			count += player.getInventory().countItem(item);
		}
		return count;
	}

	/** Places the player (eyes at standing height) at x,y,z looking at the target point, then screenshots. */
	private static void photo(
		final ClientGameTestContext ctx, final TestServerContext server, final String name, final double x, final double y, final double z, final double lx, final double ly,
		final double lz
	) {
		aim(server, x, y, z, lx, ly, lz);
		ctx.waitTicks(25);
		ctx.takeScreenshot(name);
	}

	private static void aim(final TestServerContext server, final double x, final double y, final double z, final double lx, final double ly, final double lz) {
		double dx = lx - x;
		double dy = ly - (y + 1.62);
		double dz = lz - z;
		float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float)-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runOnServer(s -> player(s).teleportTo(hiveLevel(s), x, y, z, java.util.Set.of(), yaw, pitch, false));
	}

	private static void teleport(final TestServerContext server, final double x, final double y, final double z, final float yaw) {
		server.runOnServer(s -> player(s).teleportTo(hiveLevel(s), x, y, z, java.util.Set.of(), yaw, 0.0F, false));
	}
}
