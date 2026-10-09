package com.hivemind.world;

import com.hivemind.HivemindConfig;
import com.hivemind.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets beehives hold more than the five honey levels vanilla can show. Once a hive looks full, every
 * further delivery goes into a reserve kept on the hive's block entity, up to the configured limit.
 * Harvesting a full hive pays out a bonus that grows with the reserve, then empties it.
 */
public final class HiveHoney {
	private HiveHoney() {
	}

	/** The most a hive can keep in reserve on top of the five visible levels. */
	public static int maxReserve() {
		return HivemindConfig.get().maxHiveHoney - BeehiveBlock.MAX_HONEY_LEVELS;
	}

	public static int reserve(final Level level, final BlockPos pos) {
		return level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive ? hive.getAttachedOrElse(ModAttachments.HONEY_RESERVE, 0) : 0;
	}

	/** Total honey in the hive: the visible levels plus the reserve. */
	public static int total(final Level level, final BlockPos pos, final BlockState state) {
		return state.getValue(BeehiveBlock.HONEY_LEVEL) + reserve(level, pos);
	}

	/** A bee just delivered nectar to a hive that already looks full. */
	public static void onDeliveredToFullHive(final Level level, final BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive) {
			int reserve = hive.getAttachedOrElse(ModAttachments.HONEY_RESERVE, 0);
			if (reserve < maxReserve()) {
				hive.setAttached(ModAttachments.HONEY_RESERVE, reserve + 1);
			}
		}
	}

	public static void clear(final Level level, final BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive && hive.hasAttached(ModAttachments.HONEY_RESERVE)) {
			hive.removeAttached(ModAttachments.HONEY_RESERVE);
		}
	}

	/** Honeycomb on top of vanilla's three when a full hive is sheared. */
	public static int bonusHoneycomb(final int reserve) {
		HivemindConfig.Values config = HivemindConfig.get();
		return config.shearsBonusHoneycomb + Mth.floor(reserve * config.shearsHoneycombPerStoredHoney);
	}

	/** Honey bottles on top of vanilla's one when a full hive is bottled. */
	public static int bonusBottles(final int reserve) {
		return HivemindConfig.get().bottleBonusHoney + reserveBottles(reserve);
	}

	/** The part of the bottle bonus that comes from the reserve alone. A bucket gets this on top of the bucket. */
	public static int reserveBottles(final int reserve) {
		return Mth.floor(reserve * HivemindConfig.get().bottleHoneyPerStoredHoney);
	}

	/** Pops the bonus honeycomb out of the hive, the same way vanilla's three come out. */
	public static void dropBonusHoneycomb(final ServerLevel level, final BlockPos pos, final int count) {
		for (int left = count; left > 0; left -= 64) {
			Block.popResource(level, pos, new ItemStack(Items.HONEYCOMB, Math.min(64, left)));
		}
	}

	/** Hands the player bonus honey bottles, dropping whatever doesn't fit. */
	public static void giveBonusBottles(final Player player, final int count) {
		for (int left = count; left > 0; left -= 16) {
			player.getInventory().placeItemBackInInventory(new ItemStack(Items.HONEY_BOTTLE, Math.min(16, left)), Prediction.SERVER_ONLY);
		}
	}

	/** Tells the player how much honey a hive is holding. */
	public static void report(final Player player, final Level level, final BlockPos pos, final BlockState state) {
		player.sendOverlayMessage(
			Component.translatable("message.hivemind.hive_honey", total(level, pos, state), HivemindConfig.get().maxHiveHoney).withStyle(ChatFormatting.GOLD)
		);
	}
}
