package com.portalbrews.mixin;

import com.portalbrews.PortalBrews;
import com.portalbrews.PortalBrewsRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Give every lodestone-linked compass a unique id.
 *
 * We assign it lazily in inventoryTick rather than on the useOn binding, because
 * when a compass is linked in creative (or from a stack larger than one), the
 * game moves the lodestone tracker onto a *new* stack, not the one held during
 * useOn - so hooking useOn would miss it. Tagging any tracked-but-unlabeled
 * compass as it ticks in an inventory catches all of them before they're brewed.
 */
@Mixin(CompassItem.class)
public class CompassItemMixin {
	@Inject(method = "inventoryTick", at = @At("HEAD"))
	private void portalbrews$assignLodestoneId(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot, CallbackInfo ci) {
		if (stack.get(DataComponents.LODESTONE_TRACKER) == null) {
			return;
		}
		if (stack.get(PortalBrewsRegistry.LODESTONE_ID) != null) {
			return;
		}
		UUID id = UUID.randomUUID();
		stack.set(PortalBrewsRegistry.LODESTONE_ID, id);
		PortalBrews.LOG.info("[portalbrews] assigned lodestone_id {} to a linked compass", id);
	}
}
