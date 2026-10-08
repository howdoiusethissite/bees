package com.hivemind.world;

import com.hivemind.entity.QueenBee;
import com.hivemind.registry.ModBlocks;
import com.hivemind.registry.ModEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

/** Hooks that make hives enterable and keep the bees protective of their home. */
public final class HiveEvents {
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
			if (level instanceof ServerLevel serverLevel) {
				angerHive(serverLevel, player);
				player.sendOverlayMessage(Component.translatable("message.hivemind.no_breaking").withStyle(ChatFormatting.RED));
			}
			return false;
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
