package com.portalbrews.mixin;

import com.portalbrews.PortalBrews;
import com.portalbrews.PortalBrewsRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(CompassItem.class)
public class CompassItemMixin {
	@Inject(method = "useOn", at = @At("RETURN"))
	private void portalbrews$attachLodestoneId(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack stack = ctx.getItemInHand();
		if (stack.get(DataComponents.LODESTONE_TRACKER) == null) {
			return;
		}
		if (stack.get(PortalBrewsRegistry.LODESTONE_ID) != null) {
			return;
		}
		UUID id = UUID.randomUUID();
		stack.set(PortalBrewsRegistry.LODESTONE_ID, id);
		PortalBrews.LOG.info("[portalbrews] attached lodestone_id {} on side={}", id, ctx.getLevel().isClientSide());
	}
}