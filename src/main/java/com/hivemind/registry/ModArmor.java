package com.hivemind.registry;

import com.hivemind.Hivemind;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmor {
	public static final TagKey<Item> REPAIRS_BEE_ARMOR = TagKey.create(Registries.ITEM, Hivemind.id("repairs_bee_armor"));
	public static final ResourceKey<EquipmentAsset> BEE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Hivemind.id("bee"));

	/** About as tough as iron, but much easier to enchant. Patched up with honeycomb. */
	public static final ArmorMaterial BEE = new ArmorMaterial(
		18,
		Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2, ArmorType.BODY, 5),
		20,
		SoundEvents.ARMOR_EQUIP_LEATHER,
		0.0F,
		0.0F,
		REPAIRS_BEE_ARMOR,
		BEE_ASSET
	);

	private ModArmor() {
	}
}
