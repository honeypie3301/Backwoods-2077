package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class PetrifiedSplinteredOakWoodEntityWalksOnTheBlockProcedure {
	public static boolean execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return false;
		if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")))) {
			return false;
		}
		if ((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY).getItem() == Blocks.AIR.asItem()
				|| (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY).getItem() == Items.LEATHER_BOOTS) {
			if (world.getLevelData().getGameTime() % 30 == 0) {
				entity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.THORNS)), (float) 1.5);
			}
		} else {
			if (world.getLevelData().getGameTime() % 60 == 0) {
				if (world instanceof ServerLevel _level) {
					(entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY).hurtAndBreak(1, _level, null, _stkprov -> {
					});
				}
			}
		}
		return true;
	}
}