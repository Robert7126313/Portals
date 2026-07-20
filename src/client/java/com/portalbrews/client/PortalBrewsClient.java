package com.portalbrews.client;

import com.portalbrews.PortalBrewsRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class PortalBrewsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(
			PortalBrewsRegistry.PORTAL_POTION_ENTITY,
			ctx -> new ThrownItemRenderer<>(ctx, 1.0f, false)
		);
		EntityRendererRegistry.register(
			PortalBrewsRegistry.PORTAL_FRAME_ENTITY,
			PortalFrameRenderer::new
		);
	}
}