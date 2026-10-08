package com.hivemind.world;

import com.hivemind.entity.QueenBee;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

/**
 * While players are inside a hive, critters periodically try to raid it. They go for the queen;
 * the guards and the players fight them off. Win and the queen rewards everyone who helped.
 */
public final class HiveRaids {
	public static final String INVADER_TAG = "hivemind.invader";
	private static final int FIRST_RAID_MIN = 20 * 120;
	private static final int FIRST_RAID_MAX = 20 * 180;
	private static final int NEXT_RAID_MIN = 20 * 240;
	private static final int NEXT_RAID_MAX = 20 * 420;
	/** If the queen drops below this fraction of her health, the raiders make off with the honey. */
	private static final float QUEEN_LOSS_FRACTION = 0.25F;

	private static final Map<Integer, Long> NEXT_RAID = new HashMap<>();
	private static final Map<Integer, ActiveRaid> ACTIVE = new HashMap<>();

	private static final class ActiveRaid {
		final int total;
		final ServerBossEvent bar;

		ActiveRaid(final int total) {
			this.total = total;
			this.bar = new ServerBossEvent(
				UUID.randomUUID(), Component.translatable("bossbar.hivemind.raid"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10
			);
		}
	}

	private HiveRaids() {
	}

	public static void tick(final ServerLevel level) {
		if (level.getGameTime() % 20 != 0) {
			return;
		}

		Map<Integer, List<ServerPlayer>> playersByHive = new HashMap<>();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			int index = HiveLayout.indexAt(player.position());
			if (index >= 0) {
				playersByHive.computeIfAbsent(index, i -> new ArrayList<>()).add(player);
			}
		}

		// Clear boss bars for anyone who walked out mid-raid.
		for (Map.Entry<Integer, ActiveRaid> entry : ACTIVE.entrySet()) {
			List<ServerPlayer> present = playersByHive.getOrDefault(entry.getKey(), List.of());
			for (ServerPlayer player : List.copyOf(entry.getValue().bar.getPlayers())) {
				if (!present.contains(player)) {
					entry.getValue().bar.removePlayer(player);
				}
			}
		}

		long now = level.getGameTime();
		for (Map.Entry<Integer, List<ServerPlayer>> entry : playersByHive.entrySet()) {
			int index = entry.getKey();
			List<ServerPlayer> players = entry.getValue();
			ActiveRaid raid = ACTIVE.get(index);
			if (raid == null) {
				List<Mob> leftovers = invaders(level, index);
				if (!leftovers.isEmpty()) {
					// Raid survived a server restart; pick it back up.
					raid = new ActiveRaid(leftovers.size());
					ACTIVE.put(index, raid);
				} else {
					long next = NEXT_RAID.computeIfAbsent(index, i -> now + Mth.nextInt(level.getRandom(), FIRST_RAID_MIN, FIRST_RAID_MAX));
					if (now >= next) {
						startRaid(level, index, players);
					}
					continue;
				}
			}
			tickRaid(level, index, raid, players);
		}
	}

	/** Starts a raid right away in the given hive (used by /hivemind raid). Returns false if one is already running. */
	public static boolean forceRaid(final ServerLevel level, final int index, final List<ServerPlayer> players) {
		if (ACTIVE.containsKey(index)) {
			return false;
		}
		startRaid(level, index, players);
		return ACTIVE.containsKey(index);
	}

	public static boolean isRaidActive(final int index) {
		return ACTIVE.containsKey(index);
	}

	private static List<Mob> invaders(final ServerLevel level, final int index) {
		return level.getEntitiesOfClass(Mob.class, HiveLayout.bounds(index), m -> m.isAlive() && m.entityTags().contains(INVADER_TAG));
	}

	private static QueenBee findQueen(final ServerLevel level, final int index) {
		List<QueenBee> queens = level.getEntitiesOfClass(QueenBee.class, HiveLayout.bounds(index), QueenBee::isAlive);
		return queens.isEmpty() ? null : queens.getFirst();
	}

	private static void startRaid(final ServerLevel level, final int index, final List<ServerPlayer> players) {
		QueenBee queen = findQueen(level, index);
		if (queen == null) {
			NEXT_RAID.put(index, level.getGameTime() + NEXT_RAID_MAX);
			return;
		}

		RandomSource random = level.getRandom();
		BlockPos c = HiveLayout.center(index);
		int count = 3 + players.size() * 2 + random.nextInt(3);
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			EntityType<? extends Mob> type = switch (random.nextInt(10)) {
				case 0, 1, 2 -> EntityTypes.SPIDER;
				case 3, 4, 5 -> EntityTypes.CAVE_SPIDER;
				default -> EntityTypes.SILVERFISH;
			};
			Mob mob = type.create(level, EntitySpawnReason.EVENT);
			if (mob == null) {
				continue;
			}
			for (int attempt = 0; attempt < 12; attempt++) {
				double angle = random.nextDouble() * Math.PI * 2;
				double radius = 14 + random.nextDouble() * 6;
				mob.snapTo(c.getX() + 0.5 + Math.cos(angle) * radius, HiveLayout.floorY() + 1, c.getZ() + 0.5 + Math.sin(angle) * radius, random.nextFloat() * 360, 0);
				if (level.noCollision(mob)) {
					mob.addTag(INVADER_TAG);
					mob.setPersistenceRequired();
					mob.setTarget(queen);
					level.addFreshEntity(mob);
					level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.3, mob.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
					spawned++;
					break;
				}
			}
		}

		if (spawned == 0) {
			NEXT_RAID.put(index, level.getGameTime() + NEXT_RAID_MIN);
			return;
		}

		ActiveRaid raid = new ActiveRaid(spawned);
		ACTIVE.put(index, raid);
		for (ServerPlayer player : players) {
			raid.bar.addPlayer(player);
			player.sendSystemMessage(Component.translatable("message.hivemind.raid_start").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		}
		level.playSound(null, queen.getX(), queen.getY(), queen.getZ(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 2.0F, 0.8F);
		queen.say(null, "raid_start");
	}

	private static void tickRaid(final ServerLevel level, final int index, final ActiveRaid raid, final List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			raid.bar.addPlayer(player);
		}

		List<Mob> invaders = invaders(level, index);
		QueenBee queen = findQueen(level, index);
		raid.bar.setProgress(Mth.clamp(invaders.size() / (float)raid.total, 0.0F, 1.0F));

		if (invaders.isEmpty()) {
			endRaid(level, index, raid);
			for (ServerPlayer player : players) {
				player.sendSystemMessage(Component.translatable("message.hivemind.raid_won").withStyle(ChatFormatting.GREEN));
				if (queen != null) {
					queen.rewardDefender(player);
				}
			}
			if (queen != null) {
				queen.say(null, "raid_won");
			}
			return;
		}

		if (queen == null || queen.getHealth() < queen.getMaxHealth() * QUEEN_LOSS_FRACTION) {
			for (Mob invader : invaders) {
				level.sendParticles(ParticleTypes.POOF, invader.getX(), invader.getY() + 0.3, invader.getZ(), 6, 0.3, 0.2, 0.3, 0.02);
				invader.discard();
			}
			endRaid(level, index, raid);
			for (ServerPlayer player : players) {
				player.sendSystemMessage(Component.translatable("message.hivemind.raid_lost").withStyle(ChatFormatting.RED));
			}
			if (queen != null) {
				queen.say(null, "raid_lost");
			}
			return;
		}

		// Raiders that lost interest in a target go back to harassing the queen.
		for (Mob invader : invaders) {
			if (invader.getTarget() == null || !invader.getTarget().isAlive()) {
				invader.setTarget(queen);
			}
		}
	}

	private static void endRaid(final ServerLevel level, final int index, final ActiveRaid raid) {
		raid.bar.removeAllPlayers();
		ACTIVE.remove(index);
		NEXT_RAID.put(index, level.getGameTime() + Mth.nextInt(level.getRandom(), NEXT_RAID_MIN, NEXT_RAID_MAX));
	}

	/** Used by the guards to decide what to attack. */
	public static boolean isInvader(final net.minecraft.world.entity.LivingEntity entity) {
		return entity.entityTags().contains(INVADER_TAG);
	}

}
