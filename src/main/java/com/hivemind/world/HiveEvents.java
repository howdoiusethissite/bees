package com.hivemind.world;

import com.hivemind.entity.QueenBee;
import com.hivemind.registry.ModBlocks;
import com.hivemind.registry.ModEffects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Hooks that make hives enterable and keep the bees protective of their home. */
public final class HiveEvents {
	/** How long the bees remember a warning. A second slip inside this window and they attack. */
	private static final int WARNING_MEMORY = 20 * 30;
	/**
	 * Right after a warning, a held-down mouse button keeps "breaking" the same block every few ticks.
	 * Anything in this window still counts as the same slip.
	 */
	private static final int WARNING_GRACE = 20 * 2;
	/** Once a hive has turned on someone, how long further hits count without a fresh warning. */
	private static final int HOSTILE_TIME = 20 * 60;
	private static final Map<UUID, Long> WARNED_AT = new HashMap<>();
	private static final Map<UUID, Long> HOSTILE_UNTIL = new HashMap<>();

	private HiveEvents() {
	}

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);

			if (HiveLayout.isHiveLevel(level)) {
				if (state.is(ModBlocks.HIVE_EXIT) || player.isCreative()) {
					return InteractionResult.PASS;
				}
				ItemStack held = player.getItemInHand(hand);
				if (held.getItem() instanceof BlockItem || held.getItem() instanceof BucketItem) {
					player.sendOverlayMessage(Component.translatable("message.hivemind.no_building").withStyle(ChatFormatting.YELLOW));
					return InteractionResult.FAIL;
				}
				return InteractionResult.PASS;
			}

			if (state.is(BlockTags.BEEHIVES) && player.hasEffect(ModEffects.SHRUNK) && !player.isSecondaryUseActive()) {
				if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
					HiveTravel.enterHive(serverPlayer, serverLevel, pos.immutable());
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!HiveLayout.isHiveLevel(level) || player.isCreative()) {
				return true;
			}
			// The block never actually breaks. The first time is let off as an accident.
			if (level instanceof ServerLevel serverLevel) {
				offend(serverLevel, player, "message.hivemind.no_breaking");
			}
			return false;
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!HiveLayout.isHiveLevel(entity.level()) || entity instanceof QueenBee || !entity.entityTags().contains(HiveInteriorBuilder.RESIDENT_TAG)) {
				return true;
			}
			if (source.getEntity() instanceof Player player && !player.isCreative() && entity.level() instanceof ServerLevel serverLevel) {
				// A stray swing during a raid is easy to make. Only a second one in a row counts.
				return offend(serverLevel, player, "message.hivemind.bee_hit");
			}
			return true;
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (!HiveLayout.isHiveLevel(entity.level()) || !entity.entityTags().contains(HiveInteriorBuilder.RESIDENT_TAG)) {
				return;
			}
			if (source.getEntity() instanceof Player player && !player.isCreative() && entity.level() instanceof ServerLevel serverLevel) {
				angerHive(serverLevel, player);
			}
		});

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (HiveLayout.isHiveLevel(level)) {
				HiveRaids.tick(level);
			}
		});
	}

	/**
	 * Called when a player breaks a hive rule (breaking a block, hitting a resident). The first slip is
	 * forgiven with a warning; doing it again within {@link #WARNING_MEMORY} sets the whole hive on them.
	 * Once the hive is angry, further offenses go straight through.
	 *
	 * @return true if the hive is (now) hostile to the player, false if this one was let off with a warning
	 */
	public static boolean offend(final ServerLevel level, final Player player, final String angryMessage) {
		if (player.isCreative() || player.isSpectator()) {
			return true;
		}
		long now = level.getGameTime();
		Long hostile = HOSTILE_UNTIL.get(player.getUUID());
		if (hostile != null && now < hostile) {
			angerHive(level, player);
			return true;
		}
		Long warned = WARNED_AT.get(player.getUUID());
		if (warned == null || now - warned > WARNING_MEMORY) {
			WARNED_AT.put(player.getUUID(), now);
			player.sendOverlayMessage(Component.translatable("message.hivemind.warning").withStyle(ChatFormatting.YELLOW));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 0.8F, 1.4F);
			QueenBee queen = queenNear(level, player);
			if (queen != null) {
				queen.say(player, "warn");
			}
			return false;
		}
		if (now - warned <= WARNING_GRACE) {
			return false;
		}
		WARNED_AT.remove(player.getUUID());
		HOSTILE_UNTIL.put(player.getUUID(), now + HOSTILE_TIME);
		angerHive(level, player);
		player.sendOverlayMessage(Component.translatable(angryMessage).withStyle(ChatFormatting.RED));
		return true;
	}

	private static @Nullable QueenBee queenNear(final ServerLevel level, final Player player) {
		int index = HiveLayout.indexAt(player.position());
		if (index < 0) {
			return null;
		}
		List<QueenBee> queens = level.getEntitiesOfClass(QueenBee.class, HiveLayout.bounds(index), QueenBee::isAlive);
		return queens.isEmpty() ? null : queens.getFirst();
	}

	/** Every bee in the player's hive turns on them, and the queen thinks a little less of them. */
	public static void angerHive(final ServerLevel level, final Player player) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		int index = HiveLayout.indexAt(player.position());
		if (index < 0) {
			return;
		}
		for (Mob mob : level.getEntitiesOfClass(Mob.class, HiveLayout.bounds(index), m -> m.entityTags().contains(HiveInteriorBuilder.RESIDENT_TAG))) {
			if (mob instanceof QueenBee queen) {
				queen.adjustFavor(player, -3);
			} else if (mob instanceof NeutralMob neutral) {
				mob.setTarget(player);
				neutral.setPersistentAngerTarget(EntityReference.<LivingEntity>of(player));
				neutral.startPersistentAngerTimer();
			}
		}
	}
}
