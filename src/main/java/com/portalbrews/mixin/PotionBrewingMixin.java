package com.portalbrews.mixin;

import com.portalbrews.PortalBrews;
import com.portalbrews.PortalBrewsRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.LodestoneTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {
	@Inject(method = "mix", at = @At("RETURN"), cancellable = true)
	private void portalbrews$copyLodestoneTracker(ItemStack ingredient, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		int color;
		if (result.is(PortalBrewsRegistry.PORTAL_POTION_ITEM)) {
			color = PortalBrewsRegistry.PORTAL_POTION_COLOR;
			LodestoneTracker tracker = ingredient.get(DataComponents.LODESTONE_TRACKER);
			if (tracker != null) {
				result.set(DataComponents.LODESTONE_TRACKER, tracker);
			}
			java.util.UUID id = ingredient.get(PortalBrewsRegistry.LODESTONE_ID);
			if (id != null) {
				result.set(PortalBrewsRegistry.LODESTONE_ID, id);
			}
		} else if (result.is(PortalBrewsRegistry.PORTAL_FRAME_POTION_ITEM)) {
			color = PortalBrewsRegistry.PORTAL_FRAME_POTION_COLOR;
			LodestoneTracker tracker = input.get(DataComponents.LODESTONE_TRACKER);
			if (tracker != null) result.set(DataComponents.LODESTONE_TRACKER, tracker);
			java.util.UUID id = input.get(PortalBrewsRegistry.LODESTONE_ID);
			if (id != null) result.set(PortalBrewsRegistry.LODESTONE_ID, id);
		} else if (result.is(PortalBrewsRegistry.PERMANENT_PORTAL_FRAME_POTION_ITEM)) {
			color = PortalBrewsRegistry.PERMANENT_PORTAL_FRAME_POTION_COLOR;
			LodestoneTracker tracker = input.get(DataComponents.LODESTONE_TRACKER);
			if (tracker != null) result.set(DataComponents.LODESTONE_TRACKER, tracker);
			java.util.UUID id = input.get(PortalBrewsRegistry.LODESTONE_ID);
			if (id != null) result.set(PortalBrewsRegistry.LODESTONE_ID, id);
		} else {
			return;
		}
		PotionContents old = result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		PotionContents recolored = new PotionContents(old.potion(), Optional.of(color), old.customEffects(), Optional.empty());
		result.set(DataComponents.POTION_CONTENTS, recolored);
		cir.setReturnValue(result);
	}
}