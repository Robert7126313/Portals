package com.portalbrews.mixin;

import com.portalbrews.PortalBrewsRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's {@code canPlaceItem} hardcodes the bottle slots (0-2) to accept only
 * POTION / SPLASH_POTION / LINGERING_POTION / GLASS_BOTTLE, so our custom potion
 * items cannot be dropped back into a brewing stand to continue the chain. Allow
 * them in the bottle slots (matching vanilla's "slot must be empty" rule).
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
	@Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
	private void portalbrews$allowCustomPotions(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (slot < 0 || slot > 2) {
			return;
		}
		if (stack.is(PortalBrewsRegistry.PORTAL_POTION_ITEM)
			|| stack.is(PortalBrewsRegistry.PORTAL_FRAME_POTION_ITEM)
			|| stack.is(PortalBrewsRegistry.PERMANENT_PORTAL_FRAME_POTION_ITEM)) {
			ItemStack current = ((Container) this).getItem(slot);
			cir.setReturnValue(current.isEmpty());
		}
	}
}
