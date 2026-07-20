package com.portalbrews.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class PortalFrameRenderState extends EntityRenderState {
	public float yaw;
	public boolean permanent;
	/** Destination dimension id (e.g. "minecraft:the_nether"), or "" if none. Drives the view tint. */
	public String destinationDimension = "";
	/** 0 = stable, ramping to 1 as a temporary frame nears expiry. Drives the "breaking soon" flicker. */
	public float instability;
}
