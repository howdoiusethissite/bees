package com.hivemind.world;

import com.hivemind.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * The full bee armor set bonus. Wild bees won't sting you, and anything that hurts you gets
 * a couple of bees sent after it.
 */
public final class BeeArmor {
	private static final int SWARM_COOLDOWN = 20 * 4;
	private static final Map<UUID, Long> LAST_SWARM = new HashMap<>();

	private BeeArmor() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.getEntity() instanceof Bee bee && entity instanceof Player player && hasFullSet(player)) {
				// They know one of their own when they see one.
				bee.stopBeingAngry();
				bee.setTarget(null);
				return false;
			}
			return true;
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (!(entity instanceof Player player) || !(entity.level() instanceof ServerLevel level) || damageTaken <= 0.0F) {
				return;
			}
			if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player || attacker instanceof Bee || !hasFullSet(player)) {
				return;
			}
			long now = level.getGameTime();
			Long last = LAST_SWARM.get(player.getUUID());
			if (last != null && now - last < SWARM_COOLDOWN) {
				return;
			}
			LAST_SWARM.put(player.getUUID(), now);
			BeeSwarms.release(level, player.position().add(0.0, player.getBbHeight() * 0.7, 0.0), player, 2, attacker);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEEHIVE_EXIT, SoundSource.PLAYERS, 1.0F, 1.2F);
		});
	}

	public static boolean hasFullSet(final LivingEntity entity) {
		return is(entity, EquipmentSlot.HEAD, ModItems.BEE_HEADGEAR)
			&& is(entity, EquipmentSlot.CHEST, ModItems.BEE_BREASTPLATE)
			&& is(entity, EquipmentSlot.LEGS, ModItems.BEE_GREAVES)
			&& is(entity, EquipmentSlot.FEET, ModItems.BEE_BOOTS);
	}

	private static boolean is(final LivingEntity entity, final EquipmentSlot slot, final Item item) {
		return entity.getItemBySlot(slot).is(item);
	}
}
