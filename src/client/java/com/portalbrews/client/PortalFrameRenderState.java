package com.portalbrews.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class PortalFrameRenderState extends EntityRenderState {
	public float yaw;
	public boolean permanent;
	/** Destination dimension id (e.g. "minecraft:the_nether"), or "" if none. Drives the sky view. */
	public String destinationDimension = "";
	/** Destination time of day (0-24000), for the sun/moon and day/night sky. */
	public int destinationTime;
	/** 0 = stable, ramping to 1 as a temporary frame nears expiry. Drives the "breaking soon" flicker. */
	public float instability;
}
