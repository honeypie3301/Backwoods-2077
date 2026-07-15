package net.mcreator.thebackwoods.procedures;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.mcreator.thebackwoods.entity.PetrifiedLogSplinterEntity;
import net.mcreator.thebackwoods.init.TheBackwoodsModBlocks;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
// 1.21.1 neoforge
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import javax.annotation.Nullable;
import java.util.Comparator;

@EventBusSubscriber
public class PetrifiedLogSplinterOnEntityTickUpdateProcedure {

    private static final double WATCH_DOT_THRESHOLD = 0.5;
    private static final double ACTIVE_MOVE_SPEED = 0.3;
    private static final float MINE_SPEED_MULTIPLIER = 12f;
    private static final float MINE_SPEED_BASE = 15f;
    private static final float MAX_BREAKABLE_HARDNESS = 60f;
    private static final double TARGET_RANGE = 56;
    private static final double MINE_RAY_DISTANCE = 2.0;
    private static final double ROSE_WILT_TICKS = 400;
    private static final int ROSE_SCAN_XZ = 6;
    private static final int ROSE_SCAN_Y = 3;
    private static final int RAGE_WATCH_THRESHOLD = 600;
    private static final int RAGE_THRESHOLD_HIT_BONUS = 150;
    private static final double RAGE_ESCAPE_RANGE = 18.0;

    private static final String K_RAGE_BONUS = "rage_watch_bonus";
    private static final String K_LAST_HURT_TIME = "rage_last_hurt_time";

    public static void execute() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof PetrifiedLogSplinterEntity petrified) {
            execute(null, petrified.level(), petrified.getX(), petrified.getY(), petrified.getZ(), petrified);
        }
    }

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        execute(null, world, x, y, z, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof PetrifiedLogSplinterEntity petrified))
            return;

        if (petrified.getTarget() instanceof Player p && (p.isCreative() || p.isSpectator())) {
            petrified.setTarget(null);
        }
        if (petrified.getLastHurtByMob() instanceof Player p && (p.isCreative() || p.isSpectator())) {
            petrified.setLastHurtByMob(null);
        }

        if (petrified.isPassenger() && (petrified.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat ||
            petrified.getVehicle() instanceof net.minecraft.world.entity.vehicle.ChestBoat)) {
            petrified.stopRiding();
            petrified.setDeltaMovement(petrified.getDeltaMovement().add(0, 0.2, 0));
        }

        Player foundPlayerDirect = (Player) findEntityInWorldRange(world, Player.class, x, y, z, TARGET_RANGE);
        if (world instanceof Level level && level.isClientSide()) {
            return;
        }
        LivingEntity foundPlayer = foundPlayerDirect;

        LivingEntity prioritizedMob = null;
        String attackerUUIDStr = petrified.getPersistentData().getString("splinter_attacker_uuid");
        if (!attackerUUIDStr.isEmpty() && world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            try {
                java.util.UUID attackerUUID = java.util.UUID.fromString(attackerUUIDStr);
                net.minecraft.world.entity.Entity foundEntity = serverLevel.getEntity(attackerUUID);
                if (foundEntity instanceof LivingEntity livingAttacker && livingAttacker.isAlive() && petrified.distanceToSqr(livingAttacker) < TARGET_RANGE * TARGET_RANGE) {
                    boolean isWoodbound = livingAttacker.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
                    if (!isWoodbound && !(livingAttacker instanceof Player)) {
                        prioritizedMob = livingAttacker;
                    }
                }
            } catch (Exception e) {}
        }

        LivingEntity attacker = petrified.getLastHurtByMob();
        if (attacker != null) {
            boolean isWoodbound = attacker.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
            if (isWoodbound) {
                if (petrified.getLastHurtByMob() == attacker) {
                    petrified.setLastHurtByMob(null);
                }
                attacker = null;
            } else if (attacker.isAlive() && petrified.distanceToSqr(attacker) < TARGET_RANGE * TARGET_RANGE) {
                if (!(attacker instanceof Player)) {
                    prioritizedMob = attacker;
                    petrified.getPersistentData().putString("splinter_attacker_uuid", attacker.getUUID().toString());
                }
            }
        }

        boolean hadPrioritizedMob = !attackerUUIDStr.isEmpty();
        if (hadPrioritizedMob && prioritizedMob == null) {
            resetState(petrified);
            petrified.getPersistentData().putInt(K_RAGE_BONUS, 0);
            if (petrified.getLastHurtByMob() != null) {
                petrified.setLastHurtByMob(null);
            }
            petrified.getPersistentData().remove("splinter_attacker_uuid");
        }

        if (prioritizedMob != null) {
            foundPlayer = prioritizedMob;
            petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_isEnraged, 1);
            if (petrified.getTarget() != prioritizedMob) {
                petrified.setTarget(prioritizedMob);
            }
            if (petrified.getLastHurtByMob() != prioritizedMob) {
                petrified.setLastHurtByMob(prioritizedMob);
            }
        } else {
            if (attacker instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator() && petrified.distanceToSqr(attacker) < TARGET_RANGE * TARGET_RANGE) {
                foundPlayer = p;
            }
            if (foundPlayer != null) {
                if (petrified.getTarget() != foundPlayer) {
                    petrified.setTarget(foundPlayer);
                }
            } else {
                if (petrified.getTarget() != null) {
                    petrified.setTarget(null);
                }
            }
        }

        if (foundPlayer != null) {
            boolean isWoodbound = foundPlayer.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
            if (isWoodbound) {
                foundPlayer = null;
            }
        }

        if (foundPlayer == null) {
            resetState(petrified);
            petrified.getPersistentData().putInt(K_RAGE_BONUS, 0);
            return;
        }

        int isEnraged = petrified.getEntityData().get(PetrifiedLogSplinterEntity.DATA_isEnraged);
        int watchTimer = petrified.getEntityData().get(PetrifiedLogSplinterEntity.DATA_watchTimer);

        // NEW: decrease threshold by +280 bonus each time hit by player
        if (petrified.getLastHurtByMob() != null) {
            boolean isWoodbound = petrified.getLastHurtByMob().getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:woodbound_entities")));
            if (!isWoodbound) {
                int lastSeenHurtTime = petrified.getPersistentData().getInt(K_LAST_HURT_TIME);
                int currentHurtTime = petrified.hurtTime;
                if (currentHurtTime > 0 && currentHurtTime != lastSeenHurtTime) {
                    int bonus = petrified.getPersistentData().getInt(K_RAGE_BONUS) + RAGE_THRESHOLD_HIT_BONUS;
                    petrified.getPersistentData().putInt(K_RAGE_BONUS, bonus);
                    petrified.getPersistentData().putInt(K_LAST_HURT_TIME, currentHurtTime);
                }
            }
        }
        int effectiveRageThreshold = Math.max(1, RAGE_WATCH_THRESHOLD - petrified.getPersistentData().getInt(K_RAGE_BONUS));

        boolean isWatched = false;
        if (foundPlayerDirect != null) {
            Vec3 toSplinter = petrified.getEyePosition().subtract(foundPlayerDirect.getEyePosition()).normalize();
            isWatched = foundPlayerDirect.getLookAngle().normalize().dot(toSplinter) > WATCH_DOT_THRESHOLD && foundPlayerDirect.hasLineOfSight(petrified);
        }

        double distToPlayer = foundPlayerDirect != null ? petrified.position().distanceTo(foundPlayerDirect.position()) : Double.MAX_VALUE;
        if (isEnraged == 1 && distToPlayer > RAGE_ESCAPE_RANGE) {
            resetState(petrified);
            isEnraged = 0;
        }

        if (isWatched && isEnraged == 0) {
            watchTimer++;
            petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_watchTimer, watchTimer);
            if (watchTimer >= effectiveRageThreshold) {
                petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_isEnraged, 1);
                petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_watchTimer, 0);
                isEnraged = 1;
            }
        } else if (!isWatched && isEnraged == 0) {
            petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_watchTimer, 0);
        }

        if ((isWatched && isEnraged == 0) || petrified.getEntityData().get(PetrifiedLogSplinterEntity.DATA_frozenByRose) == 1) {
            setSpeed(petrified, 0);
            if (petrified instanceof Mob mob) mob.getNavigation().stop();
            return;
        }

        petrified.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(foundPlayer.getX(), foundPlayer.getEyeY(), foundPlayer.getZ()));
        setSpeed(petrified, ACTIVE_MOVE_SPEED);

        Vec3 eyes = petrified.getEyePosition(1f);
        Vec3 view = petrified.getViewVector(1f);
        HitResult hit = world.clip(new ClipContext(eyes, eyes.add(view.scale(MINE_RAY_DISTANCE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, petrified));

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos facePos = ((BlockHitResult) hit).getBlockPos();
            BlockPos feetPos = new BlockPos(facePos.getX(), Mth.floor(petrified.getY()), facePos.getZ());

            boolean canMineFeet = canMine(world, feetPos, foundPlayer);
            boolean canMineFace = canMine(world, facePos, foundPlayer);

            if (canMineFeet || canMineFace) {
                int mineProgress = petrified.getEntityData().get(PetrifiedLogSplinterEntity.DATA_mineProgress) + 1;
                petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_mineProgress, mineProgress);

                if (petrified.tickCount % 6 == 0) {
                    petrified.swing(InteractionHand.MAIN_HAND);
                }

                float speedRef = canMineFeet ? world.getBlockState(feetPos).getDestroySpeed(world, feetPos) : world.getBlockState(facePos).getDestroySpeed(world, facePos);

                if (mineProgress > (speedRef * MINE_SPEED_MULTIPLIER + MINE_SPEED_BASE)) {
                    if (canMineFeet) world.destroyBlock(feetPos, false);
                    if (canMineFace) world.destroyBlock(facePos, false);

                    if (petrified instanceof Mob mob) mob.getNavigation().stop();
                    petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_mineProgress, 0);
                }
            } else {
                petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_mineProgress, 0);
            }
        } else {
            petrified.getEntityData().set(PetrifiedLogSplinterEntity.DATA_mineProgress, 0);
            handleVerticalPathing(world, petrified, foundPlayer);
        }

        boolean foundRose = false;
        if (foundPlayerDirect != null) {
            foundRose = checkHeldRose(foundPlayerDirect, petrified, world, foundPlayerDirect.getX(), foundPlayerDirect.getY(), foundPlayerDirect.getZ());
        }
        if (foundRose || (petrified.tickCount % 5 == 0 && checkNearbyRoseBlocks(world, petrified))) {
            setSpeed(petrified, 0);
            resetState(petrified);
            petrified.getPersistentData().putInt(K_RAGE_BONUS, 0);
        }

        // Melee reach range increase for non-players only (by 50%)
        LivingEntity target = petrified.getTarget();
        if (target != null && target.isAlive() && !(target instanceof Player)) {
            double reach = petrified.getBbWidth() + target.getBbWidth() + 0.8;
            double increasedReach = reach * 1.5;
            double distSqr = petrified.distanceToSqr(target);
            if (distSqr <= increasedReach * increasedReach) {
                int lastAttackTick = petrified.getPersistentData().getInt("last_melee_attack_tick");
                if (petrified.tickCount - lastAttackTick >= 20) {
                    petrified.doHurtTarget(target);
                    petrified.swing(InteractionHand.MAIN_HAND);
                    petrified.getPersistentData().putInt("last_melee_attack_tick", petrified.tickCount);
                }
            }
        }
    }

    private static void handleVerticalPathing(LevelAccessor world, PetrifiedLogSplinterEntity splinter, LivingEntity foundPlayer) {
        double heightDiff = foundPlayer.getY() - splinter.getY();
        double horizontalDist = splinter.position().distanceTo(foundPlayer.position());
        if (heightDiff >= 1.4 && horizontalDist < 12) {
            if (world instanceof ServerLevel serverLevel) {
                CommandSourceStack src = new CommandSourceStack(CommandSource.NULL, new Vec3(splinter.getX(), splinter.getY(), splinter.getZ()), Vec2.ZERO, serverLevel, 4, "", Component.literal(""), serverLevel.getServer(), null).withSuppressedOutput();
                serverLevel.getServer().getCommands().performPrefixedCommand(src, "fill ~ ~-1 ~ ~ ~-1 ~ the_backwoods:petrified_rotten_oak_wood replace minecraft:air");
            }
            if (splinter.onGround()) splinter.setDeltaMovement(new Vec3(splinter.getDeltaMovement().x(), 0.4, splinter.getDeltaMovement().z()));
            splinter.fallDistance = 0;
        } else if (heightDiff > -0.5 && heightDiff < 2.5) {
            Vec3 look = splinter.getLookAngle();
            BlockPos bridgePos = BlockPos.containing(splinter.getX() + look.x, splinter.getY() - 1, splinter.getZ() + look.z);
            if (!(world.getBlockFloorHeight(bridgePos) > 0)) {
                world.setBlock(bridgePos, TheBackwoodsModBlocks.PETRIFIED_ROTTEN_OAK_WOOD.get().defaultBlockState(), 3);
            }
        }
    }

    private static void resetState(PetrifiedLogSplinterEntity entity) {
        entity.getEntityData().set(PetrifiedLogSplinterEntity.DATA_watchTimer, 0);
        entity.getEntityData().set(PetrifiedLogSplinterEntity.DATA_isEnraged, 0);
    }

    private static void setSpeed(LivingEntity entity, double speed) {
        if (entity.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
            entity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
    }

    private static boolean canMine(LevelAccessor world, BlockPos pos, LivingEntity player) {
        float speed = world.getBlockState(pos).getDestroySpeed(world, pos);
        return speed >= 0 && speed < MAX_BREAKABLE_HARDNESS && pos.getY() != (int) (player.getY() - 2) && !world.getBlockState(pos).isAir();
    }

    private static boolean checkHeldRose(Entity holder, PetrifiedLogSplinterEntity splinter, LevelAccessor world, double x, double y, double z) {
        if (!(holder instanceof LivingEntity living)) return false;
        ItemStack main = living.getMainHandItem();
        ItemStack off = living.getOffhandItem();
        boolean hasRose = main.getItem() == TheBackwoodsModBlocks.ASH_ROSE.get().asItem() || off.getItem() == TheBackwoodsModBlocks.ASH_ROSE.get().asItem();
        if (hasRose) {
            if (main.getItem() == TheBackwoodsModBlocks.ASH_ROSE.get().asItem()) tickRoseItem(main, world, x, y, z);
            if (off.getItem() == TheBackwoodsModBlocks.ASH_ROSE.get().asItem()) tickRoseItem(off, world, x, y, z);
            return true;
        }
        return false;
    }

    private static void tickRoseItem(ItemStack rose, LevelAccessor world, double x, double y, double z) {
        double wiltTimer = rose.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("wilt_timer") + 1;
        CustomData.update(DataComponents.CUSTOM_DATA, rose, tag -> tag.putDouble("wilt_timer", wiltTimer));
        if (wiltTimer > ROSE_WILT_TICKS) {
            if (world instanceof Level level && !level.isClientSide()) {
                level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.NEUTRAL, 1, 1);
            }
            rose.shrink(1);
        }
    }

    private static boolean checkNearbyRoseBlocks(LevelAccessor world, PetrifiedLogSplinterEntity splinter) {
        for (int sx = -ROSE_SCAN_XZ; sx < ROSE_SCAN_XZ; sx++) {
            for (int sy = -ROSE_SCAN_Y; sy < ROSE_SCAN_Y; sy++) {
                for (int sz = -ROSE_SCAN_XZ; sz < ROSE_SCAN_XZ; sz++) {
                    if (world.getBlockState(BlockPos.containing(splinter.getX() + sx, splinter.getY() + sy, splinter.getZ() + sz)).getBlock() == TheBackwoodsModBlocks.ASH_ROSE.get()) return true;
                }
            }
        }
        return false;
    }

    private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range) {
        return world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> {
            if (e instanceof Player p) {
                return !p.isCreative() && !p.isSpectator();
            }
            return true;
        }).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).findFirst().orElse(null);
    }
}