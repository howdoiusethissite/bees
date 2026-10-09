package com.hivemind.block;

import com.hivemind.HivemindConfig;
import com.hivemind.world.GrandHiveBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A planted royal egg. Bees gather round and slowly build a grand hive over it; once it's grown
 * (see {@code grandHiveGrowMinutes}) the cradle is replaced by the finished hive, door facing
 * the way {@link #FACING} points.
 */
public class RoyalCradleBlock extends Block {
	public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
	public static final int MAX_AGE = BlockStateProperties.MAX_AGE_4;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	public RoyalCradleBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(FACING, Direction.SOUTH));
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE, FACING);
	}

	/** Ticks between growth stages. */
	public static int stageTicks() {
		return Math.max(1, Mth.floor(HivemindConfig.get().grandHiveGrowMinutes * 60 * 20 / (MAX_AGE + 1)));
	}

	@Override
	protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
		if (!oldState.is(this)) {
			level.scheduleTick(pos, this, stageTicks());
		}
	}

	@Override
	protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
		int age = state.getValue(AGE);
		if (age < MAX_AGE) {
			level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_ALL);
			level.scheduleTick(pos, this, stageTicks());
			level.playSound(null, pos, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.5F, 0.8F + age * 0.1F);
			level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20 + age * 10, 1.5, 1.0, 1.5, 0.05);
			return;
		}
		GrandHiveBuilder.build(level, pos, state.getValue(FACING));
	}

	@Override
	public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
		// The builders at work: honey drips and a busy buzz of wax that grows as the hive does.
		int age = state.getValue(AGE);
		for (int i = 0; i <= age; i++) {
			if (random.nextInt(2) == 0) {
				level.addParticle(
					ParticleTypes.WAX_ON,
					pos.getX() + 0.5 + (random.nextDouble() - 0.5) * (2 + age),
					pos.getY() + random.nextDouble() * (1 + age),
					pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * (2 + age),
					0.0,
					0.02,
					0.0
				);
			}
		}
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.DRIPPING_HONEY, pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
		}
	}
}
