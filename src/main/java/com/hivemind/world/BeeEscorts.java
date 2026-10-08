package com.hivemind.world;

import com.hivemind.Hivemind;
import com.hivemind.entity.QueenBee;
import com.hivemind.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Bees that follow a player around. Sneak and right-click a bee with an empty hand to have it tag
 * along; do it again to send it off. Following bees stay out of their hives (so they don't dive home
 * the moment they pick up nectar), and they come with you into and out of hives. That's how you take
 * bees from a hive's nursery home with you, as long as the queen likes you enough to let them go.
 */
public final class BeeEscorts {
	public static final int MAX_PER_PLAYER = 6;
	/** How much the queen has to like you before she lets her workers leave with you. */
	public static final int FAVOR_TO_TAKE_RESIDENTS = 6;
	private static final double CARRY_RANGE = 16.0;
	private static final double CATCH_UP_DISTANCE = 5.0;
	private static final double TELEPORT_DISTANCE = 24.0;
	/** Visiting bees inside a hive are scaled up to match everything else in there. */
	private static final Identifier HIVE_SIZE = Hivemind.id("hive_size");

	/** Loaded following bees -> who they follow. */
	private static final Map<UUID, UUID> TRACKED = new HashMap<>();
	/** One click can reach us as two interaction packets; only the first one toggles. */
	private static final Map<UUID, Long> LAST_TOGGLE = new HashMap<>();

	private BeeEscorts() {
	}

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty() || !(entity instanceof Bee bee)) {
				return InteractionResult.PASS;
			}
			if (bee.entityTags().contains(BeeSwarms.SWARM_TAG)) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel serverLevel) {
				toggle(serverLevel, player, bee);
			}
			return InteractionResult.SUCCESS;
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Bee bee) {
				UUID owner = bee.getAttached(ModAttachments.ESCORTING);
				if (owner != null) {
					TRACKED.put(bee.getUUID(), owner);
				}
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> TRACKED.remove(entity.getUUID()));

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 10 == 0 && !TRACKED.isEmpty()) {
				tick(level);
			}
		});
	}

	public static boolean isFollowing(final Bee bee, final Player player) {
		return player.getUUID().equals(bee.getAttached(ModAttachments.ESCORTING));
	}

	private static void toggle(final ServerLevel level, final Player player, final Bee bee) {
		Long last = LAST_TOGGLE.put(player.getUUID(), level.getGameTime());
		if (last != null && level.getGameTime() - last < 4) {
			return;
		}
		UUID owner = bee.getAttached(ModAttachments.ESCORTING);
		if (owner != null) {
			if (owner.equals(player.getUUID())) {
				stopFollowing(bee);
				level.sendParticles(ParticleTypes.POOF, bee.getX(), bee.getY() + 0.3, bee.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
				player.sendOverlayMessage(Component.translatable("message.hivemind.escort.dismissed").withStyle(ChatFormatting.YELLOW));
			} else {
				player.sendOverlayMessage(Component.translatable("message.hivemind.escort.taken").withStyle(ChatFormatting.YELLOW));
			}
			return;
		}
		if (bee.isAngry()) {
			player.sendOverlayMessage(Component.translatable("message.hivemind.escort.angry").withStyle(ChatFormatting.RED));
			return;
		}
		long following = TRACKED.values().stream().filter(player.getUUID()::equals).count();
		if (following >= MAX_PER_PLAYER) {
			player.sendOverlayMessage(Component.translatable("message.hivemind.escort.too_many", MAX_PER_PLAYER).withStyle(ChatFormatting.YELLOW));
			return;
		}
		if (bee.entityTags().contains(HiveInteriorBuilder.RESIDENT_TAG) && HiveLayout.isHiveLevel(level)) {
			// The hive's own workers need the queen's say-so.
			QueenBee queen = queenFor(level, bee);
			if (queen != null && queen.getFavor(player) < FAVOR_TO_TAKE_RESIDENTS) {
				player.sendOverlayMessage(Component.translatable("message.hivemind.escort.queen_says_no", FAVOR_TO_TAKE_RESIDENTS).withStyle(ChatFormatting.YELLOW));
				queen.say(player, "escort_no");
				return;
			}
		}
		bee.setAttached(ModAttachments.ESCORTING, player.getUUID());
		TRACKED.put(bee.getUUID(), player.getUUID());
		bee.setPersistenceRequired();
		level.sendParticles(ParticleTypes.HEART, bee.getX(), bee.getY() + 0.5, bee.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
		level.playSound(null, bee.getX(), bee.getY(), bee.getZ(), SoundEvents.BEE_POLLINATE, SoundSource.NEUTRAL, 1.0F, 1.3F);
		player.sendOverlayMessage(Component.translatable("message.hivemind.escort.following").withStyle(ChatFormatting.GOLD));
	}

	private static void stopFollowing(final Bee bee) {
		bee.removeAttached(ModAttachments.ESCORTING);
		TRACKED.remove(bee.getUUID());
		bee.setStayOutOfHiveCountdown(0);
	}

	private static QueenBee queenFor(final ServerLevel level, final Bee bee) {
		int index = HiveLayout.indexAt(bee.position());
		if (index < 0) {
			return null;
		}
		List<QueenBee> queens = level.getEntitiesOfClass(QueenBee.class, HiveLayout.bounds(index), QueenBee::isAlive);
		return queens.isEmpty() ? null : queens.getFirst();
	}

	private static void tick(final ServerLevel level) {
		List<UUID> gone = new ArrayList<>();
		for (Map.Entry<UUID, UUID> entry : TRACKED.entrySet()) {
			if (!(level.getEntity(entry.getKey()) instanceof Bee bee)) {
				continue;
			}
			if (!bee.isAlive()) {
				gone.add(entry.getKey());
				continue;
			}
			// Keeps it from heading home, nectar or not.
			bee.setStayOutOfHiveCountdown(20 * 30);
			Player owner = level.getPlayerByUUID(entry.getValue());
			if (owner == null || owner.isSpectator() || bee.isInLove() || bee.getTarget() != null || bee.isLeashed()) {
				// Busy (breeding, fighting) or the owner isn't around. Let it be a bee for a bit.
				continue;
			}
			double dist = bee.distanceTo(owner);
			if (dist > TELEPORT_DISTANCE) {
				bee.snapTo(owner.getX(), owner.getY() + owner.getBbHeight() * 0.8, owner.getZ(), bee.getYRot(), 0.0F);
				bee.getNavigation().stop();
			} else if (dist > CATCH_UP_DISTANCE) {
				bee.getNavigation().moveTo(owner.getX(), owner.getY() + owner.getBbHeight() * 0.6, owner.getZ(), 1.2);
			}
		}
		gone.forEach(TRACKED::remove);
	}

	/** The bees following {@code player} that are close enough to come along through a hive entrance or exit. */
	public static List<Bee> nearbyFollowers(final ServerLevel level, final Player player) {
		return level.getEntitiesOfClass(Bee.class, player.getBoundingBox().inflate(CARRY_RANGE), b -> b.isAlive() && isFollowing(b, player));
	}

	/**
	 * Brings following bees along to where the player just went. Bees going into a hive are scaled up to
	 * match the residents; bees leaving go back to normal size, and residents leaving stop being residents.
	 */
	public static void bringAlong(final List<Bee> followers, final ServerPlayer player) {
		ServerLevel destination = player.level();
		boolean intoHive = HiveLayout.isHiveLevel(destination);
		int i = 0;
		for (Bee bee : followers) {
			double angle = i++ * Math.PI * 2 / Math.max(1, followers.size());
			Vec3 pos = player.position().add(Math.cos(angle) * 1.2, player.getBbHeight() * 0.8, Math.sin(angle) * 1.2);
			UUID owner = bee.getAttached(ModAttachments.ESCORTING);
			Entity moved = bee.teleport(new TeleportTransition(destination, pos, Vec3.ZERO, bee.getYRot(), 0.0F, TeleportTransition.DO_NOTHING));
			if (!(moved instanceof Bee arrived)) {
				continue;
			}
			if (owner != null) {
				arrived.setAttached(ModAttachments.ESCORTING, owner);
				TRACKED.put(arrived.getUUID(), owner);
			}
			AttributeInstance scale = arrived.getAttribute(Attributes.SCALE);
			if (intoHive) {
				if (scale != null && !arrived.entityTags().contains(HiveInteriorBuilder.RESIDENT_TAG)) {
					scale.addOrReplacePermanentModifier(
						new AttributeModifier(HIVE_SIZE, HiveInteriorBuilder.RESIDENT_BEE_SCALE - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
					);
				}
			} else {
				if (arrived.entityTags().contains(HiveInteriorBuilder.RESIDENT_TAG)) {
					// A hive bee seeing the outside world for the first time.
					arrived.removeTag(HiveInteriorBuilder.RESIDENT_TAG);
					arrived.clearHome();
					if (scale != null) {
						scale.setBaseValue(Attributes.SCALE.value().getDefaultValue());
					}
				}
				if (scale != null) {
					scale.removeModifier(HIVE_SIZE);
				}
			}
			arrived.setStayOutOfHiveCountdown(20 * 30);
			destination.playSound(null, arrived.getX(), arrived.getY(), arrived.getZ(), SoundEvents.BEEHIVE_EXIT, SoundSource.NEUTRAL, 0.6F, 1.2F);
		}
	}
}
