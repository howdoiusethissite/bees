package com.hivemind.item;

import com.hivemind.block.RoyalCradleBlock;
import com.hivemind.registry.ModBlocks;
import com.hivemind.world.HiveData;
import com.hivemind.world.HiveInfluence;
import com.hivemind.world.HiveLayout;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A new queen, still in her egg. Plant it on the ground and the bees build a grand hive around her:
 * a big skep you can walk straight into, no shrinking needed, where the queen adores you from day one.
 */
public class RoyalEggItem extends Item {
	public RoyalEggItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(final UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		if (HiveLayout.isHiveLevel(level)) {
			player.sendOverlayMessage(Component.translatable("message.hivemind.royal_egg.inside").withStyle(ChatFormatting.YELLOW));
			return InteractionResult.FAIL;
		}
		BlockPlaceContext place = new BlockPlaceContext(context);
		BlockPos pos = place.getClickedPos();
		if (!place.canPlace() || context.getClickedFace() != Direction.UP) {
			player.sendOverlayMessage(Component.translatable("message.hivemind.royal_egg.ground").withStyle(ChatFormatting.YELLOW));
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel serverLevel) {
			// The door ends up facing whoever planted it.
			Direction front = player.getDirection().getOpposite();
			serverLevel.setBlockAndUpdate(pos, ModBlocks.ROYAL_CRADLE.defaultBlockState().setValue(RoyalCradleBlock.FACING, front));
			serverLevel.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
			serverLevel.playSound(null, pos, SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
			HiveData.get(serverLevel.getServer()).createGrand(serverLevel.dimension(), pos, player.getUUID());
			// Founding a hive makes you royalty as far as the bees are concerned.
			HiveInfluence.onFavor(null, player, HiveInfluence.MAX_INFLUENCE);
			player.sendSystemMessage(Component.translatable("message.hivemind.royal_egg.planted").withStyle(ChatFormatting.GOLD));
		}
		context.getItemInHand().consume(1, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(
		final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag
	) {
		tooltip.accept(Component.translatable("item.hivemind.royal_egg.hint").withStyle(ChatFormatting.GRAY));
	}
}
