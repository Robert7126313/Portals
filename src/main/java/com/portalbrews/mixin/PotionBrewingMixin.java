package com.portalbrews.mixin;

import com.portalbrews.PortalBrews;
import com.portalbrews.PortalBrewsRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.LodestoneTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {
	@Inject(method = "mix", at = @At("RETURN"), cancellable = true)
	private void portalbrews$copyLodestoneTracker(ItemStack ingredient, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		PortalBrews.LOG.info("[portalbrews] mix ingredient={} input={} result={}", ingredient, input, result);
		if (!result.is(PortalBrewsRegistry.PORTAL_POTION_ITEM)) {
			return;
		}
		LodestoneTracker tracker = ingredient.get(DataComponents.LODESTONE_TRACKER);
		PortalBrews.LOG.info("[portalbrews] tracker on ingredient = {}", tracker);
		if (tracker == null) {
			return;
		}
		result.set(DataComponents.LODESTONE_TRACKER, tracker);
		cir.setReturnValue(result);
	}
}