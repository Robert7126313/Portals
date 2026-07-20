package com.portalbrews;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
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
	private static final EntityDataAccessor<String> DATA_DEST_DIM =
		SynchedEntityData.defineId(PortalFrameEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<BlockPos> DATA_DEST_POS =
		SynchedEntityData.defineId(PortalFrameEntity.class, EntityDataSerializers.BLOCK_POS);

	public PortalFrameEntity(EntityType<? extends PortalFrameEntity> type, Level level) {
		super(type, level);
	}

	public void configure(UUID lodestoneId, boolean permanent, float facingYaw, GlobalPos destination) {
		entityData.set(DATA_LODESTONE_ID, lodestoneId == null ? "" : lodestoneId.toString());
		entityData.set(DATA_PERMANENT, permanent);
		entityData.set(DATA_YAW, facingYaw);
		entityData.set(DATA_TTL, permanent ? Integer.MAX_VALUE : DEFAULT_TTL);
		if (destination != null) {
			entityData.set(DATA_DEST_DIM, destination.dimension().identifier().toString());
			entityData.set(DATA_DEST_POS, destination.pos());
		} else {
			entityData.set(DATA_DEST_DIM, "");
			entityData.set(DATA_DEST_POS, BlockPos.ZERO);
		}
	}

	public UUID getLodestoneId() {
		String s = entityData.get(DATA_LODESTONE_ID);
		if (s == null || s.isEmpty()) return null;
		try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
	}

	public boolean isPermanent() { return entityData.get(DATA_PERMANENT); }
	public int getRemainingTicks() { return entityData.get(DATA_TTL); }
	public float getFacingYaw() { return entityData.get(DATA_YAW); }

	public boolean hasDestination() { return !entityData.get(DATA_DEST_DIM).isEmpty(); }
	public String getDestinationDimensionId() { return entityData.get(DATA_DEST_DIM); }
	public BlockPos getDestinationPos() { return entityData.get(DATA_DEST_POS); }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		b.define(DATA_TTL, DEFAULT_TTL);
		b.define(DATA_PERMANENT, false);
		b.define(DATA_LODESTONE_ID, "");
		b.define(DATA_YAW, 0.0f);
		b.define(DATA_DEST_DIM, "");
		b.define(DATA_DEST_POS, BlockPos.ZERO);
	}

	/** How far, in blocks, an entity may be from the frame to be pulled through. */
	private static final double PORTAL_HALF_WIDTH = 1.3;
	private static final double PORTAL_HEIGHT = 3.0;
	private static final double PORTAL_CENTER_Y = 1.5;
	private static final double PORTAL_RADIUS = 1.5;
	/** Ticks of immunity after a trip so entities don't bounce between paired frames. */
	private static final int TELEPORT_COOLDOWN = 40;
	/** Ticks before expiry at which a temporary frame starts visibly destabilizing. */
	private static final int BREAK_WARN_TICKS = 100;
	/** Dr. Strange orange spark, and the red spark of a frame that is about to collapse. */
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xFF7A18, 1.3f);
	private static final DustParticleOptions BREAK_SPARK = new DustParticleOptions(0xFF2410, 1.5f);

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel serverLevel)) return;

		if (!isPermanent()) {
			int t = entityData.get(DATA_TTL);
			if (t <= 0) {
				collapse(serverLevel);
				discard();
				return;
			}
			entityData.set(DATA_TTL, t - 1);
		}

		emitSparks(serverLevel);
		teleportEntitiesInPortal(serverLevel);
	}

	private boolean isBreakingSoon() {
		return !isPermanent() && entityData.get(DATA_TTL) <= BREAK_WARN_TICKS;
	}

	/** True if this frame leads to the lodestone at {@code pos} in dimension {@code dimId}. */
	public boolean pointsAtLodestone(String dimId, BlockPos pos) {
		return hasDestination() && getDestinationDimensionId().equals(dimId) && getDestinationPos().equals(pos);
	}

	/**
	 * Collapse every frame (in any loaded dimension) whose destination is the lodestone
	 * that was just broken. A frame's destination block <em>is</em> its source lodestone,
	 * so this both honours "destroy the lodestone to remove a permanent portal" and cleans
	 * up temporary frames whose target no longer exists.
	 */
	public static void onLodestoneBroken(MinecraftServer server, String lodestoneDimId, BlockPos pos) {
		int collapsed = 0;
		for (ServerLevel level : server.getAllLevels()) {
			List<? extends PortalFrameEntity> frames = level.getEntities(
				EntityTypeTest.forClass(PortalFrameEntity.class),
				frame -> frame.pointsAtLodestone(lodestoneDimId, pos)
			);
			for (PortalFrameEntity frame : frames) {
				frame.collapse(level);
				frame.discard();
				collapsed++;
			}
		}
		if (collapsed > 0) {
			PortalBrews.LOG.info("[portalbrews] lodestone broken at {} {}; collapsed {} frame(s)", lodestoneDimId, pos, collapsed);
		}
	}

	/** A short burst of sparks + collapse sound when the frame is torn down. */
	private void collapse(ServerLevel level) {
		double cx = getX();
		double cy = getY() + PORTAL_CENTER_Y;
		double cz = getZ();
		level.sendParticles(ParticleTypes.PORTAL, cx, cy, cz, 60, PORTAL_RADIUS * 0.6, PORTAL_RADIUS * 0.6, 0.3, 0.5);
		level.sendParticles(BREAK_SPARK, cx, cy, cz, 45, PORTAL_RADIUS * 0.6, PORTAL_RADIUS * 0.6, 0.3, 0.2);
		level.playSound(null, cx, cy, cz, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.7f, 0.5f);
	}

	/** Spawns orange sparks orbiting the rim, tangent to the circle, for the Dr. Strange look. */
	private void emitSparks(ServerLevel level) {
		float yawRad = (float) Math.toRadians(getFacingYaw());
		double ux = Math.cos(yawRad); // in-plane horizontal axis in world space
		double uz = Math.sin(yawRad);
		double cx = getX();
		double cy = getY() + PORTAL_CENTER_Y;
		double cz = getZ();
		double spin = level.getGameTime() * 0.18;

		// As the frame nears expiry it sparks harder, redder, and more erratically.
		boolean breaking = isBreakingSoon();
		int sparks = breaking ? 7 : 3;
		DustParticleOptions spark = breaking ? BREAK_SPARK : SPARK;
		double speed = breaking ? 0.22 : 0.12;
		double scatter = breaking ? 0.5 : 0.15;
		for (int i = 0; i < sparks; i++) {
			double angle = spin + i * (Math.PI * 2.0 / sparks) + random.nextGaussian() * scatter;
			double rimX = Math.cos(angle) * PORTAL_RADIUS;
			double rimY = Math.sin(angle) * PORTAL_RADIUS;
			double px = cx + rimX * ux;
			double py = cy + rimY;
			double pz = cz + rimX * uz;

			double tangentInPlane = -Math.sin(angle);
			double vx = tangentInPlane * ux * speed;
			double vy = Math.cos(angle) * speed;
			double vz = tangentInPlane * uz * speed;
			level.sendParticles(spark, px, py, pz, 0, vx, vy, vz, 1.0);
		}
	}

	private void teleportEntitiesInPortal(ServerLevel level) {
		if (!hasDestination()) return;
		ServerLevel destination = level.getServer().getLevel(destinationLevelKey());
		if (destination == null) return;
		Vec3 dest = Vec3.atCenterOf(getDestinationPos()).add(0.0, 1.0, 0.0);

		Vec3 c = position();
		AABB portal = new AABB(
			c.x - PORTAL_HALF_WIDTH, c.y, c.z - PORTAL_HALF_WIDTH,
			c.x + PORTAL_HALF_WIDTH, c.y + PORTAL_HEIGHT, c.z + PORTAL_HALF_WIDTH
		);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, portal)) {
			if (entity.isOnPortalCooldown()) continue;
			teleport(entity, destination, dest);
			entity.setPortalCooldown(TELEPORT_COOLDOWN);
		}
	}

	private ResourceKey<Level> destinationLevelKey() {
		return ResourceKey.create(Registries.DIMENSION, Identifier.parse(getDestinationDimensionId()));
	}

	private static void teleport(LivingEntity entity, ServerLevel destination, Vec3 dest) {
		if (entity.level() == destination) {
			entity.teleportTo(destination, dest.x, dest.y, dest.z, EnumSet.noneOf(Relative.class), entity.getYRot(), entity.getXRot(), true);
		} else {
			TeleportTransition t = new TeleportTransition(
				destination, dest, Vec3.ZERO, entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING
			);
			entity.teleport(t);
		}
		destination.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y, dest.z, 30, 0.4, 0.6, 0.4, 0.3);
		destination.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.0f);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		out.putInt("TTL", entityData.get(DATA_TTL));
		out.putBoolean("Permanent", isPermanent());
		out.putString("LodestoneId", entityData.get(DATA_LODESTONE_ID));
		out.putFloat("FacingYaw", getFacingYaw());
		out.putString("DestDim", entityData.get(DATA_DEST_DIM));
		BlockPos p = entityData.get(DATA_DEST_POS);
		out.putInt("DestX", p.getX());
		out.putInt("DestY", p.getY());
		out.putInt("DestZ", p.getZ());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		entityData.set(DATA_TTL, in.getIntOr("TTL", DEFAULT_TTL));
		entityData.set(DATA_PERMANENT, in.getBooleanOr("Permanent", false));
		entityData.set(DATA_LODESTONE_ID, in.getStringOr("LodestoneId", ""));
		entityData.set(DATA_YAW, in.getFloatOr("FacingYaw", 0.0f));
		entityData.set(DATA_DEST_DIM, in.getStringOr("DestDim", ""));
		entityData.set(DATA_DEST_POS, new BlockPos(
			in.getIntOr("DestX", 0),
			in.getIntOr("DestY", 0),
			in.getIntOr("DestZ", 0)
		));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Immune to the environment, but a player may deliberately tear a frame down.
		if (source.getEntity() instanceof Player) {
			collapse(level);
			discard();
			return true;
		}
		return false;
	}
}