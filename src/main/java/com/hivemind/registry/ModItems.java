package com.hivemind.registry;

import com.hivemind.Hivemind;
import com.hivemind.item.BeeMultitoolItem;
import com.hivemind.item.BeenadeItem;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
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

	public static final Item BEENADE = register("beenade", BeenadeItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	public static final Item BROOD_CELL = register("brood_cell", p -> new BlockItem(ModBlocks.BROOD_CELL, p), new Item.Properties().useBlockDescriptionPrefix());

	// Honey food.
	public static final Item ROYAL_JELLY = register(
		"royal_jelly",
		Item::new,
		new Item.Properties()
			.rarity(Rarity.UNCOMMON)
			.food(
				new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).alwaysEdible().build(),
				Consumables.defaultFood()
					.consumeSeconds(1.2F)
					.sound(SoundEvents.HONEY_DRINK)
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 0)))
					.build()
			)
	);
	public static final Item HONEY_TOAST = register(
		"honey_toast", Item::new, new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9F).build())
	);
	public static final Item HONEYED_APPLE = register(
		"honeyed_apple",
		Item::new,
		new Item.Properties()
			.food(
				new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build(),
				// Honey settles a stomach full of spider venom, which comes in handy during a raid.
				Consumables.defaultFood().onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.POISON)).build()
			)
	);
	public static final Item HONEYCOMB_CANDY = register(
		"honeycomb_candy",
		Item::new,
		new Item.Properties()
			.food(
				new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).alwaysEdible().build(),
				Consumables.defaultFood()
					.consumeSeconds(0.8F)
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 15, 0)))
					.build()
			)
	);
	public static final Item HONEY_GLAZED_HAM = register(
		"honey_glazed_ham", Item::new, new Item.Properties().food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0F).build())
	);

	// Bee gear.
	public static final Item BEE_HEADGEAR = register("bee_headgear", Item::new, new Item.Properties().humanoidArmor(ModArmor.BEE, ArmorType.HELMET));
	public static final Item BEE_BREASTPLATE = register("bee_breastplate", Item::new, new Item.Properties().humanoidArmor(ModArmor.BEE, ArmorType.CHESTPLATE));
	public static final Item BEE_GREAVES = register("bee_greaves", Item::new, new Item.Properties().humanoidArmor(ModArmor.BEE, ArmorType.LEGGINGS));
	public static final Item BEE_BOOTS = register("bee_boots", Item::new, new Item.Properties().humanoidArmor(ModArmor.BEE, ArmorType.BOOTS));
	public static final Item BEE_MULTITOOL = register(
		"bee_multitool", BeeMultitoolItem::new, BeeMultitoolItem.properties().rarity(Rarity.RARE).repairable(ROYAL_JELLY)
	);

	public static final CreativeModeTab TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		Hivemind.id("hivemind"),
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.hivemind"))
			.icon(() -> new ItemStack(SHRINKING_HONEY))
			.displayItems((params, output) -> {
				output.accept(SHRINKING_HONEY);
				output.accept(BEENADE);
				output.accept(ROYAL_JELLY);
				output.accept(HONEY_TOAST);
				output.accept(HONEYED_APPLE);
				output.accept(HONEYCOMB_CANDY);
				output.accept(HONEY_GLAZED_HAM);
				output.accept(BEE_HEADGEAR);
				output.accept(BEE_BREASTPLATE);
				output.accept(BEE_GREAVES);
				output.accept(BEE_BOOTS);
				output.accept(BEE_MULTITOOL);
				output.accept(BROOD_CELL);
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
