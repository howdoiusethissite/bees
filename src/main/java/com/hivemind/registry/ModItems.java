package com.hivemind.registry;

import com.hivemind.Hivemind;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public final class ModItems {
	public static final Item SHRINKING_HONEY = register(
		"shrinking_honey",
		Item::new,
		new Item.Properties()
			.stacksTo(16)
			.rarity(Rarity.UNCOMMON)
			.craftRemainder(Items.GLASS_BOTTLE)
			.usingConvertsTo(Items.GLASS_BOTTLE)
			.component(
				net.minecraft.core.component.DataComponents.CONSUMABLE,
				Consumables.defaultDrink()
					.consumeSeconds(1.6F)
					.sound(SoundEvents.HONEY_DRINK)
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(ModEffects.SHRUNK, 20 * 60 * 5, 0)))
					.build()
			)
	);
	public static final Item HIVE_EXIT = register(
		"hive_exit", p -> new BlockItem(ModBlocks.HIVE_EXIT, p), new Item.Properties().useBlockDescriptionPrefix()
	);
	public static final Item GUARD_BEE_SPAWN_EGG = register(
		"guard_bee_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.GUARD_BEE)
	);
	public static final Item QUEEN_BEE_SPAWN_EGG = register(
		"queen_bee_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.QUEEN_BEE)
	);

	public static final CreativeModeTab TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		Hivemind.id("hivemind"),
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.hivemind"))
			.icon(() -> new ItemStack(SHRINKING_HONEY))
			.displayItems((params, output) -> {
				output.accept(SHRINKING_HONEY);
				output.accept(GUARD_BEE_SPAWN_EGG);
				output.accept(QUEEN_BEE_SPAWN_EGG);
			})
			.build()
	);

	private ModItems() {
	}

	private static Item register(final String name, final Function<Item.Properties, Item> factory, final Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Hivemind.id(name));
		Item item = factory.apply(properties.setId(key));
		if (item instanceof BlockItem blockItem) {
			blockItem.registerBlocks(Item.BY_BLOCK, item);
		}
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
	}
}
