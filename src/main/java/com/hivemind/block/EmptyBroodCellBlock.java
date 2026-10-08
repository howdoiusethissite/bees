package com.hivemind.block;

import com.hivemind.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A nursery cell after its bee has hatched. It stays empty until the queen comes by and lays a new egg in it. */
public class EmptyBroodCellBlock extends Block {
	public EmptyBroodCellBlock(final Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.hivemind.brood.empty").withStyle(ChatFormatting.YELLOW));
		}
		return InteractionResult.SUCCESS;
	}

	/** The queen lays an egg here. */
	public static void layEgg(final ServerLevel level, final BlockPos pos) {
		level.setBlock(pos, ModBlocks.BROOD_CELL.defaultBlockState(), Block.UPDATE_CLIENTS);
	}
}
