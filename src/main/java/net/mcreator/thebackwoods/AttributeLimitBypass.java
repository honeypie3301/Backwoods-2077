/*
 * The code of this mod element is always locked.
 *
 * You can register new events in this class too.
 *
 * If you want to make a plain independent class, create it using
 * Project Browser -> New... and make sure to make the class
 * outside net.mcreator.thebackwoods as this package is managed by MCreator.
 *
 * If you change workspace package, modid or prefix, you will need
 * to manually adapt this file to these changes or remake it.
 *
 * This class will be added in the mod root package.
*/
package net.mcreator.thebackwoods;

import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Holder;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"deprecation", "removal"})
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class AttributeLimitBypass {
	public static class BypassProfile {
		public final double maxHp;
		public final double armor;
		public final double toughness;
		public final double knockbackResistance;

		public BypassProfile(double maxHp, double armor, double toughness, double knockbackResistance) {
			this.maxHp = maxHp;
			this.armor = armor;
			this.toughness = toughness;
			this.knockbackResistance = knockbackResistance;
		}
	}

	private static final Map<String, BypassProfile> REGISTERED_PROFILES = new ConcurrentHashMap<>();

	static {
		registerProfile("verdant", new BypassProfile(2500.0D, 150.0D, 40.0D, 1.0D));
		registerProfile("verdantengine", new BypassProfile(2500.0D, 150.0D, 40.0D, 1.0D));
	}

	public AttributeLimitBypass() {
	}

	public static void registerProfile(String entityIdentifier, BypassProfile profile) {
		REGISTERED_PROFILES.put(entityIdentifier.toLowerCase(), profile);
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		event.enqueueWork(AttributeLimitBypass::unlockAllAttributes);
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
	}

	public static void unlockAllAttributes() {
		unlockRangedAttribute(Attributes.MAX_HEALTH, 10_000_000.0D);
		unlockRangedAttribute(Attributes.ARMOR, 100_000.0D);
		unlockRangedAttribute(Attributes.ARMOR_TOUGHNESS, 100_000.0D);
		unlockRangedAttribute(Attributes.ATTACK_DAMAGE, 100_000.0D);
		unlockRangedAttribute(Attributes.KNOCKBACK_RESISTANCE, 100.0D);
	}

	private static void unlockRangedAttribute(Holder<Attribute> holder, double newMax) {
		if (holder == null) return;
		try {
			Attribute attr = holder.value();
			if (attr instanceof RangedAttribute ranged) {
				double curMax = ranged.getMaxValue();
				for (Field field : RangedAttribute.class.getDeclaredFields()) {
					if (field.getType() == double.class) {
						field.setAccessible(true);
						double val = field.getDouble(ranged);
						// Only target the exact maxValue field to prevent modifying minValue or defaultValue
						if (Double.compare(val, curMax) == 0 && (field.getName().toLowerCase().contains("max") || field.getName().equals("f_22187_") || val > ranged.getMinValue())) {
							field.setDouble(ranged, newMax);
						}
					}
				}
			}
		} catch (Exception ignored) {}
	}

	public static void applyBypass(LivingEntity entity, double maxHp, double armor) {
		applyBypass(entity, maxHp, armor, 20.0D, 1.0D);
	}

	public static void applyBypass(LivingEntity entity, double maxHp, double armor, double toughness, double knockbackRes) {
		if (entity == null) return;
		unlockAllAttributes();

		if (maxHp > 0) {
			AttributeInstance hpAttr = entity.getAttribute(Attributes.MAX_HEALTH);
			if (hpAttr != null) {
				hpAttr.setBaseValue(maxHp);
				if (entity.getHealth() < (float) maxHp) {
					entity.setHealth((float) maxHp);
				}
			}
		}

		if (armor > 0) {
			AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
			if (armorAttr != null) {
				armorAttr.setBaseValue(armor);
			}
		}

		if (toughness > 0) {
			AttributeInstance toughAttr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
			if (toughAttr != null) {
				toughAttr.setBaseValue(toughness);
			}
		}

		if (knockbackRes > 0) {
			AttributeInstance kbAttr = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			if (kbAttr != null) {
				kbAttr.setBaseValue(knockbackRes);
			}
		}
	}

	@EventBusSubscriber
	public static class AttributeLimitBypassForgeBusEvents {
		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
			unlockAllAttributes();
		}

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public static void onEntityJoin(EntityJoinLevelEvent event) {
			Entity e = event.getEntity();
			if (e instanceof LivingEntity living && !e.level().isClientSide()) {
				CompoundTag nbt = living.getPersistentData();
				String descId = living.getType().getDescriptionId().toLowerCase();
				String className = living.getClass().getSimpleName().toLowerCase();

				for (Map.Entry<String, BypassProfile> entry : REGISTERED_PROFILES.entrySet()) {
					String key = entry.getKey();
					if (descId.contains(key) || className.contains(key)) {
						if (!nbt.getBoolean("bypass_attributes_configured")) {
							nbt.putBoolean("bypass_attributes_configured", true);
							BypassProfile p = entry.getValue();
							applyBypass(living, p.maxHp, p.armor, p.toughness, p.knockbackResistance);
						}
						break;
					}
				}
			}
		}
	}
 // 1.21.1
}