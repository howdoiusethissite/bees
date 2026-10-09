package com.hivemind.world;

import com.hivemind.Hivemind;
import com.hivemind.registry.ModEffects;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Moving players into and out of hive interiors. */
public final class HiveTravel {
	private HiveTravel() {
	}

	public static void enterHive(final ServerPlayer player, final ServerLevel fromLevel, final BlockPos hivePos) {
		ServerLevel hiveLevel = player.level().getServer().getLevel(HiveLayout.HIVE_LEVEL);
		if (hiveLevel == null) {
			Hivemind.LOGGER.error("Hive dimension is missing; is the mod's data pack disabled?");
			return;
		}

		HiveData data = HiveData.get(hiveLevel.getServer());
		HiveData.Hive hive = data.getOrCreate(fromLevel.dimension(), hivePos);
		enter(player, fromLevel, hiveLevel, data, hive, hivePos);
	}

	/** Walking in through the door of a grand hive grown from a royal egg. No shrinking needed; it's big enough. */
	public static void enterGrandHive(final ServerPlayer player, final ServerLevel fromLevel, final BlockPos doorPos) {
		ServerLevel hiveLevel = player.level().getServer().getLevel(HiveLayout.HIVE_LEVEL);
		if (hiveLevel == null) {
			Hivemind.LOGGER.error("Hive dimension is missing; is the mod's data pack disabled?");
			return;
		}
		HiveData data = HiveData.get(hiveLevel.getServer());
		Optional<HiveData.Hive> hive = data.findGrand(fromLevel.dimension(), doorPos, GrandHiveBuilder.DOOR_SEARCH_RANGE);
		if (hive.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.hivemind.grand_hive_lost").withStyle(ChatFormatting.YELLOW));
			return;
		}
		enter(player, fromLevel, hiveLevel, data, hive.get(), doorPos);
	}

	private static void enter(
		final ServerPlayer player, final ServerLevel fromLevel, final ServerLevel hiveLevel, final HiveData data, HiveData.Hive hive, final BlockPos hivePos
	) {
		if (!hive.isBuilt()) {
			HiveInteriorBuilder.build(hiveLevel, hive.index(), hive.founder().orElse(null));
			hive = data.markUpgraded(hive);
		} else if (hive.version() < HiveInteriorBuilder.LAYOUT_VERSION) {
			HiveInteriorBuilder.upgrade(hiveLevel, hive.index(), hive.version());
			hive = data.markUpgraded(hive);
		}
		// The queen's flowers are always back in their pots, and two of them hold whatever she's craving.
		HiveInteriorBuilder.refillFlowerPots(hiveLevel, hive.index(), QueenCravings.craving(hiveLevel.getServer(), hive.index()));

		// Remember where to put the player back. Step them out in front of the hive rather than inside the block.
		Vec3 back = player.position();
		data.setReturn(player.getUUID(), new HiveData.ReturnPoint(fromLevel.dimension(), back, player.getYRot()));

		// Inside, everything is already bee-sized, so the player goes back to their normal scale.
		player.removeEffect(ModEffects.SHRUNK);
		fromLevel.playSound(null, hivePos, SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);

		Vec3 arrival = HiveLayout.arrivalPos(hive.index());
		List<Bee> followers = BeeEscorts.nearbyFollowers(fromLevel, player);
		player.teleport(new TeleportTransition(hiveLevel, arrival, Vec3.ZERO, 180.0F, 0.0F, TeleportTransition.DO_NOTHING));
		BeeEscorts.bringAlong(followers, player);
		hiveLevel.playSound(null, BlockPos.containing(arrival), SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
		player.sendOverlayMessage(Component.translatable(hive.isGrand() ? "message.hivemind.entered_grand" : "message.hivemind.entered").withStyle(ChatFormatting.GOLD));
	}

	public static void leaveHive(final ServerPlayer player) {
		HiveData data = HiveData.get(player.level().getServer());
		Optional<HiveData.ReturnPoint> point = data.takeReturn(player.getUUID());
		TeleportTransition transition = point.map(p -> {
				ServerLevel level = player.level().getServer().getLevel(p.level());
				return level == null ? null : new TeleportTransition(level, p.pos(), Vec3.ZERO, p.yRot(), 0.0F, TeleportTransition.DO_NOTHING);
			})
			.orElseGet(() -> player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
		List<Bee> followers = BeeEscorts.nearbyFollowers(player.level(), player);
		player.teleport(transition);
		BeeEscorts.bringAlong(followers, player);
		transition.newLevel().playSound(null, BlockPos.containing(transition.position()), SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
	}
}
