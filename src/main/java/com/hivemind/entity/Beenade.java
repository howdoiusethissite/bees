package com.hivemind.entity;

import com.hivemind.registry.ModEntities;
import com.hivemind.registry.ModItems;
import com.hivemind.world.BeeSwarms;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A thrown jar of angry bees. It bursts on impact and lets out a swarm that goes after nearby monsters. */
public class Beenade extends ThrowableItemProjectile {
	public Beenade(final EntityType<? extends Beenade> type, final Level level) {
		super(type, level);
	}

	public Beenade(final Level level, final LivingEntity owner, final ItemStack itemStack) {
		super(ModEntities.BEENADE, owner, level, itemStack);
	}

	public Beenade(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
		super(ModEntities.BEENADE, x, y, z, level, itemStack);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.BEENADE;
	}

	@Override
	public void handleEntityEvent(final @EntityEvent.Value byte id) {
		if (id == 3) {
			ItemParticleOption shards = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(this.getItem()));
			for (int i = 0; i < 12; i++) {
				double dx = (this.random.nextDouble() - 0.5) * 0.3;
				double dz = (this.random.nextDouble() - 0.5) * 0.3;
				this.level().addParticle(shards, this.getX(), this.getY(), this.getZ(), dx, 0.15, dz);
				this.level().addParticle(ParticleTypes.FALLING_HONEY, this.getX() + dx * 3, this.getY() + 0.2, this.getZ() + dz * 3, 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected void onHitEntity(final EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		Entity entity = hitResult.getEntity();
		entity.hurt(this.damageSources().thrown(this, this.getOwner()), 1.0F);
	}

	@Override
	protected void onHit(final HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level) {
			level.broadcastEntityEvent(this, (byte)3);
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.3F);
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 1.2F, 1.1F);
			// Back off the surface a little so the bees don't spawn inside the block that was hit.
			BeeSwarms.release(level, this.position().subtract(this.getDeltaMovement().normalize().scale(0.4)), this.getOwner());
			this.discard();
		}
	}
}
