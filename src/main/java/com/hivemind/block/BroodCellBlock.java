package com.hivemind.block;

import com.hivemind.entity.QueenBee;
import com.hivemind.registry.ModItems;
import com.hivemind.world.HiveInteriorBuilder;
import com.hivemind.world.HiveLayout;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A honeycomb cell in the hive's nursery with a baby bee growing in it. Every so often the baby
 * gets hungry (feed it honey or royal jelly) or lonely (give it a pat with an empty hand). Each time
 * you look after it, it grows. Once it's capped it hatches on its own into a baby bee.
 */
public class BroodCellBlock extends Block {
	public static final int EGG = 0;
	public static final int CAPPED = 3;
	public static final IntegerProperty STAGE = IntegerProperty.create("stage", EGG, CAPPED);
	public static final EnumProperty<Need> NEED = EnumProperty.create("need", Need.class);
	/** Past this many bees in the hive, new hatchlings fly off to find a home of their own. */
	private static final int MAX_HIVE_BEES = 32;

	public enum Need implements StringRepresentable {
		NONE("none"),
		HUNGRY("hungry"),
		LONELY("lonely");

		private final String name;

		Need(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public BroodCellBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, EGG).setValue(NEED, Need.NONE));
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(STAGE, NEED);
	}

	@Override
	protected boolean isRandomlyTicking(final BlockState state) {
		return state.getValue(NEED) == Need.NONE;
	}

	@Override
	protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
		if (random.nextBoolean()) {
			return;
		}
		if (state.getValue(STAGE) == CAPPED) {
			this.hatch(state, level, pos);
		} else {
			// Eggs only need keeping warm; larvae can be hungry too.
			Need need = state.getValue(STAGE) == EGG || random.nextInt(3) == 0 ? Need.LONELY : Need.HUNGRY;
			level.setBlock(pos, state.setValue(NEED, need), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected InteractionResult useItemOn(
		final ItemStack itemStack,
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final Player player,
		final InteractionHand hand,
		final BlockHitResult hitResult
	) {
		boolean food = itemStack.is(Items.HONEY_BOTTLE) || itemStack.is(ModItems.ROYAL_JELLY);
		if (!food) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (state.getValue(NEED) != Need.HUNGRY) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable(this.moodKey(state, true)).withStyle(ChatFormatting.YELLOW));
			}
			return InteractionResult.SUCCESS;
		}
		if (level instanceof ServerLevel serverLevel) {
			if (itemStack.is(Items.HONEY_BOTTLE)) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
			} else {
				itemStack.consume(1, player);
			}
			serverLevel.playSound(null, pos, SoundEvents.HONEY_DRINK.value(), SoundSource.BLOCKS, 0.8F, 1.6F);
			this.cared(state, serverLevel, pos, player, ParticleTypes.HAPPY_VILLAGER, "message.hivemind.brood.fed");
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!player.getMainHandItem().isEmpty()) {
			// Holding something else; let it do its own thing.
			return InteractionResult.PASS;
		}
		if (state.getValue(NEED) != Need.LONELY) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable(this.moodKey(state, false)).withStyle(ChatFormatting.YELLOW));
			}
			return InteractionResult.SUCCESS;
		}
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.playSound(null, pos, SoundEvents.BEE_POLLINATE, SoundSource.BLOCKS, 0.8F, 1.8F);
			this.cared(state, serverLevel, pos, player, ParticleTypes.HEART, "message.hivemind.brood.patted");
		}
		return InteractionResult.SUCCESS;
	}

	/** What the cell says about itself when you offer it something it doesn't need right now. */
	private String moodKey(final BlockState state, final boolean offeredFood) {
		return switch (state.getValue(NEED)) {
			case HUNGRY -> "message.hivemind.brood.wants_food";
			case LONELY -> "message.hivemind.brood.wants_pat";
			case NONE -> state.getValue(STAGE) == CAPPED ? "message.hivemind.brood.capped" : offeredFood ? "message.hivemind.brood.full" : "message.hivemind.brood.content";
		};
	}

	private void cared(final BlockState state, final ServerLevel level, final BlockPos pos, final Player player, final ParticleOptions particle, final String message) {
		int stage = Math.min(CAPPED, state.getValue(STAGE) + 1);
		level.setBlock(pos, state.setValue(STAGE, stage).setValue(NEED, Need.NONE), Block.UPDATE_CLIENTS);
		level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 4, 0.25, 0.1, 0.25, 0.0);

		QueenBee queen = findQueen(level, pos);
		if (queen != null) {
			queen.adjustFavor(player, 1);
		}
		if (stage == CAPPED) {
			// The nurse bees seal the cell, and a little royal jelly is left over for whoever raised it.
			level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
			Block.popResourceFromFace(level, pos, Direction.UP, new ItemStack(ModItems.ROYAL_JELLY));
			player.sendOverlayMessage(Component.translatable("message.hivemind.brood.sealed").withStyle(ChatFormatting.GOLD));
			if (queen != null) {
				queen.say(player, "cocoon");
			}
		} else {
			player.sendOverlayMessage(Component.translatable(message).withStyle(ChatFormatting.YELLOW));
		}
	}

	private static QueenBee findQueen(final ServerLevel level, final BlockPos pos) {
		int index = HiveLayout.indexAt(Vec3.atCenterOf(pos));
		if (index < 0 || !HiveLayout.isHiveLevel(level)) {
			return null;
		}
		List<QueenBee> queens = level.getEntitiesOfClass(QueenBee.class, HiveLayout.bounds(index), QueenBee::isAlive);
		return queens.isEmpty() ? null : queens.getFirst();
	}

	private void hatch(final BlockState state, final ServerLevel level, final BlockPos pos) {
		level.setBlock(pos, state.setValue(STAGE, EGG).setValue(NEED, Need.NONE), Block.UPDATE_CLIENTS);
		level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.5F);
		level.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.05);

		int nearbyBees = level.getEntitiesOfClass(Bee.class, new AABB(pos).inflate(40.0), Bee::isAlive).size();
		if (nearbyBees >= MAX_HIVE_BEES) {
			level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.05);
			return;
		}
		Bee baby = EntityTypes.BEE.create(level, EntitySpawnReason.BREEDING);
		if (baby == null) {
			return;
		}
		baby.setAge(-24000);
		baby.snapTo(pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
		if (HiveLayout.isHiveLevel(level)) {
			AttributeInstance scale = baby.getAttribute(Attributes.SCALE);
			if (scale != null) {
				scale.setBaseValue(HiveInteriorBuilder.RESIDENT_BEE_SCALE);
			}
			int index = HiveLayout.indexAt(Vec3.atCenterOf(pos));
			if (index >= 0) {
				baby.setHomeTo(HiveLayout.center(index), 20);
			}
			baby.setPersistenceRequired();
			baby.addTag(HiveInteriorBuilder.RESIDENT_TAG);
		}
		level.addFreshEntity(baby);
		level.sendParticles(ParticleTypes.HEART, baby.getX(), baby.getY() + 0.5, baby.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
	}

	@Override
	public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
		Need need = state.getValue(NEED);
		if (need == Need.NONE || random.nextInt(4) != 0) {
			return;
		}
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
		if (need == Need.LONELY) {
			// Note particles take their color from the x speed; this one is a soft blue.
			level.addParticle(ParticleTypes.NOTE, x, pos.getY() + 1.2, z, 0.6, 0.0, 0.0);
		} else {
			level.addParticle(ParticleTypes.FALLING_HONEY, x, pos.getY() + 1.5, z, 0.0, 0.0, 0.0);
		}
	}
}
