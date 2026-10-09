package com.hivemind.mixin;

import com.hivemind.HivemindConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bees spend less time inside the hive turning nectar into honey (see {@code hiveWorkTimeMultiplier}). */
@Mixin(BeehiveBlockEntity.Occupant.class)
public abstract class BeehiveOccupantMixin {
	@Inject(method = "of", at = @At("RETURN"), cancellable = true)
	private static void hivemind$shortenStay(final Entity entity, final CallbackInfoReturnable<BeehiveBlockEntity.Occupant> cir) {
		double multiplier = HivemindConfig.get().hiveWorkTimeMultiplier;
		if (multiplier == 1.0) {
			return;
		}
		BeehiveBlockEntity.Occupant occupant = cir.getReturnValue();
		// Always at least a second, so a bee going in is still seen going in.
		int ticks = Math.max(20, Mth.floor(occupant.minTicksInHive() * multiplier));
		cir.setReturnValue(new BeehiveBlockEntity.Occupant(occupant.entityData(), occupant.ticksInHive(), ticks));
	}
}
