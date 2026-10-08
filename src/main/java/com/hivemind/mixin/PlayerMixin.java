package com.hivemind.mixin;

import com.hivemind.item.BeeMultitoolItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sweeping attacks are for swords only, and the bee multitool counts as one while it's in sword mode. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@WrapOperation(method = "isSweepAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean hivemind$multitoolSweeps(final ItemStack stack, final TagKey<Item> tag, final Operation<Boolean> original) {
		return original.call(stack, tag) || BeeMultitoolItem.isSwordMode(stack);
	}
}
