package com.hivemind.entity;

import com.hivemind.world.HiveRaids;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A bigger, armored bee that keeps watch over a hive. Neutral until provoked. Unlike a worker
 * bee it fights with a little spear and can sting as many times as it likes without dying.
 */
public class GuardBee extends PathfinderMob implements NeutralMob {
	private static final EntityDataAccessor<Long> DATA_ANGER_END_TIME = SynchedEntityData.defineId(GuardBee.class, EntityDataSerializers.LONG);
	private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(30, 50);
	private static final float STING_CHANCE = 0.35F;
	private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

	public GuardBee(final EntityType<? extends GuardBee> type, final Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setPathfindingMalus(PathType.FIRE, -1.0F);
		this.setPathfindingMalus(PathType.WATER, -1.0F);
		this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
		this.setPathfindingMalus(PathType.FENCE, -1.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 28.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.FLYING_SPEED, 0.7F)
			.add(Attributes.MOVEMENT_SPEED, 0.3F)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ATTACK_KNOCKBACK, 0.4)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_ANGER_END_TIME, -1L);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
		this.goalSelector.addGoal(5, new PatrolGoal());
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(GuardBee.class));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false, (target, level) -> HiveRaids.isInvader(target)));
		this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, true));
	}

	@Override
	protected PathNavigation createNavigation(final Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level) {
			@Override
			public boolean isStableDestination(final BlockPos pos) {
				return !this.level.getBlockState(pos.below()).isAir();
			}
		};
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(false);
		navigation.setRequiredPathLength(48.0F);
		return navigation;
	}

	@Override
	public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
		return level.getBlockState(pos).isAir() ? 10.0F : 0.0F;
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living && this.random.nextFloat() < STING_CHANCE) {
			// Guards keep their stingers, so they can do this again and again.
			int seconds = level.getDifficulty() == Difficulty.HARD ? 8 : level.getDifficulty() == Difficulty.NORMAL ? 5 : 0;
			if (seconds > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20, 0), this);
			}
			living.setStingerCount(living.getStingerCount() + 1);
			this.playSound(SoundEvents.BEE_STING, 1.0F, 0.8F);
		}
		return hit;
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		this.updatePersistentAnger(level, false);
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.addPersistentAngerSaveData(output);
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.readPersistentAngerSaveData(this.level(), input);
	}

	@Override
	public long getPersistentAngerEndTime() {
		return this.entityData.get(DATA_ANGER_END_TIME);
	}

	@Override
	public void setPersistentAngerEndTime(final long endTime) {
		this.entityData.set(DATA_ANGER_END_TIME, endTime);
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
		return this.persistentAngerTarget;
	}

	@Override
	public void setPersistentAngerTarget(final @Nullable EntityReference<LivingEntity> target) {
		this.persistentAngerTarget = target;
	}

	@Override
	public void startPersistentAngerTimer() {
		this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}

	@Override
	protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
	}

	@Override
	protected boolean omnidirectionalAirMover() {
		return true;
	}

	@Override
	public boolean isFlapping() {
		return !this.onGround() && this.tickCount % 2 == 0;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.BEE_LOOP;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 60;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.BEE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BEE_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.75F;
	}

	/** Drifts around near home, the way worker bees do, but never strays far from the hive. */
	private class PatrolGoal extends Goal {
		PatrolGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return GuardBee.this.navigation.isDone() && GuardBee.this.random.nextInt(10) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return GuardBee.this.navigation.isInProgress();
		}

		@Override
		public void start() {
			Vec3 target = this.findPos();
			if (target != null) {
				GuardBee.this.navigation.moveTo(GuardBee.this.navigation.createPath(BlockPos.containing(target), 1), 1.0);
			}
		}

		private @Nullable Vec3 findPos() {
			Vec3 direction;
			if (GuardBee.this.hasHome() && !GuardBee.this.isWithinHome()) {
				direction = Vec3.atCenterOf(GuardBee.this.getHomePosition()).subtract(GuardBee.this.position()).normalize();
			} else {
				direction = GuardBee.this.getViewVector(0.0F);
			}
			Vec3 pos = HoverRandomPos.getPos(GuardBee.this, 8, 7, direction.x, direction.z, (float) (Math.PI / 2), 3, 1);
			return pos != null ? pos : AirAndWaterRandomPos.getPos(GuardBee.this, 8, 4, -2, direction.x, direction.z, (float) (Math.PI / 2));
		}
	}
}
