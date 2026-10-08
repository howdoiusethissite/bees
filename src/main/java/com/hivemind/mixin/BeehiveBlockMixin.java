package com.hivemind.mixin;

import com.hivemind.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An empty bucket on a full beehive scoops out a bucket of liquid honey, the same way a bottle gets a honey bottle. */
@Mixin(BeehiveBlock.class)
public abstract class BeehiveBlockMixin {
	@Shadow
	private void angerNearbyBees(final Level level, final BlockPos pos) {
	}

	@Shadow
	private boolean hiveContainsBees(final Level level, final BlockPos pos) {
		return false;
	}

	@Shadow
	public abstract void releaseBeesAndResetHoneyLevel(
		Level level, BlockState state, BlockPos pos, Player player, BeehiveBlockEntity.BeeReleaseStatus releaseStatus
	);

	@Shadow
	public abstract void resetHoneyLevel(Level level, BlockState state, BlockPos pos);

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void hivemind$bucketHoney(
		final ItemStack itemStack,
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final Player player,
		final InteractionHand hand,
		final BlockHitResult hitResult,
		final CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!itemStack.is(Items.BUCKET) || state.getValue(BeehiveBlock.HONEY_LEVEL) < BeehiveBlock.MAX_HONEY_LEVELS) {
			return;
		}
		level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
		player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(ModItems.HONEY_BUCKET)));
		level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		if (!CampfireBlock.isSmokeyPos(level, pos)) {
			if (this.hiveContainsBees(level, pos)) {
				this.angerNearbyBees(level, pos);
			}
			this.releaseBeesAndResetHoneyLevel(level, state, pos, player, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
		} else {
			this.resetHoneyLevel(level, state, pos);
		}
		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
