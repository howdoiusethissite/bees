package com.hivemind.mixin;

import com.hivemind.HivemindConfig;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Bees pick up nectar faster (see {@code pollinationTimeMultiplier}). */
@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeePollinateGoal")
public abstract class BeePollinateGoalMixin {
	@ModifyConstant(method = "hasPollinatedLongEnough", constant = @Constant(intValue = 400))
	private int hivemind$pollinateFaster(final int ticks) {
		return Mth.floor(ticks * HivemindConfig.get().pollinationTimeMultiplier);
	}
}
