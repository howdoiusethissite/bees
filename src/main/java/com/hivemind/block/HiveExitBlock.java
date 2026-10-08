package com.hivemind.block;

import com.hivemind.world.HiveTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The way out of a hive interior. Right-click to return to where you came in. */
public class HiveExitBlock extends Block {
	public HiveExitBlock(final Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			HiveTravel.leaveHive(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
		if (random.nextInt(3) == 0) {
			level.addParticle(
				ParticleTypes.DRIPPING_HONEY,
				pos.getX() + random.nextDouble(),
				pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble(),
				0.0,
				0.0,
				0.0
			);
		}
	}
}
