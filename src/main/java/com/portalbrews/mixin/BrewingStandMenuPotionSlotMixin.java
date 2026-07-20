package com.portalbrews.mixin;

import com.portalbrews.PortalBrewsRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The brewing-stand *GUI* gates its bottle slots through
 * {@code BrewingStandMenu$PotionSlot.mayPlaceItem}, which is separate from the
 * block entity's {@code canPlaceItem} (that one only governs hopper/automation
 * insertion). Without this, our custom potions can't be dropped back into the
 * stand by hand to continue the brew chain.
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public class BrewingStandMenuPotionSlotMixin {
	@Inject(method = "mayPlaceItem", at = @At("HEAD"), cancellable = true)
	private static void portalbrews$allowCustomPotions(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.is(PortalBrewsRegistry.PORTAL_POTION_ITEM)
			|| stack.is(PortalBrewsRegistry.PORTAL_FRAME_POTION_ITEM)
			|| stack.is(PortalBrewsRegistry.PERMANENT_PORTAL_FRAME_POTION_ITEM)) {
			cir.setReturnValue(true);
		}
	}
}
