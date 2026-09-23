package net.mcreator.thebackwoods.procedures;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class LossPlayerLeavesDimensionProcedure {

    private static final int WEAKNESS_DURATION = 12000;
    private static final int WEAKNESS_AMPLIFIER = 1;

    private static final int SLOWNESS_DURATION = 11000;
    private static final int SLOWNESS_AMPLIFIER = 0;

    private static final int DIG_SLOWDOWN_DURATION = 9700;
    private static final int DIG_SLOWDOWN_AMPLIFIER = 0;

    private static final int UNLUCK_DURATION = 9870;
    private static final int UNLUCK_AMPLIFIER = 0;

    private static final int HUNGER_DURATION = 12000;
    private static final int HUNGER_AMPLIFIER = 1;

    // Number of hotbar slots (0-8)
    private static final int HOTBAR_SIZE = 9;

    // Number of random hotbar swaps to perform for the shuffle
    private static final int HOTBAR_SHUFFLE_SWAPS = 12;

    public static void execute(LevelAccessor world, Entity entity) {
        if (entity == null)
            return;
        if (world.isClientSide() || !(entity instanceof Player player))
            return;

        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_DURATION, WEAKNESS_AMPLIFIER, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, DIG_SLOWDOWN_DURATION, DIG_SLOWDOWN_AMPLIFIER, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, UNLUCK_DURATION, UNLUCK_AMPLIFIER, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, HUNGER_DURATION, HUNGER_AMPLIFIER, false, false));

        shuffleHotbar(player);
        dropRandomItem(player);
    }

    private static void shuffleHotbar(Player player) {
        Inventory inventory = player.getInventory();
        Random random = new Random();
        for (int i = 0; i < HOTBAR_SHUFFLE_SWAPS; i++) {
            int slotA = random.nextInt(HOTBAR_SIZE);
            int slotB = random.nextInt(HOTBAR_SIZE);
            if (slotA == slotB)
                continue;
            ItemStack temp = inventory.getItem(slotA).copy();
            inventory.setItem(slotA, inventory.getItem(slotB).copy());
            inventory.setItem(slotB, temp);
        }
    }

    private static void dropRandomItem(Player player) {
        Inventory inventory = player.getInventory();
        Random random = new Random();
        List<Integer> occupiedSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!inventory.getItem(i).isEmpty())
                occupiedSlots.add(i);
        }
        if (occupiedSlots.isEmpty())
            return;
        int chosenSlot = occupiedSlots.get(random.nextInt(occupiedSlots.size()));
        ItemStack toDrop = inventory.getItem(chosenSlot).copy();
        inventory.setItem(chosenSlot, ItemStack.EMPTY);
        player.drop(toDrop, false);
    }
}