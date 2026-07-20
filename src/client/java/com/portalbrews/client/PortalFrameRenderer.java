package com.portalbrews.client;

import com.portalbrews.PortalFrameEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class PortalFrameRenderer extends EntityRenderer<PortalFrameEntity, EntityRenderState> {
	public PortalFrameRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}