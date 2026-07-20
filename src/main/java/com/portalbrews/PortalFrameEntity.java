package com.portalbrews;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class PortalFrameEntity extends Entity {
	public static final int DEFAULT_TTL = 30 * 20;

	private static final EntityDataAccessor<Integer> DATA_TTL =
		SynchedEntityData.defineId(PortalFrameEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_PERMANENT =
		SynchedEntityData.defineId(PortalFrameEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> DATA_LODESTONE_ID =
		SynchedEntityData.defineId(PortalFrameEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> DATA_YAW =
		SynchedEntityData.defineId(PortalFrameEntity.class, EntityDataSerializers.FLOAT);

	public PortalFrameEntity(EntityType<? extends PortalFrameEntity> type, Level level) {
		super(type, level);
	}

	public void configure(UUID lodestoneId, boolean permanent, float facingYaw) {
		entityData.set(DATA_LODESTONE_ID, lodestoneId == null ? "" : lodestoneId.toString());
		entityData.set(DATA_PERMANENT, permanent);
		entityData.set(DATA_YAW, facingYaw);
		entityData.set(DATA_TTL, permanent ? Integer.MAX_VALUE : DEFAULT_TTL);
	}

	public UUID getLodestoneId() {
		String s = entityData.get(DATA_LODESTONE_ID);
		if (s == null || s.isEmpty()) return null;
		try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
	}

	public boolean isPermanent() { return entityData.get(DATA_PERMANENT); }
	public int getRemainingTicks() { return entityData.get(DATA_TTL); }
	public float getFacingYaw() { return entityData.get(DATA_YAW); }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		b.define(DATA_TTL, DEFAULT_TTL);
		b.define(DATA_PERMANENT, false);
		b.define(DATA_LODESTONE_ID, "");
		b.define(DATA_YAW, 0.0f);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		if (isPermanent()) return;
		int t = entityData.get(DATA_TTL);
		if (t <= 0) {
			discard();
			return;
		}
		entityData.set(DATA_TTL, t - 1);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		out.putInt("TTL", entityData.get(DATA_TTL));
		out.putBoolean("Permanent", isPermanent());
		out.putString("LodestoneId", entityData.get(DATA_LODESTONE_ID));
		out.putFloat("FacingYaw", getFacingYaw());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		entityData.set(DATA_TTL, in.getIntOr("TTL", DEFAULT_TTL));
		entityData.set(DATA_PERMANENT, in.getBooleanOr("Permanent", false));
		entityData.set(DATA_LODESTONE_ID, in.getStringOr("LodestoneId", ""));
		entityData.set(DATA_YAW, in.getFloatOr("FacingYaw", 0.0f));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}
}