package com.hivemind.item;

import com.hivemind.registry.ModComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * One tool for everything. It digs whatever a diamond pickaxe, axe, shovel or hoe could, and the
 * swap-hands key (F by default) flips between sword, axe, shovel and hoe for fighting and right-clicking.
 */
public class BeeMultitoolItem extends Item {
	public static final int DURABILITY = 2400;
	private static final float MINING_SPEED = 8.0F;

	public BeeMultitoolItem(final Item.Properties properties) {
		super(properties);
	}

	/** Item properties for a fresh multitool, which starts out in sword mode. */
	public static Item.Properties properties() {
		HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
		Tool tool = new Tool(
			List.of(
				Tool.Rule.deniesDrops(blocks.getOrThrow(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)),
				Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
				Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), MINING_SPEED),
				Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_AXE), MINING_SPEED),
				Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), MINING_SPEED),
				Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_HOE), MINING_SPEED)
			),
			1.0F,
			1,
			true
		);
		return new Item.Properties()
			.durability(DURABILITY)
			.enchantable(15)
			.component(DataComponents.TOOL, tool)
			.component(ModComponents.TOOL_MODE, BeeToolMode.SWORD)
			.attributes(attributes(BeeToolMode.SWORD))
			.component(DataComponents.WEAPON, weapon(BeeToolMode.SWORD));
	}

	private static ItemAttributeModifiers attributes(final BeeToolMode mode) {
		return ItemAttributeModifiers.builder()
			.add(
				Attributes.ATTACK_DAMAGE,
				new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, mode.attackDamage, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND
			)
			.add(
				Attributes.ATTACK_SPEED,
				new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, mode.attackSpeed, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND
			)
			.build();
	}

	private static Weapon weapon(final BeeToolMode mode) {
		return mode == BeeToolMode.SWORD ? new Weapon(1) : new Weapon(2, mode.disableBlockingSeconds);
	}

	public static BeeToolMode getMode(final ItemStack stack) {
		return stack.getOrDefault(ModComponents.TOOL_MODE, BeeToolMode.SWORD);
	}

	public static boolean isSwordMode(final ItemStack stack) {
		return stack.getItem() instanceof BeeMultitoolItem && getMode(stack) == BeeToolMode.SWORD;
	}

	/** Moves the multitool in the player's main hand to its next mode. Called when they press the swap-hands key. */
	public static void cycleMode(final ServerPlayer player, final ItemStack stack) {
		BeeToolMode mode = getMode(stack).next();
		stack.set(ModComponents.TOOL_MODE, mode);
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes(mode));
		stack.set(DataComponents.WEAPON, weapon(mode));
		if (mode.transformer == null) {
			stack.remove(DataComponents.BLOCK_TRANSFORMER);
		} else {
			stack.set(DataComponents.BLOCK_TRANSFORMER, player.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getOrThrow(mode.transformer));
		}
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEEHIVE_WORK, SoundSource.PLAYERS, 0.8F, 1.4F + mode.ordinal() * 0.1F);
		player.sendOverlayMessage(Component.translatable("message.hivemind.multitool_mode", Component.translatable(mode.translationKey())).withStyle(ChatFormatting.GOLD));
	}

	@Override
	public void appendHoverText(
		final ItemStack itemStack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> builder, final TooltipFlag tooltipFlag
	) {
		builder.accept(
			Component.translatable("item.hivemind.bee_multitool.mode", Component.translatable(getMode(itemStack).translationKey())).withStyle(ChatFormatting.GOLD)
		);
		builder.accept(Component.translatable("item.hivemind.bee_multitool.hint", Component.keybind("key.swapOffhand")).withStyle(ChatFormatting.GRAY));
	}
}
