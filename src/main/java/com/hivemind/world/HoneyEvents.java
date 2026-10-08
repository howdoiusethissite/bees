package com.hivemind.world;

import com.hivemind.fluid.HoneyLiquidBlock;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bottling liquid honey. */
public final class HoneyEvents {
	private HoneyEvents() {
	}

	public static void init() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack held = player.getItemInHand(hand);
			if (!held.is(Items.GLASS_BOTTLE)) {
				return InteractionResult.PASS;
			}
			Vec3 eye = player.getEyePosition();
			Vec3 reach = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
			BlockHitResult hit = level.clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
			if (hit.getType() != HitResult.Type.BLOCK || !(level.getBlockState(hit.getBlockPos()).getBlock() instanceof HoneyLiquidBlock)) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel serverLevel && HoneyLiquidBlock.takeBottle(serverLevel, hit.getBlockPos())) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 0.9F);
				level.gameEvent(player, GameEvent.FLUID_PICKUP, hit.getBlockPos());
				player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
				player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.HONEY_BOTTLE)));
			}
			return InteractionResult.SUCCESS;
		});
	}
}
