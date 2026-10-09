package com.hivemind.mixin;

import com.hivemind.world.HiveHoney;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bee bringing nectar home to a hive that already shows five honey levels adds to the hive's reserve instead. */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin {
	@Inject(
		method = "releaseOccupant",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;dropOffNectar()V")
	)
	private static void hivemind$storeExtraHoney(
		final Level level,
		final BlockPos pos,
		final BlockState state,
		final BeehiveBlockEntity.Occupant occupant,
		final List<Entity> releasedBees,
		final BeehiveBlockEntity.BeeReleaseStatus status,
		final BlockPos flowerPos,
		final CallbackInfoReturnable<Boolean> cir
	) {
		if (state.hasProperty(BeehiveBlock.HONEY_LEVEL) && state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS) {
			HiveHoney.onDeliveredToFullHive(level, pos);
		}
	}
}
