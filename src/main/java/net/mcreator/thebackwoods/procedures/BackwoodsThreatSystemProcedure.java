package net.mcreator.thebackwoods.procedures;

// 1.21.1 neoforge
import net.mcreator.thebackwoods.TheBackwoodsMod;
import net.mcreator.thebackwoods.entity.RotEntity;
import net.mcreator.thebackwoods.init.TheBackwoodsModEntities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.fml.ModList;

@EventBusSubscriber
public class BackwoodsThreatSystemProcedure {

    private static final ResourceKey<Level> BACKWOODS_DIM =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("the_backwoods:backwoods"));

    private static final int ROT_NEARBY_RADIUS = 64;
    private static final int SPAWN_COOLDOWN_TICKS = 20 * 12;
    private static final int DECAY_INTERVAL_TICKS = 20;
    private static final int DECAY_PER_INTERVAL = 1;

    private static final int THREAT_WARN = 20;
    private static final int THREAT_SPAWN = 40;

    private static final int THREAT_KILL_WHILE_POWERED = 0; // disable vanilla melee mass-farm trigger
    private static final int THREAT_PROJECTILE_HIT = 3;
    private static final int THREAT_HIGH_DPS_HIT = 2;
    private static final int THREAT_KILLED_ROT = 8;

    private static final int WARN_COOLDOWN_TICKS = 20 * 30;
    private static final int SPAWN_DELAY_TICKS = 40;

    private static final String K_THREAT = "bw_threat";
    private static final String K_LAST_DECAY = "bw_last_decay";
    private static final String K_LAST_SPAWN = "bw_last_rot_spawn";
    private static final String K_LAST_WARN = "bw_last_warn";

    private static final String[] WARN_LINES = new String[] {
            "I feel your violence, trespasser.",
            "You kill too quickly.",
            "You enter frightened, then dare teach fear to everything else?",
            "You teach death too efficiently.",
            "You arrive wrapped in smoke and arrogance.",
            "I have already buried stronger things than you.",
            "I can feel your heartbeat through the floor.",
            "You survive because I have allowed it. Now I will not.",
            "I was killing long before your kind learned iron.",
            "You are already standing where you die.",
            "I know every direction you can run.",
            "You cannot frighten something older than fear.",
            "That mistake will be your final lesson.",
            "Thy end is now.",
            "Die.",
            "A visitor?",
            "This will hurt.",
            "Be gone.",
            "You thought yourself the apex predator here.",
            "Fall.",
            "You are strong. But I am beyond strength.",
            "I was grown to end things like you.",
            "You are a biped of glass and soft meat. Why are you here?",
            "Your people will experience nothing but death.",
            "I will teach your remains silence.",
            "You carry death loudly. I carry it patiently.",
            "Nothing violent survives here forever.",
            "You should have left while the I was still patient.",
            "You are not feared here.",
            "I have already decided what becomes of you.",
            "The woods survive everything. Including you.",
            "I am what this place sends when it has had enough.",
            "Are you unable to fight with your bare hands?",
            "The rot blooms brightest around arrogant people.",
            "I am the end of your people.",
            "You bring noise. I will bring the quiet.",
            "The woods have survived worse invasions.",
            "Your tools sound wrong here.",
            "Your violence echoes too far.",
            "You arrive carrying endings.",
            "The woods no longer mistake you for prey.",
            "You move like an infection.",
            "The silence you break is my own.",
            "You are a splinter in my skin.",
            "Steel belongs in the earth, not in the hand.",
            "You are becoming easier to find.",
            "I grow bold in your wake.",
            "Pruning begins."
    };

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) return;
        if (player.isCreative() || player.isSpectator()) {
            putInt(player, K_THREAT, 0); // Reset threat to 0 for creative/spectator
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) return;
        if (level.dimension() != BACKWOODS_DIM) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        long gameTime = level.getGameTime();

        decayThreat(player, gameTime);
        maybeWarn(player, serverLevel, gameTime);
        maybeSpawnRot(player, serverLevel, gameTime);
        scanPlayerLoadout(player, gameTime);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();

        // --- VANISH LOGIC ---
        if (dead instanceof Player player) {
            Level level = player.level();
            if (!level.isClientSide() && level instanceof ServerLevel s) {
                // Find any Rot in range of the player's death and clear them
                s.getEntitiesOfClass(RotEntity.class, player.getBoundingBox().inflate(ROT_NEARBY_RADIUS)).forEach(rot -> {
                    s.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, rot.getX(), rot.getY() + 1.0, rot.getZ(), 90, 0.45, 0.9, 0.45, 0.02);
                    TheBackwoodsMod.queueServerWork(60, () -> {
                        if (rot.isAlive()) {
                            rot.discard();
                        }
                    });
                });
            }
            return;
        }

        if (dead instanceof RotEntity) {
            Player killer = resolvePlayerAttacker(event.getSource(), dead);
            if (killer != null && killer.level().dimension() == BACKWOODS_DIM) {
                if (killer.isCreative() || killer.isSpectator()) return;
                long now = killer.level().getGameTime();
                putLong(killer, K_LAST_SPAWN, Math.max(0L, now - (SPAWN_COOLDOWN_TICKS - 40)));
                addThreat(killer, THREAT_KILLED_ROT);
            }
            return;
        }

        if (THREAT_KILL_WHILE_POWERED > 0) {
            Player attacker = resolvePlayerAttacker(event.getSource(), dead);
            if (attacker == null) return;

            Level level = attacker.level();
            if (level.isClientSide()) return;
            if (level.dimension() != BACKWOODS_DIM) return;

            if (isPowerPlayer(attacker)) {
                addThreat(attacker, THREAT_KILL_WHILE_POWERED);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim == null) return;

        // 1. Ignore Spore mod entities entirely
        ResourceLocation victimRl = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (victimRl != null && victimRl.getNamespace().equals("spore")) {
            return;
        }

        // 2. Resolve and validate player attacker
        Player attacker = resolvePlayerAttacker(event.getSource(), victim);
        if (attacker == null) return;
        if (attacker.isCreative() || attacker.isSpectator()) return;

        Level level = attacker.level();
        if (level.isClientSide()) return;
        if (level.dimension() != BACKWOODS_DIM) return;

        // 3. Only trigger threat when the victim is a woodbound entity (Splinter) in the Backwoods
        boolean isWoodbound = victim.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ResourceLocation.parse("the_backwoods:woodbound_entities")));
        if (!isWoodbound) return;

        Entity direct = event.getSource().getDirectEntity();

        // Exclude arrows from projectile threat
        if (direct instanceof AbstractArrow) return;

        boolean projectileLike =
                event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                        || direct instanceof Projectile
                        || (event.getSource().getEntity() != null
                        && direct != null
                        && event.getSource().getEntity() != direct);

        if (projectileLike) {
            // Detect if they actively hold/use a gun or a gun mod is loaded and they are wielding it
            boolean holdsGun = attacker.getPersistentData().getBoolean("bw_threat_holds_gun") 
                            || attacker.getPersistentData().getBoolean("bw_threat_gun_mod");
            
            if (holdsGun) {
                // High-threat trigger: actively shooting with a gun
                addThreat(attacker, THREAT_PROJECTILE_HIT);

                // If doing high DPS or rapid firing (e.g., hit is high damage)
                if (event.getNewDamage() >= 25.0F) {
                    addThreat(attacker, THREAT_HIGH_DPS_HIT);
                }
            } else {
                // If they don't hold a gun, standard projectile hits (e.g. snowballs, standard bow) don't trigger high threat
                // unless it is an extremely high DPS single hit (>= 25.0F)
                if (event.getNewDamage() >= 25.0F) {
                    addThreat(attacker, THREAT_PROJECTILE_HIT);
                }
            }
        } else {
            // Melee damage
            // Only trigger high threat if the player has op/modded combat gear and is dealing massive DPS
            double attackDmg = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double attackSpeed = attacker.getAttributeValue(Attributes.ATTACK_SPEED);
            boolean highDpsMelee = attackDmg > 20.0 || (attackDmg * attackSpeed) > 30.0;
            
            if (highDpsMelee && event.getNewDamage() >= 25.0F) {
                addThreat(attacker, THREAT_HIGH_DPS_HIT);
            }
        }
    }

    private static Player resolvePlayerAttacker(DamageSource source, LivingEntity victim) {
        if (source == null) return null;

        Entity attacker = source.getEntity();
        if (attacker instanceof Player p) return p;

        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile proj && proj.getOwner() instanceof Player pOwner) return pOwner;

        LivingEntity lastMob = victim.getLastHurtByMob();
        if (lastMob instanceof Player lastPlayer) return lastPlayer;

        return null;
    }

    
    private static final int SCAN_INTERVAL_TICKS = 40;

    private static void scanPlayerLoadout(Player player, long gameTime) {
        if (player.isCreative() || player.isSpectator()) return;
        long lastScan = getLong(player, "bw_last_scan", 0L);
        if (gameTime - lastScan < SCAN_INTERVAL_TICKS) return;
        putLong(player, "bw_last_scan", gameTime);

        // 1. Check Armor & Toughness
        boolean highArmor = false;
        double armorVal = player.getAttributeValue(Attributes.ARMOR);
        double toughnessVal = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        
        // Treat over 25 armor or 15+ toughness as high (above standard vanilla tier)
        if (armorVal > 25.0 || toughnessVal >= 15.0) {
            highArmor = true;
        }

        // 2. Check Melee Damage & Speed of currently held item
        boolean highDps = false;
        double attackDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        // Standard Netherite Sword is ~8 damage, 1.6 speed. (12.8 dps).
        // If damage > 20, or dps > 30, consider it extreme melee damage
        if (attackDmg > 20.0 || (attackDmg * attackSpeed) > 30.0) {
            highDps = true;
        }

        // 3. Scan equipped items for guns
        boolean holdsGun = false;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (isGunItem(main) || isGunItem(off)) {
            holdsGun = true;
        }

        // 4. Check for powerful combat or gun mods loaded in the instance
        boolean combatMod = false;
        if (ModList.get().isLoaded("epicfight") || ModList.get().isLoaded("bettercombat")) {
            combatMod = true;
        }

        boolean hasGunMod = ModList.get().isLoaded("tacz") 
                         || ModList.get().isLoaded("pointblank") 
                         || ModList.get().isLoaded("cgm")
                         || ModList.get().isLoaded("vicmwc");

        double maxHp = player.getAttributeValue(Attributes.MAX_HEALTH);
        boolean opStats = maxHp > 40.0 || (attackDmg * attackSpeed) > 100.0;

        // Record persistent flags on the player for the Rot AI to read
        player.getPersistentData().putBoolean("bw_threat_high_armor", highArmor);
        player.getPersistentData().putBoolean("bw_threat_high_dps", highDps);
        player.getPersistentData().putBoolean("bw_threat_holds_gun", holdsGun);
        player.getPersistentData().putBoolean("bw_threat_combat_mod", combatMod);
        player.getPersistentData().putBoolean("bw_threat_gun_mod", hasGunMod);
    }

    private static boolean isGunItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (rl == null) return false;
        String path = rl.getPath().toLowerCase(java.util.Locale.ROOT);
        String namespace = rl.getNamespace().toLowerCase(java.util.Locale.ROOT);
        
        if (namespace.equals("tacz") || namespace.equals("pointblank") || namespace.equals("cgm") || namespace.equals("mr_crayfish") || namespace.equals("vicmwc")) {
            return true;
        }
        
        return path.contains("gun") || path.contains("rifle") || path.contains("pistol") 
            || path.contains("revolver") || path.contains("shotgun") || path.contains("sniper") 
            || path.contains("carbine") || path.contains("blaster") || path.contains("musket") || path.contains("cannon");
    }

    private static boolean isPowerPlayer(Player p) {
        if (p == null) return false;

        int armor = p.getArmorValue();
        boolean superHighArmor = armor > 25;
        boolean flying = p.isFallFlying();
        return superHighArmor || flying;
    }

    private static boolean hasItem(Player p, net.minecraft.world.item.Item item) {
        for (ItemStack s : p.getInventory().items) if (!s.isEmpty() && s.getItem() == item) return true;
        for (ItemStack s : p.getInventory().armor) if (!s.isEmpty() && s.getItem() == item) return true;
        for (ItemStack s : p.getInventory().offhand) if (!s.isEmpty() && s.getItem() == item) return true;
        return false;
    }

    private static void addThreat(Player player, int amount) {
        int current = getInt(player, K_THREAT, 0);
        int next = Mth.clamp(current + amount, 0, 200);
        putInt(player, K_THREAT, next);
    }

    private static void decayThreat(Player player, long gameTime) {
        long last = getLong(player, K_LAST_DECAY, 0L);
        if (gameTime - last < DECAY_INTERVAL_TICKS) return;

        putLong(player, K_LAST_DECAY, gameTime);

        int current = getInt(player, K_THREAT, 0);
        int next = Math.max(0, current - DECAY_PER_INTERVAL);
        putInt(player, K_THREAT, next);

        if (next == 0) {
            player.getPersistentData().putBoolean("bw_threat_warn_played", false);
        }
    }

    private static void maybeWarn(Player player, ServerLevel level, long gameTime) {
        int threat = getInt(player, K_THREAT, 0);
        if (threat < THREAT_WARN) return;

        if (!player.getPersistentData().getBoolean("bw_threat_warn_played")) {
            player.getPersistentData().putBoolean("bw_threat_warn_played", true);

            String line = WARN_LINES[level.random.nextInt(WARN_LINES.length)];
            player.displayClientMessage(Component.literal(line), true);

            SoundEvent bell = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.bell.resonate"));
            if (bell != null) {
                level.playSound(
                        null,
                        BlockPos.containing(player.getX(), player.getY(), player.getZ()),
                        bell,
                        SoundSource.HOSTILE,
                        1.35f,
                        0.65f
                );
            }
        }
    }

    private static void maybeSpawnRot(Player player, ServerLevel level, long gameTime) {
        int threat = getInt(player, K_THREAT, 0);
        if (threat < THREAT_SPAWN) return;

        long lastSpawn = getLong(player, K_LAST_SPAWN, 0L);
        if (gameTime - lastSpawn < SPAWN_COOLDOWN_TICKS) return;

        if (isRotActiveForPlayer(level, player)) return;
        if (level.random.nextFloat() > 0.22f) return;

        putLong(player, K_LAST_SPAWN, gameTime);

        TheBackwoodsMod.queueServerWork(SPAWN_DELAY_TICKS, () -> {
            if (isRotActiveForPlayer(level, player)) return;
            int sx = Mth.floor(player.getX() + Mth.nextDouble(RandomSource.create(), -10, 10));
            int sz = Mth.floor(player.getZ() + Mth.nextDouble(RandomSource.create(), -10, 10));
            int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);

            Entity spawned = TheBackwoodsModEntities.ROT.get().spawn(
                    level,
                    BlockPos.containing(sx, sy, sz),
                    MobSpawnType.MOB_SUMMONED
            );
            if (spawned instanceof RotEntity rot) {
                rot.getPersistentData().putBoolean("sentinel_should_scan", true);
                rot.setDeltaMovement(0, 0, 0);
                player.getPersistentData().putString("bw_active_rot_uuid", rot.getUUID().toString());
            }
        });
    }

    private static boolean isRotActiveForPlayer(ServerLevel level, Player player) {
        if (player.getPersistentData().contains("bw_active_rot_uuid")) {
            try {
                java.util.UUID rotUuid = java.util.UUID.fromString(player.getPersistentData().getString("bw_active_rot_uuid"));
                Entity activeRot = level.getEntity(rotUuid);
                if (activeRot instanceof RotEntity && activeRot.isAlive()) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore parsing or retrieval errors
            }
        }
        return false;
    }

    private static boolean isRotNearby(LevelAccessor world, double x, double y, double z, double radius) {
        double searchRadius = 160.0; // Optimized tracking/simulation distance to prevent chunk search lookup lag
        return !world.getEntitiesOfClass(
                RotEntity.class,
                new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3(x, y, z)).inflate(searchRadius),
                e -> true
        ).isEmpty();
    }

    private static int getInt(Player p, String key, int def) {
        return p.getPersistentData().contains(key) ? p.getPersistentData().getInt(key) : def;
    }

    private static long getLong(Player p, String key, long def) {
        return p.getPersistentData().contains(key) ? p.getPersistentData().getLong(key) : def;
    }

    private static void putInt(Player p, String key, int value) {
        p.getPersistentData().putInt(key, value);
    }

    private static void putLong(Player p, String key, long value) {
        p.getPersistentData().putLong(key, value);
    }
} // 1.21.1