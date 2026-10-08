package com.hivemind.entity;

import com.hivemind.Hivemind;
import com.hivemind.block.EmptyBroodCellBlock;
import com.hivemind.network.QueenSpeechPayload;
import com.hivemind.registry.ModBlocks;
import com.hivemind.world.HiveEvents;
import com.hivemind.world.HiveLayout;
import com.hivemind.world.HiveRaids;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The queen at the heart of every hive. She holds court on her dais, accepts flowers, remembers who has
 * been kind (or rude) to her hive, and hands out gifts to her favorites and to those who defend her.
 * Every so often she gets up to lay eggs in the nursery, and during a raid she rallies her guards.
 */
public class QueenBee extends PathfinderMob {
	public static final ResourceKey<LootTable> GIFT_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Hivemind.id("gameplay/queen_gift"));
	public static final ResourceKey<LootTable> DEFENSE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Hivemind.id("gameplay/queen_defense_reward"));
	private static final Codec<Map<UUID, Integer>> FAVOR_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT);
	/** Minimum time between flower gifts for one player, so a stack of dandelions isn't a stack of emeralds. */
	private static final int GIFT_COOLDOWN = 20 * 30;
	private static final int FLOWERS_PER_GIFT = 3;
	private static final int FAVOR_PER_EXTRA_ROLL = 15;
	private static final int MAX_GIFT_ROLLS = 3;

	/** How many lines the queen has for each occasion. Lines live in the lang file as {@code queen.hivemind.<topic>.<n>}. */
	private static final Map<String, Integer> LINES = Map.ofEntries(
		Map.entry("greet", 5), Map.entry("flower", 5), Map.entry("gift", 4), Map.entry("angry", 3), Map.entry("warn", 3), Map.entry("idle", 6),
		Map.entry("cocoon", 4), Map.entry("escort_no", 3), Map.entry("nursery", 4), Map.entry("raid_start", 3), Map.entry("raid_won", 3), Map.entry("raid_lost", 3)
	);
	/** Lines that always get said (never skipped for being too soon after the last one) and are kept in the chat log. */
	private static final Set<String> IMPORTANT = Set.of("gift", "raid_start", "raid_won", "raid_lost");
	/** Small talk is skipped if she said something less than this long ago, so a handful of flowers isn't a handful of lines. */
	private static final int SMALL_TALK_GAP = 20 * 6;
	private static final double SPEECH_RANGE = 24.0;
	private static final double IDLE_CHAT_RANGE = 8.0;
	private static final int IDLE_CHAT_MIN = 20 * 150;
	private static final int IDLE_CHAT_MAX = 20 * 300;
	/** After any conversation she waits at least this long before starting one on her own. */
	private static final int IDLE_CHAT_AFTER_TALK = 20 * 90;

	private static final double WALK_SPEED = 0.18;
	private static final int NURSERY_VISIT_MIN = 20 * 60;
	private static final int NURSERY_VISIT_MAX = 20 * 120;
	/** During a raid she rallies everyone nearby this often. */
	private static final int RALLY_INTERVAL = 20 * 8;
	private static final double RALLY_RANGE = 20.0;

	private final Map<UUID, Integer> favor = new HashMap<>();
	private final Map<UUID, Long> lastGift = new HashMap<>();
	private int idleChatCooldown = IDLE_CHAT_MIN;
	private long lastSpoke = Long.MIN_VALUE / 2;
	private int nurseryCooldown = 20 * 20;
	/** True while she's out visiting the nursery, so she doesn't turn straight back for the throne. */
	private boolean tending;

	public QueenBee(final EntityType<? extends QueenBee> type, final Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 120.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new ReturnToThroneGoal());
		this.goalSelector.addGoal(1, new TendNurseryGoal());
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0F, 0.6F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	private boolean raidActive() {
		return HiveRaids.isRaidActive(HiveLayout.indexAt(this.position()));
	}

	/** Her seat on the dais. Queens placed by hand (spawn egg) treat wherever they were put as their throne. */
	private BlockPos throne() {
		return this.hasHome() ? this.getHomePosition() : this.blockPosition();
	}

	public int getFavor(final Player player) {
		return this.favor.getOrDefault(player.getUUID(), 0);
	}

	public void adjustFavor(final Player player, final int amount) {
		this.favor.merge(player.getUUID(), amount, Integer::sum);
	}

	@Override
	protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(BlockItemTags.FLOWERS.item())) {
			if (this.level() instanceof ServerLevel level) {
				this.acceptFlower(level, player, held);
			}
			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
			if (!this.level().isClientSide()) {
				this.say(player, "greet", this.getFavor(player));
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	private void acceptFlower(final ServerLevel level, final Player player, final ItemStack flower) {
		flower.consume(1, player);
		this.adjustFavor(player, 1);
		int favor = this.getFavor(player);

		level.sendParticles(ParticleTypes.HEART, this.getX(), this.getEyeY() + 0.4, this.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEE_POLLINATE, SoundSource.NEUTRAL, 1.0F, 0.8F);

		long now = level.getGameTime();
		long last = this.lastGift.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
		boolean dueGift = favor > 0 && favor % FLOWERS_PER_GIFT == 0;
		if (dueGift && now - last >= GIFT_COOLDOWN) {
			this.lastGift.put(player.getUUID(), now);
			int rolls = Math.min(MAX_GIFT_ROLLS, 1 + favor / FAVOR_PER_EXTRA_ROLL);
			for (int i = 0; i < rolls; i++) {
				this.giveGift(level, player, GIFT_LOOT);
			}
			this.say(player, "gift", favor);
		} else {
			this.say(player, "flower", favor);
		}
	}

	/** Called when a raid on this queen's hive is beaten while the player was there to help. */
	public void rewardDefender(final Player player) {
		if (this.level() instanceof ServerLevel level) {
			this.adjustFavor(player, 5);
			this.giveGift(level, player, DEFENSE_LOOT);
		}
	}

	private void giveGift(final ServerLevel level, final Player player, final ResourceKey<LootTable> table) {
		this.swingForAttack(InteractionHand.MAIN_HAND);
		boolean close = this.distanceToSqr(player) < 16.0;
		this.dropFromGiftLootTable(level, table, (l, stack) -> {
			if (close) {
				BehaviorUtils.throwItem(this, stack, player.position());
			} else {
				// Too far to toss it; the gift turns up at the player's feet instead.
				ItemEntity item = new ItemEntity(l, player.getX(), player.getY() + 0.5, player.getZ(), stack);
				item.setNoPickUpDelay();
				l.addFreshEntity(item);
			}
		});
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.2F);
		level.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + 1.2, this.getZ(), 12, 0.5, 0.6, 0.5, 0.1);
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		if (source.getEntity() instanceof Player player) {
			if (!player.isCreative()) {
				// Nobody lays a hand on the queen. A stray swing gets a warning; a second sets the hive on you.
				if (HiveEvents.offend(level, player, "message.hivemind.queen_hit")) {
					this.say(player, "angry", -1);
				}
				return false;
			}
			return super.hurtServer(level, source, damage);
		}
		if (source.isCreativePlayer() || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		// Raiders can wear her down but never kill her; losing too much health just loses the raid.
		float capped = Math.min(damage, Math.max(0.0F, this.getHealth() - 1.0F));
		return capped > 0.0F && super.hurtServer(level, source, capped);
	}

	/**
	 * Has the queen say a random line about {@code topic}. Everyone nearby hears her babble; the line
	 * itself is shown to {@code listener}, or to everyone nearby if there is no particular listener.
	 * Players without the mod installed just get the line as text.
	 */
	public void say(final @Nullable Player listener, final String topic, final int favor) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		boolean important = IMPORTANT.contains(topic);
		long now = level.getGameTime();
		if (!important && now - this.lastSpoke < SMALL_TALK_GAP) {
			// She's still talking (or only just finished). Let the last line breathe.
			return;
		}
		this.lastSpoke = now;
		String key = "queen.hivemind." + topic + "." + this.random.nextInt(LINES.getOrDefault(topic, 1));
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) > SPEECH_RANGE * SPEECH_RANGE) {
				continue;
			}
			boolean addressed = listener == null || listener == player;
			if (ServerPlayNetworking.canSend(player, QueenSpeechPayload.TYPE)) {
				ServerPlayNetworking.send(player, new QueenSpeechPayload(this.getId(), key, addressed, addressed ? favor : -1, important));
			} else if (addressed) {
				MutableComponent line = Component.translatable("queen.hivemind.speaker", Component.translatable(key)).withStyle(ChatFormatting.GOLD);
				if (favor >= 0) {
					line.append(Component.translatable("queen.hivemind.favor", favor).withStyle(ChatFormatting.GRAY));
				}
				player.sendOverlayMessage(line);
			}
		}
		this.idleChatCooldown = Math.max(this.idleChatCooldown, IDLE_CHAT_AFTER_TALK);
	}

	public void say(final @Nullable Player listener, final String topic) {
		this.say(listener, topic, -1);
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		boolean raid = this.raidActive();
		if (--this.idleChatCooldown <= 0) {
			this.idleChatCooldown = Mth.nextInt(this.random, IDLE_CHAT_MIN, IDLE_CHAT_MAX);
			Player near = level.getNearestPlayer(this, IDLE_CHAT_RANGE);
			if (near != null && !near.isSpectator() && !raid) {
				this.say(near, "idle");
			}
		}
		if (this.nurseryCooldown > 0) {
			this.nurseryCooldown--;
		}
		if (raid) {
			if (this.tickCount % RALLY_INTERVAL == 0) {
				this.rally(level);
			}
		} else if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
			this.heal(2.0F);
		}
	}

	/**
	 * During a raid the queen sends out a royal command: guards fight harder for a while and anyone
	 * defending her gets a little regeneration.
	 */
	private void rally(final ServerLevel level) {
		AABB area = this.getBoundingBox().inflate(RALLY_RANGE);
		for (GuardBee guard : level.getEntitiesOfClass(GuardBee.class, area, GuardBee::isAlive)) {
			guard.addEffect(new MobEffectInstance(MobEffects.STRENGTH, RALLY_INTERVAL + 40, 0), this);
			guard.addEffect(new MobEffectInstance(MobEffects.SPEED, RALLY_INTERVAL + 40, 0), this);
		}
		for (Player player : level.getEntitiesOfClass(Player.class, area, p -> p.isAlive() && !p.isSpectator())) {
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 4, 0), this);
		}
		this.swingForAttack(InteractionHand.OFF_HAND);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEEHIVE_WORK, SoundSource.NEUTRAL, 2.0F, 0.6F);
		for (int i = 0; i < 16; i++) {
			double angle = i * Math.PI / 8;
			level.sendParticles(ParticleTypes.WAX_ON, this.getX() + Math.cos(angle) * 1.5, this.getY() + 1.0, this.getZ() + Math.sin(angle) * 1.5, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** Walks back to her throne whenever she isn't busy, and always when a raid starts. */
	private class ReturnToThroneGoal extends Goal {
		ReturnToThroneGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		private boolean awayFromThrone() {
			return QueenBee.this.distanceToSqr(Vec3.atBottomCenterOf(QueenBee.this.throne())) > 1.5 * 1.5;
		}

		@Override
		public boolean canUse() {
			if (QueenBee.this.raidActive()) {
				return this.awayFromThrone();
			}
			return this.awayFromThrone() && !QueenBee.this.tending && QueenBee.this.navigation.isDone();
		}

		@Override
		public boolean canContinueToUse() {
			return this.awayFromThrone() && QueenBee.this.navigation.isInProgress();
		}

		@Override
		public void start() {
			BlockPos throne = QueenBee.this.throne();
			boolean walking = QueenBee.this.navigation.moveTo(throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5, QueenBee.this.raidActive() ? 1.6 : 1.0);
			if (!walking && QueenBee.this.level() instanceof ServerLevel level) {
				// No way back on foot (something's in the way). She's the queen; the workers carry her.
				level.sendParticles(ParticleTypes.POOF, QueenBee.this.getX(), QueenBee.this.getY() + 1.0, QueenBee.this.getZ(), 8, 0.4, 0.6, 0.4, 0.02);
				QueenBee.this.snapTo(throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5, 0.0F, 0.0F);
			}
		}

		@Override
		public void stop() {
			if (!this.awayFromThrone()) {
				// Settle back in facing the hive entrance.
				QueenBee.this.setYRot(0.0F);
				QueenBee.this.setYBodyRot(0.0F);
			}
		}
	}

	/**
	 * Every minute or two the queen gets up and walks to one of the nursery patches. If a cell there is
	 * empty she lays a new egg in it; otherwise she hums to the little ones for a bit. Then she heads home.
	 */
	private class TendNurseryGoal extends Goal {
		private static final int GIVE_UP_TICKS = 20 * 20;
		private static final int HUM_TICKS = 20 * 3;
		private @Nullable BlockPos patch;
		private int ticks;
		private int humming;

		TendNurseryGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (QueenBee.this.nurseryCooldown > 0 || QueenBee.this.raidActive() || !(QueenBee.this.level() instanceof ServerLevel level)) {
				return false;
			}
			QueenBee.this.nurseryCooldown = Mth.nextInt(QueenBee.this.random, NURSERY_VISIT_MIN, NURSERY_VISIT_MAX);
			int index = HiveLayout.indexAt(QueenBee.this.position());
			if (index < 0 || !HiveLayout.isHiveLevel(level)) {
				return false;
			}
			if (!QueenBee.this.hasHome()) {
				// A queen placed by hand remembers where she was put, so she has somewhere to come back to.
				QueenBee.this.setHomeTo(QueenBee.this.blockPosition(), 2);
			}
			// Patches with empty cells come first; otherwise any patch will do for a visit.
			List<BlockPos> patches = HiveLayout.nurseryCenters(index);
			List<BlockPos> needy = patches.stream().filter(p -> !emptyCells(level, p).isEmpty()).toList();
			List<BlockPos> choices = needy.isEmpty() ? patches : needy;
			this.patch = choices.get(QueenBee.this.random.nextInt(choices.size()));
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return this.patch != null && this.ticks < GIVE_UP_TICKS && !QueenBee.this.raidActive();
		}

		@Override
		public void start() {
			this.ticks = 0;
			this.humming = 0;
			QueenBee.this.tending = true;
			if (this.patch != null) {
				QueenBee.this.navigation.moveTo(this.patch.getX() + 0.5, this.patch.getY() + 1, this.patch.getZ() + 0.5, 1.0);
			}
		}

		@Override
		public void stop() {
			this.patch = null;
			QueenBee.this.tending = false;
			QueenBee.this.navigation.stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			this.ticks++;
			if (this.patch == null || !(QueenBee.this.level() instanceof ServerLevel level)) {
				return;
			}
			Vec3 target = Vec3.atBottomCenterOf(this.patch.above());
			QueenBee.this.getLookControl().setLookAt(target.x, target.y, target.z);
			double dx = QueenBee.this.getX() - target.x;
			double dz = QueenBee.this.getZ() - target.z;
			if (dx * dx + dz * dz > 2.5 * 2.5) {
				if (QueenBee.this.navigation.isDone()) {
					QueenBee.this.navigation.moveTo(target.x, target.y, target.z, 1.0);
				}
				return;
			}
			QueenBee.this.navigation.stop();
			if (this.humming == 0) {
				this.arrive(level);
			}
			if (++this.humming >= HUM_TICKS) {
				this.patch = null;
			} else if (this.humming % 10 == 0) {
				level.sendParticles(ParticleTypes.NOTE, target.x, target.y + 0.6, target.z, 1, 0.8, 0.1, 0.8, 0.0);
			}
		}

		private void arrive(final ServerLevel level) {
			List<BlockPos> empty = emptyCells(level, this.patch);
			QueenBee.this.swingForAttack(InteractionHand.MAIN_HAND);
			// She lays a couple of eggs per visit.
			for (int i = 0; i < Math.min(2, empty.size()); i++) {
				BlockPos cell = empty.get(i);
				EmptyBroodCellBlock.layEgg(level, cell);
				level.sendParticles(ParticleTypes.WAX_ON, cell.getX() + 0.5, cell.getY() + 1.1, cell.getZ() + 0.5, 6, 0.25, 0.1, 0.25, 0.02);
				level.playSound(null, cell, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.NEUTRAL, 0.8F, 1.4F);
			}
			Player near = level.getNearestPlayer(QueenBee.this, IDLE_CHAT_RANGE);
			if (near != null && !near.isSpectator() && QueenBee.this.random.nextInt(3) == 0) {
				QueenBee.this.say(near, "nursery");
			}
		}

		private static List<BlockPos> emptyCells(final ServerLevel level, final BlockPos patch) {
			List<BlockPos> cells = new ArrayList<>();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos cell = patch.offset(dx, 0, dz);
					if (HiveLayout.isNurseryCell(dx, dz) && level.getBlockState(cell).is(ModBlocks.EMPTY_BROOD_CELL)) {
						cells.add(cell);
					}
				}
			}
			return cells;
		}
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("favor", FAVOR_CODEC, Map.copyOf(this.favor));
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.favor.clear();
		input.read("favor", FAVOR_CODEC).ifPresent(this.favor::putAll);
		// Queens from before she could walk saved a movement speed of zero.
		AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null && speed.getBaseValue() < WALK_SPEED) {
			speed.setBaseValue(WALK_SPEED);
		}
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.BEE_LOOP;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 120;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.BEE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BEE_DEATH;
	}
}
