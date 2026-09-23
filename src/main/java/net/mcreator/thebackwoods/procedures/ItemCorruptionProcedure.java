package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.mcreator.thebackwoods.network.TheBackwoodsModVariables;
import net.mcreator.thebackwoods.init.TheBackwoodsModItems;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;

import javax.annotation.Nullable;

@EventBusSubscriber
public class ItemCorruptionProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double slot = 0;
		double itemCount = 0;
		double corruptionChance = 0;
		slot = 0;
		corruptionChance = 0;
		if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:loss"))) {
			{
				TheBackwoodsModVariables.PlayerVariables _vars = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
				_vars.lossTimer = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).lossTimer + 1;
				_vars.markSyncDirty();
			}
			if (entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES).lossTimer >= 200) {
				corruptionChance = 0.000116;
			}
		} else {
			{
				TheBackwoodsModVariables.PlayerVariables _vars = entity.getData(TheBackwoodsModVariables.PLAYER_VARIABLES);
				_vars.lossTimer = 0;
				_vars.markSyncDirty();
			}
		}
		if (hasEntityInInventory(entity, new ItemStack(TheBackwoodsModBlocks.FADED_BLOCK.get()))) {
			if (corruptionChance < 0.00005) {
				corruptionChance = 0.00005;
			}
		}
		if (corruptionChance > 0) {
			if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandlerIter) {
				for (int _idx = 0; _idx < _modHandlerIter.getSlots(); _idx++) {
					ItemStack itemstackiterator = _modHandlerIter.getStackInSlot(_idx).copy();
					if (Mth.nextDouble(RandomSource.create(), 0, 1) < corruptionChance) {
						if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("minecraft:pickaxes"))) && !itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:fade_immune")))
								&& !(itemstackiterator.getItem() == TheBackwoodsModItems.FADED_PICKAXE.get())) {
							if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
								ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_PICKAXE.get()).copy();
								_setstack.setCount(1);
								_modHandler.setStackInSlot((int) slot, _setstack);
							}
						} else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("minecraft:axes"))) && !itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:fade_immune")))
								&& !(itemstackiterator.getItem() == TheBackwoodsModItems.FADED_AXE.get())) {
							if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
								ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_AXE.get()).copy();
								_setstack.setCount(1);
								_modHandler.setStackInSlot((int) slot, _setstack);
							}
						} else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("minecraft:shovels"))) && !itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:fade_immune")))
								&& !(itemstackiterator.getItem() == TheBackwoodsModItems.FADED_SHOVEL.get())) {
							if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
								ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_SHOVEL.get()).copy();
								_setstack.setCount(1);
								_modHandler.setStackInSlot((int) slot, _setstack);
							}
						} else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("minecraft:hoes"))) && !itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:fade_immune")))
								&& !(itemstackiterator.getItem() == TheBackwoodsModItems.FADED_HOE.get())) {
							if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
								ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_HOE.get()).copy();
								_setstack.setCount(1);
								_modHandler.setStackInSlot((int) slot, _setstack);
							}
						} else if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("minecraft:swords"))) && !itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:fade_immune")))
								&& !(itemstackiterator.getItem() == TheBackwoodsModItems.FADED_SWORD.get())) {
							if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
								ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_SWORD.get()).copy();
								_setstack.setCount(1);
								_modHandler.setStackInSlot((int) slot, _setstack);
							}
						}
						if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:fade_blocks")))) {
							itemCount = itemstackiterator.getCount();
							if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
								ItemStack _setstack = new ItemStack(TheBackwoodsModBlocks.FADED_BLOCK.get()).copy();
								_setstack.setCount(itemstackiterator.getCount());
								_modHandler.setStackInSlot((int) slot, _setstack);
							}
						}
						if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:ingot_fade")))) {
							itemCount = itemstackiterator.getCount();
							if (itemstackiterator.getItem() == Items.IRON_INGOT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_IRON_INGOT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.GOLD_INGOT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_GOLD_INGOT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.COPPER_INGOT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COPPER_INGOT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.NETHERITE_INGOT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_NETHERITE_INGOT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.GOLD_NUGGET) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_GOLD_NUGGET.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							}
						}
						if (itemstackiterator.is(ItemTags.create(ResourceLocation.parse("the_backwoods:food_fade")))) {
							itemCount = itemstackiterator.getCount();
							if (itemstackiterator.getItem() == Items.BREAD) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_BREAD.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.WHEAT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_WHEAT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.BEETROOT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_BEETROOT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.DRIED_KELP) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_DRIED_KELP.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_ENCHANTED_GOLDEN_APPLE.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.COD) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COD.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == TheBackwoodsModItems.FADED_COOKED_COD.get()) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COOKED_COD.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.RABBIT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_RABBIT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.COOKED_RABBIT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COOKED_RABBIT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.CHICKEN) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_RAW_CHICKEN.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.BEEF) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_RAW_BEEF.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.PORKCHOP) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_PORKCHOP.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.MUTTON) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_MUTTON.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.COOKED_MUTTON) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COOKED_MUTTON.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.COOKED_PORKCHOP) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COOKED_PORKCHOP.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.PUMPKIN_PIE) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_PUMPKIN_PIE.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.COOKIE) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COOKIE.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.POTATO) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_POTATO.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.BAKED_POTATO) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_BAKED_POTATO.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.COOKED_CHICKEN) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COOKED_CHICKEN.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.APPLE) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_APPLE.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.GOLDEN_APPLE) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_GOLDEN_APPLE.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.GOLDEN_CARROT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_GOLDEN_CARROT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.COOKED_BEEF) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_COOKED_BEEF.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							} else if (itemstackiterator.getItem() == Items.CARROT) {
								if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) instanceof IItemHandlerModifiable _modHandler) {
									ItemStack _setstack = new ItemStack(TheBackwoodsModItems.FADED_CARROT.get()).copy();
									_setstack.setCount(itemstackiterator.getCount());
									_modHandler.setStackInSlot((int) slot, _setstack);
								}
							}
						}
					}
					slot = slot + 1;
				}
			}
		}
	}

	private static boolean hasEntityInInventory(Entity entity, ItemStack itemstack) {
		if (entity instanceof Player player)
			return player.getInventory().contains(stack -> !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack));
		return false;
	}
}