package com.hivemind.fluid;

import com.hivemind.world.HiveLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/** The block form of liquid honey. Keeps track of how many bottles have been scooped out of a source. */
public class HoneyLiquidBlock extends LiquidBlock {
	public static final IntegerProperty BOTTLES_TAKEN = IntegerProperty.create("bottles_taken", 0, HoneyFluid.BOTTLES_PER_SOURCE - 1);

	public HoneyLiquidBlock(final FlowingFluid fluid, final Properties properties) {
		super(fluid, properties);
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BOTTLES_TAKEN);
	}

	@Override
	protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
		if (!this.setIfTouchingWaterOrLava(level, pos, state)) {
			super.onPlace(state, level, pos, oldState, movedByPiston);
		}
	}

	@Override
	protected void neighborChanged(
		final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
	) {
		if (!this.setIfTouchingWaterOrLava(level, pos, state)) {
			super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
		}
	}

	/** Honey meeting water sets into a honey block; honey meeting lava crisps into honeycomb. */
	private boolean setIfTouchingWaterOrLava(final Level level, final BlockPos pos, final BlockState state) {
		if (level.isClientSide()) {
			return false;
		}
		for (Direction direction : Direction.values()) {
			FluidState neighbor = level.getFluidState(pos.relative(direction));
			BlockState result = neighbor.is(FluidTags.LAVA) ? Blocks.HONEYCOMB_BLOCK.defaultBlockState()
				: neighbor.is(FluidTags.WATER) ? Blocks.HONEY_BLOCK.defaultBlockState()
				: null;
			if (result != null) {
				level.setBlockAndUpdate(pos, result);
				level.playSound(null, pos, neighbor.is(FluidTags.LAVA) ? SoundEvents.LAVA_EXTINGUISH : SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
				return true;
			}
		}
		return false;
	}

	@Override
	public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
		if (random.nextInt(40) == 0 && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()) {
			level.addParticle(
				ParticleTypes.WAX_ON, pos.getX() + random.nextDouble(), pos.getY() + 0.95, pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0
			);
		}
	}

	/**
	 * Scoops a bottle's worth out of a honey source. Returns false if there's no source here. The source
	 * is gone once its last bottle is taken, except for the honey springs inside hives, which the bees
	 * keep topped up.
	 */
	public static boolean takeBottle(final ServerLevel level, final BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof HoneyLiquidBlock) || !state.getFluidState().isSource()) {
			return false;
		}
		if (HiveLayout.isHiveLevel(level)) {
			return true;
		}
		int taken = state.getValue(BOTTLES_TAKEN) + 1;
		if (taken >= HoneyFluid.BOTTLES_PER_SOURCE) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		} else {
			level.setBlock(pos, state.setValue(BOTTLES_TAKEN, taken), Block.UPDATE_CLIENTS);
		}
		return true;
	}
}
