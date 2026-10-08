package com.hivemind.test;

import com.hivemind.Hivemind;
import com.hivemind.entity.GuardBee;
import com.hivemind.entity.QueenBee;
import com.hivemind.registry.ModBlocks;
import com.hivemind.registry.ModEffects;
import com.hivemind.world.HiveLayout;
import com.hivemind.world.HiveRaids;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

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
			Hivemind.LOGGER.info("Hivemind client game test passed");
		}
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
		for (var item : List.of(Items.EMERALD, Items.BONE_MEAL, Items.HONEY_BOTTLE, Items.HONEYCOMB, Items.HONEY_BLOCK, com.hivemind.registry.ModItems.SHRINKING_HONEY)) {
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
