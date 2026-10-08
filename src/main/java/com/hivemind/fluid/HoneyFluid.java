package com.hivemind.fluid;

import com.hivemind.registry.ModBlocks;
import com.hivemind.registry.ModFluids;
import com.hivemind.registry.ModItems;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Liquid honey. Thick and slow: it creeps a short way like lava does in the overworld, never makes new
 * sources on its own, and wading through it slows you down. Sitting in it heals you, the way honey
 * does in Terraria, and you can't take fall damage landing in it. It sets into a honey block when it
 * touches water and crisps into honeycomb when it touches lava.
 */
public abstract class HoneyFluid extends FlowingFluid {
	/** Each honey source holds this many bottles' worth before it's used up. */
	public static final int BOTTLES_PER_SOURCE = 4;
	private static final double WADE_SLOWDOWN = 0.6;
	private static final double MAX_SINK_SPEED = 0.08;
	private static final int REGEN_TICKS = 20 * 5;
	/** Last tick each entity was slowed, so standing in two honey blocks at once doesn't slow you twice. */
	private static final Map<Entity, Integer> LAST_SLOWED = Collections.synchronizedMap(new WeakHashMap<>());

	@Override
	public Fluid getFlowing() {
		return ModFluids.FLOWING_HONEY;
	}

	@Override
	public Fluid getSource() {
		return ModFluids.HONEY;
	}

	@Override
	public Item getBucket() {
		return ModItems.HONEY_BUCKET;
	}

	@Override
	public boolean isSame(final Fluid other) {
		return other == ModFluids.HONEY || other == ModFluids.FLOWING_HONEY;
	}

	@Override
	protected boolean canConvertToSource(final ServerLevel level) {
		return false;
	}

	@Override
	protected void beforeDestroyingBlock(final LevelAccessor level, final BlockPos pos, final BlockState state) {
		// Honey smothers whatever it flows over (grass, flowers, torches); they just drop as items.
		Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
	}

	@Override
	protected int getSlopeFindDistance(final LevelReader level) {
		return 2;
	}

	@Override
	protected int getDropOff(final LevelReader level) {
		return 2;
	}

	@Override
	public int getTickDelay(final LevelReader level) {
		return 40;
	}

	@Override
	protected boolean canBeReplacedWith(final FluidState state, final BlockGetter level, final BlockPos pos, final Fluid other, final Direction direction) {
		return false;
	}

	@Override
	protected float getExplosionResistance() {
		return 100.0F;
	}

	@Override
	protected BlockState createLegacyBlock(final FluidState fluidState) {
		return ModBlocks.HONEY.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL);
	}

	@Override
	protected @Nullable ParticleOptions getDripParticle() {
		return ParticleTypes.DRIPPING_HONEY;
	}

	@Override
	protected void entityInside(final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier) {
		if (entity instanceof Bee) {
			// Bees know how to handle honey.
			return;
		}
		entity.resetFallDistance();
		Integer last = LAST_SLOWED.put(entity, entity.tickCount);
		if (last == null || last != entity.tickCount) {
			Vec3 motion = entity.getDeltaMovement();
			// Sluggish sideways, and you sink slowly instead of dropping. Jumping still works, so you can climb out.
			entity.setDeltaMovement(motion.x * WADE_SLOWDOWN, Math.max(motion.y, -MAX_SINK_SPEED), motion.z * WADE_SLOWDOWN);
		}
		if (!level.isClientSide() && entity instanceof LivingEntity living) {
			// Topping up only when it's nearly run out lets the effect actually tick (it heals on fixed beats).
			MobEffectInstance regen = living.getEffect(MobEffects.REGENERATION);
			if (regen == null || regen.getAmplifier() == 0 && regen.getDuration() < 10) {
				living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 0, true, true));
			}
		}
	}

	public static class Flowing extends HoneyFluid {
		@Override
		protected void createFluidStateDefinition(final StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(final FluidState fluidState) {
			return fluidState.getValue(LEVEL);
		}

		@Override
		public boolean isSource(final FluidState fluidState) {
			return false;
		}
	}

	public static class Source extends HoneyFluid {
		@Override
		public int getAmount(final FluidState fluidState) {
			return 8;
		}

		@Override
		public boolean isSource(final FluidState fluidState) {
			return true;
		}
	}
}
