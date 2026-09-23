package net.mcreator.thebackwoods.procedures;

import net.mcreator.thebackwoods.entity.LignumGigasEntity;
import net.minecraft.world.entity.Entity;
// 1.21.1 neoforge
public class LignumGigasEntityIsHurtProcedure {
	private static final int REQUIRED_HITS = 12;
	private static final int AURA_DURATION_TICKS = 20 * 60;

	public static void execute(Entity entity) {
		if (!(entity instanceof LignumGigasEntity gigas)) return;
		if (gigas.level().isClientSide()) return;
		if (gigas.getEntityData().get(LignumGigasEntity.DATA_seq_active)) return;

		int hits = gigas.getEntityData().get(LignumGigasEntity.DATA_hit_count) + 1;
		gigas.getEntityData().set(LignumGigasEntity.DATA_hit_count, hits);

		if (hits >= REQUIRED_HITS) {
			gigas.getEntityData().set(LignumGigasEntity.DATA_hit_count, 0);
			gigas.getEntityData().set(LignumGigasEntity.DATA_seq_active, true);
			gigas.getEntityData().set(LignumGigasEntity.DATA_seq_t, 0);
			gigas.getEntityData().set(LignumGigasEntity.DATA_effect_done, false);
			gigas.getEntityData().set(LignumGigasEntity.DATA_aura_time_left, AURA_DURATION_TICKS);

			// start raise anim, stop lower anim
			gigas.getEntityData().set(LignumGigasEntity.DATA_play_attack_anim, true);
			gigas.getEntityData().set(LignumGigasEntity.DATA_play_lower_anim, false);
		}
	}
}