package com.hivemind.mixin;

import com.hivemind.world.FlowerSpread;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets a bee that just finished pollinating a flower spread it a little. */
@Mixin(Bee.class)
public abstract class BeeMixin {
	@Inject(method = "setHasNectar", at = @At("HEAD"))
	private void hivemind$spreadFlower(final boolean hasNectar, final CallbackInfo ci) {
		Bee bee = (Bee)(Object)this;
		// tickCount is 0 while a bee is being loaded from disk or let out of a hive, which isn't pollinating.
		if (hasNectar && !bee.hasNectar() && bee.tickCount > 0 && bee.level() instanceof ServerLevel level && bee.hasSavedFlowerPos()) {
			FlowerSpread.onPollinated(level, bee, bee.getSavedFlowerPos());
		}
	}
}
