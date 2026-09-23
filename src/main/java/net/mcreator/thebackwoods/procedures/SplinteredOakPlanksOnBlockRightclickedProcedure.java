package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import javax.annotation.Nullable;

@EventBusSubscriber
public class SplinteredOakPlanksOnBlockRightclickedProcedure {
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND)
			return;
		execute(event, event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == TheBackwoodsModBlocks.SPLINTERED_OAK_PLANKS.get()) {
			if (!world.isClientSide()) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).is(ItemTags.create(ResourceLocation.parse("minecraft:axes")))
						|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).is(ItemTags.create(ResourceLocation.parse("minecraft:is_shears")))) {
					if (event instanceof ICancellableEvent _cancellable) {
						_cancellable.setCanceled(true);
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.strip")), SoundSource.BLOCKS, (float) 0.5, 1);
						} else {
							_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.strip")), SoundSource.BLOCKS, (float) 0.5, 1, false);
						}
					}
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.DUST_PLUME, x, y, z, 15, 0.5, 0.5, 0.5, 0.001);
					for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 0, 2); index0++) {
						if (world instanceof ServerLevel _level) {
							ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(TheBackwoodsModItems.SHARPENED_SPLINTER_SHARD.get()));
							entityToSpawn.setPickUpDelay(10);
							_level.addFreshEntity(entityToSpawn);
						}
					}
					world.setBlock(BlockPos.containing(x, y, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
					if (Math.random() < 0.25) {
						world.destroyBlock(BlockPos.containing(x, y, z), false);
					}
					if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
						if (world instanceof ServerLevel _level) {
							(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).hurtAndBreak(1, _level, null, _stkprov -> {
							});
						}
					}
				}
			}
		}
	}
}