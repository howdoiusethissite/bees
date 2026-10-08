package com.hivemind.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Short-lived swarms of ordinary vanilla bees, let loose by beenades and bee armor. They hunt the
 * nearest monster, sting it (and, being bees, die from it), and fly off after a while if they never do.
 */
public final class BeeSwarms {
	public static final String SWARM_TAG = "hivemind.swarm";
	private static final int LIFETIME = 20 * 25;
	private static final double HUNT_RANGE = 16.0;

	/** Swarm bee id -> game time it should leave. Only kept in memory; swarm bees loaded back in after a restart leave right away. */
	private static final Map<UUID, Long> EXPIRY = new HashMap<>();

	private BeeSwarms() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 10 == 0 && !EXPIRY.isEmpty()) {
				tick(level);
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Bee && entity.entityTags().contains(SWARM_TAG) && !EXPIRY.containsKey(entity.getUUID())) {
				// A swarm bee saved before a restart. Let it go on the next swarm tick.
				EXPIRY.put(entity.getUUID(), level.getGameTime());
			}
		});
	}

	public static int release(final ServerLevel level, final Vec3 pos, final @Nullable Entity owner) {
		return release(level, pos, owner, 3 + level.getRandom().nextInt(3), null);
	}

	/** Spawns {@code count} bees at {@code pos}. They go for {@code target} if given, otherwise the nearest monster. */
	public static int release(final ServerLevel level, final Vec3 pos, final @Nullable Entity owner, final int count, final @Nullable LivingEntity target) {
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			Bee bee = EntityTypes.BEE.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (bee == null) {
				continue;
			}
			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			bee.snapTo(pos.x + Math.cos(angle) * 0.3, pos.y, pos.z + Math.sin(angle) * 0.3, (float)Math.toDegrees(angle), 0.0F);
			bee.setDeltaMovement(Math.cos(angle) * 0.2, 0.2, Math.sin(angle) * 0.2);
			if (HiveLayout.isHiveLevel(level)) {
				// Inside a hive everything is bee-sized, so the swarm matches the residents.
				AttributeInstance scale = bee.getAttribute(Attributes.SCALE);
				if (scale != null) {
					scale.setBaseValue(HiveInteriorBuilder.RESIDENT_BEE_SCALE);
				}
			}
			bee.addTag(SWARM_TAG);
			EXPIRY.put(bee.getUUID(), level.getGameTime() + LIFETIME + level.getRandom().nextInt(40));
			level.addFreshEntity(bee);
			LivingEntity prey = target != null && target.isAlive() ? target : findPrey(level, bee, owner);
			if (prey != null) {
				sic(bee, prey);
			}
			spawned++;
		}
		return spawned;
	}

	private static void tick(final ServerLevel level) {
		long now = level.getGameTime();
		List<UUID> done = new ArrayList<>();
		for (Map.Entry<UUID, Long> entry : EXPIRY.entrySet()) {
			Entity entity = level.getEntity(entry.getKey());
			if (!(entity instanceof Bee bee)) {
				// Either it's in another dimension or it's gone. Forget it once its time is up either way.
				if (now > entry.getValue() + 200) {
					done.add(entry.getKey());
				}
				continue;
			}
			if (!bee.isAlive() || now >= entry.getValue()) {
				if (bee.isAlive()) {
					level.sendParticles(ParticleTypes.POOF, bee.getX(), bee.getY() + 0.2, bee.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
					bee.discard();
				}
				done.add(entry.getKey());
				continue;
			}
			if (!bee.hasStung() && (bee.getTarget() == null || !bee.getTarget().isAlive())) {
				LivingEntity prey = findPrey(level, bee, null);
				if (prey != null) {
					sic(bee, prey);
				}
			}
		}
		done.forEach(EXPIRY::remove);
	}

	private static @Nullable LivingEntity findPrey(final ServerLevel level, final Bee bee, final @Nullable Entity owner) {
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity candidate : level.getEntitiesOfClass(
			LivingEntity.class, bee.getBoundingBox().inflate(HUNT_RANGE), e -> e instanceof Enemy && e.isAlive() && e != owner && !e.isInvisible()
		)) {
			double dist = candidate.distanceToSqr(bee);
			if (dist < bestDist) {
				best = candidate;
				bestDist = dist;
			}
		}
		return best;
	}

	private static void sic(final Bee bee, final LivingEntity prey) {
		bee.setTarget(prey);
		bee.setPersistentAngerTarget(EntityReference.of(prey));
		bee.startPersistentAngerTimer();
	}
}
