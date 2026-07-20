package com.portalbrews;

import com.portalbrews.PortalBrews;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class PortalPotionEntity extends AbstractThrownPotion {
	public PortalPotionEntity(EntityType<? extends PortalPotionEntity> type, Level level) {
		super(type, level);
	}

	public PortalPotionEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(PortalBrewsRegistry.PORTAL_POTION_ENTITY, level, owner, stack);
		PortalBrews.LOG.info("[portalbrews] entity ctor(owner) side={} stack={} tracker={}", level.isClientSide(), stack, stack.get(DataComponents.LODESTONE_TRACKER));
	}

	public PortalPotionEntity(Level level, double x, double y, double z, ItemStack stack) {
		super(PortalBrewsRegistry.PORTAL_POTION_ENTITY, level, x, y, z, stack);
		PortalBrews.LOG.info("[portalbrews] entity ctor(pos) side={} stack={} tracker={}", level.isClientSide(), stack, stack.get(DataComponents.LODESTONE_TRACKER));
	}

	@Override
	protected Item getDefaultItem() {
		return PortalBrewsRegistry.PORTAL_POTION_ITEM;
	}

	@Override
	protected void onHit(HitResult hit) {
		if (level() instanceof ServerLevel serverLevel) {
			onHitAsPotion(serverLevel, getItem(), hit);
			serverLevel.levelEvent(2002, blockPosition(), 0x8A2BE2);
			discard();
		}
	}

	@Override
	protected void onHitAsPotion(ServerLevel level, ItemStack stack, HitResult hit) {
		LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
		PortalBrews.LOG.info("[portalbrews] potion splash stack={} tracker={}", stack, tracker);
		Optional<GlobalPos> target = tracker == null ? Optional.empty() : tracker.target();

		Vec3 impact = hit.getLocation();
		level.sendParticles(ParticleTypes.PORTAL, impact.x, impact.y + 0.5, impact.z, 40, 0.5, 0.5, 0.5, 0.3);
		level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1.0f);

		if (target.isEmpty()) {
			return;
		}

		GlobalPos gp = target.get();
		ServerLevel destination = level.getServer().getLevel(gp.dimension());
		if (destination == null) {
			PortalBrews.LOG.info("[portalbrews] destination dimension not found: {}", gp.dimension());
			return;
		}

		BlockPos p = gp.pos();
		Vec3 dest = Vec3.atCenterOf(p).add(0, 1, 0);

		AABB area = getBoundingBox().inflate(AbstractThrownPotion.SPLASH_RANGE);
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area);
		PortalBrews.LOG.info("[portalbrews] teleporting {} entities to {} in {}", victims.size(), dest, destination.dimension().identifier());
		for (LivingEntity victim : victims) {
			PortalBrews.LOG.info("[portalbrews]   -> {} (was at {})", victim, victim.position());
			teleportEntity(victim, destination, dest);
		}
	}

	private static void teleportEntity(LivingEntity entity, ServerLevel destination, Vec3 dest) {
		if (entity.level() == destination) {
			entity.teleportTo(destination, dest.x, dest.y, dest.z, EnumSet.noneOf(Relative.class), entity.getYRot(), entity.getXRot(), true);
		} else {
			TeleportTransition t = new TeleportTransition(
				destination,
				dest,
				Vec3.ZERO,
				entity.getYRot(),
				entity.getXRot(),
				TeleportTransition.DO_NOTHING
			);
			entity.teleport(t);
		}
		destination.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y, dest.z, 30, 0.4, 0.6, 0.4, 0.3);
		destination.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.0f);
	}
}