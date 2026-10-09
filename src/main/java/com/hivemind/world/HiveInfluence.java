package com.hivemind.world;

import com.hivemind.entity.QueenBee;
import com.hivemind.registry.ModAttachments;
import com.hivemind.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A player's standing with the bees in general: the highest favor they've reached with any queen.
 * It never goes down, and each rank makes the bees fight harder for them.
 *
 * <ul>
 *   <li>Rank 1, Hive Friend: beenades and bee armor send an extra bee, and every bee hits harder.</li>
 *   <li>Rank 2, Honored Keeper: their bees grow their stingers back, so they keep fighting instead of dying after one sting.</li>
 *   <li>Rank 3, Royal Confidant: bigger, faster, longer-lived swarms, and a queen hands over a royal egg.</li>
 * </ul>
 */
public final class HiveInfluence {
	public static final int[] RANK_FAVOR = {0, 10, 25, 50};
	public static final int MAX_RANK = RANK_FAVOR.length - 1;
	public static final int MAX_INFLUENCE = RANK_FAVOR[MAX_RANK];

	private HiveInfluence() {
	}

	public static int influence(final Player player) {
		return player.getAttachedOrElse(ModAttachments.INFLUENCE, 0);
	}

	public static int rank(final Player player) {
		return rankFor(influence(player));
	}

	public static int rankFor(final int influence) {
		int rank = 0;
		for (int i = 1; i <= MAX_RANK; i++) {
			if (influence >= RANK_FAVOR[i]) {
				rank = i;
			}
		}
		return rank;
	}

	/** The rank of whoever let a swarm loose, or 0 if it wasn't a player. */
	public static int rankOf(final @Nullable Entity owner) {
		return owner instanceof Player player ? rank(player) : 0;
	}

	public static Component rankName(final int rank) {
		return Component.translatable("rank.hivemind." + rank);
	}

	/**
	 * Called whenever a queen's favor for a player goes up. Raises their influence to match and, if
	 * that takes them up a rank, celebrates it.
	 */
	public static void onFavor(final @Nullable QueenBee queen, final Player player, final int favor) {
		int before = influence(player);
		if (favor <= before) {
			return;
		}
		player.setAttached(ModAttachments.INFLUENCE, favor);
		int oldRank = rankFor(before);
		int newRank = rankFor(favor);
		for (int rank = oldRank + 1; rank <= newRank; rank++) {
			announce(queen, player, rank);
		}
	}

	private static void announce(final @Nullable QueenBee queen, final Player player, final int rank) {
		player.sendSystemMessage(
			Component.translatable("message.hivemind.rank_up", rankName(rank).copy().withStyle(ChatFormatting.LIGHT_PURPLE))
				.withStyle(ChatFormatting.GOLD)
		);
		player.sendSystemMessage(Component.translatable("message.hivemind.rank_up." + rank).withStyle(ChatFormatting.GRAY));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
		if (rank == MAX_RANK && !player.getAttachedOrElse(ModAttachments.ROYAL_EGG_GIVEN, false)) {
			player.setAttached(ModAttachments.ROYAL_EGG_GIVEN, true);
			player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.ROYAL_EGG), Prediction.SERVER_ONLY);
			player.sendSystemMessage(Component.translatable("message.hivemind.royal_egg_given").withStyle(ChatFormatting.LIGHT_PURPLE));
			if (queen != null) {
				queen.say(player, "royal_egg");
			}
		}
	}
}
