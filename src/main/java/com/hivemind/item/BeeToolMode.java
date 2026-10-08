package com.hivemind.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.component.BlockTransformers;
import org.jspecify.annotations.Nullable;

/**
 * The bee multitool mines everything no matter what. Its mode decides how it fights and what
 * right-clicking a block does, the same way the matching diamond tool would.
 */
public enum BeeToolMode implements StringRepresentable {
	SWORD(0, "sword", 6.0F, -2.4F, 0.0F, null),
	AXE(1, "axe", 8.0F, -3.0F, 5.0F, BlockTransformers.AXE),
	SHOVEL(2, "shovel", 4.5F, -3.0F, 0.0F, BlockTransformers.SHOVEL),
	HOE(3, "hoe", 0.0F, 0.0F, 0.0F, BlockTransformers.HOE);

	public static final Codec<BeeToolMode> CODEC = StringRepresentable.fromValues(BeeToolMode::values);
	public static final IntFunction<BeeToolMode> BY_ID = ByIdMap.continuous(m -> m.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, BeeToolMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, m -> m.id);

	private final int id;
	private final String name;
	final float attackDamage;
	final float attackSpeed;
	final float disableBlockingSeconds;
	final @Nullable ResourceKey<BlockTransformer> transformer;

	BeeToolMode(
		final int id,
		final String name,
		final float attackDamage,
		final float attackSpeed,
		final float disableBlockingSeconds,
		final @Nullable ResourceKey<BlockTransformer> transformer
	) {
		this.id = id;
		this.name = name;
		this.attackDamage = attackDamage;
		this.attackSpeed = attackSpeed;
		this.disableBlockingSeconds = disableBlockingSeconds;
		this.transformer = transformer;
	}

	public BeeToolMode next() {
		return BY_ID.apply((this.id + 1) % values().length);
	}

	public String translationKey() {
		return "item.hivemind.bee_multitool.mode." + this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
