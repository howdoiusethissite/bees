package com.hivemind.entity;

import com.hivemind.Hivemind;
import com.hivemind.network.QueenSpeechPayload;
import com.hivemind.world.HiveEvents;
import com.hivemind.world.HiveLayout;
import com.hivemind.world.HiveRaids;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * The queen at the heart of every hive. She stays on her dais, accepts flowers, remembers who has
 * been kind (or rude) to her hive, and hands out gifts to her favorites and to those who defend her.
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
	private static final Map<String, Integer> LINES = Map.of(
		"greet", 5, "flower", 5, "gift", 4, "angry", 3, "idle", 6, "cocoon", 4, "raid_start", 3, "raid_won", 3, "raid_lost", 3
	);
	private static final double SPEECH_RANGE = 24.0;
	private static final double IDLE_CHAT_RANGE = 8.0;

	private final Map<UUID, Integer> favor = new HashMap<>();
	private final Map<UUID, Long> lastGift = new HashMap<>();
	private int idleChatCooldown = 20 * 20;

	public QueenBee(final EntityType<? extends QueenBee> type, final Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 120.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 0.6F));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
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
				// Nobody lays a hand on the queen.
				HiveEvents.angerHive(level, player);
				player.sendSystemMessage(Component.translatable("message.hivemind.queen_hit").withStyle(ChatFormatting.RED));
				this.say(player, "angry", -1);
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
		String key = "queen.hivemind." + topic + "." + this.random.nextInt(LINES.getOrDefault(topic, 1));
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) > SPEECH_RANGE * SPEECH_RANGE) {
				continue;
			}
			boolean addressed = listener == null || listener == player;
			if (ServerPlayNetworking.canSend(player, QueenSpeechPayload.TYPE)) {
				ServerPlayNetworking.send(player, new QueenSpeechPayload(this.getId(), key, addressed, addressed ? favor : -1));
			} else if (addressed) {
				MutableComponent line = Component.translatable("queen.hivemind.speaker", Component.translatable(key)).withStyle(ChatFormatting.GOLD);
				if (favor >= 0) {
					line.append(Component.translatable("queen.hivemind.favor", favor).withStyle(ChatFormatting.GRAY));
				}
				player.sendOverlayMessage(line);
			}
		}
		this.idleChatCooldown = Math.max(this.idleChatCooldown, 20 * 15);
	}

	public void say(final @Nullable Player listener, final String topic) {
		this.say(listener, topic, -1);
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		if (--this.idleChatCooldown <= 0) {
			this.idleChatCooldown = 20 * (40 + this.random.nextInt(50));
			Player near = level.getNearestPlayer(this, IDLE_CHAT_RANGE);
			if (near != null && !near.isSpectator() && !HiveRaids.isRaidActive(HiveLayout.indexAt(this.position()))) {
				this.say(near, "idle");
			}
		}
		if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth() && !HiveRaids.isRaidActive(HiveLayout.indexAt(this.position()))) {
			this.heal(2.0F);
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
