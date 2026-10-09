package com.hivemind.mixin;

import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Bee.class)
public interface BeeAccessor {
	/** Lets a royal swarm bee grow its stinger back. */
	@Invoker("setHasStung")
	void hivemind$setHasStung(boolean hasStung);
}
