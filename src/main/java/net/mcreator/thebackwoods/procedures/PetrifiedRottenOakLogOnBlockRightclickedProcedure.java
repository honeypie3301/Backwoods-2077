package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.thebackwoods.init.TheBackwoodsModItems;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import javax.annotation.Nullable;

@EventBusSubscriber
public class PetrifiedRottenOakLogOnBlockRightclickedProcedure {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        execute(event, event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity());
    }

    public static boolean execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        return execute(null, world, x, y, z, entity);
    }

    private static boolean execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null)
            return false;

        LivingEntity livingEntity = (LivingEntity) entity;
        ItemStack mainHand = livingEntity.getMainHandItem();
        ItemStack offHand = livingEntity.getOffhandItem();
        ResourceLocation axeTag = ResourceLocation.parse("minecraft:axes");

        if ((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_LOG.get() && 
           (mainHand.is(ItemTags.create(axeTag)) || offHand.is(ItemTags.create(axeTag)))) {

            // FIX: Use CancellationResult to stop the torch placement
            if (event instanceof PlayerInteractEvent.RightClickBlock _rbe) {
                _rbe.setCanceled(true); // Direct cancel
                _rbe.setCancellationResult(InteractionResult.SUCCESS); // Tells the game "I'm done here"
            }

            InteractionHand activeHand = mainHand.is(ItemTags.create(axeTag)) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack axeStack = livingEntity.getItemInHand(activeHand);

            // 1. Swing
            livingEntity.swing(activeHand, true);

            // 2. Damage (Survival)
            if (!(entity instanceof Player _plr && _plr.getAbilities().instabuild)) {
                if (world instanceof ServerLevel _level) {
                    axeStack.hurtAndBreak(20, _level, null, _stkprov -> {}); // Damage
                }
            }

            // 3. Swap Block
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockState _bso = world.getBlockState(_bp);
            BlockState _bsn = TheBackwoodsModBlocks.PETRIFIED_STRIPPED_ROTTEN_OAK_LOG.get().defaultBlockState();
            
            for (Property<?> _propertyOld : _bso.getProperties()) {
                Property _propertyNew = _bsn.getBlock().getStateDefinition().getProperty(_propertyOld.getName());
                if (_propertyNew != null && _bsn.getValue(_propertyNew) != null) {
                    try {
                        _bsn = _bsn.setValue(_propertyNew, _bso.getValue(_propertyOld));
                    } catch (Exception e) {}
                }
            }
            world.setBlock(_bp, _bsn, 3);

            // 4. Sound
            if (world instanceof Level _level) {
                _level.playSound(null, _bp, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.strip")), SoundSource.BLOCKS, 1, 1);
            }

            // Chance
            if (Math.random() < (1.0 / 23.0)) {
                if (world instanceof ServerLevel _level) {
                    ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack(TheBackwoodsModItems.PETRIFIED_BARK.get()));
                    entityToSpawn.setPickUpDelay(15);
                    _level.addFreshEntity(entityToSpawn);
                }
            }
            
            return true;
        }
        return false;
    }
}