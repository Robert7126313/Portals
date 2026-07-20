package com.portalbrews.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.portalbrews.PortalFrameEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the standing portal frame as a double-sided, emissive, animated quad
 * and floats the encoded destination above it as a name label.
 *
 * The swirl texture is a 16x512 vertical strip of 32 frames; we advance the
 * frame ourselves (V offset) rather than relying on atlas .mcmeta animation,
 * because this texture is bound directly as an entity texture, not stitched.
 */
public class PortalFrameRenderer extends EntityRenderer<PortalFrameEntity, PortalFrameRenderState> {
	private static final Identifier TEXTURE =
		Identifier.fromNamespaceAndPath("portalbrews", "textures/item/portal_swirl.png");
	private static final int FRAMES = 32;
	private static final float WIDTH = 2.0f;
	private static final float HEIGHT = 3.0f;
	private static final int FULL_BRIGHT = 0x00F000F0;
	private static final int TEMPORARY_TINT = 0xFFD100FF; // magenta portal
	private static final int PERMANENT_TINT = 0xFFFFD700; // gold

	public PortalFrameRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public PortalFrameRenderState createRenderState() {
		return new PortalFrameRenderState();
	}

	@Override
	public void extractRenderState(PortalFrameEntity entity, PortalFrameRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yaw = entity.getFacingYaw();
		state.permanent = entity.isPermanent();
		state.nameTag = buildLabel(entity);
		state.nameTagAttachment = new Vec3(0.0, HEIGHT + 0.4, 0.0);
	}

	@Override
	public void submit(PortalFrameRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, pose, collector, camera); // name label (+ shadow, if any)

		int frame = (int) (state.ageInTicks * 0.6f) % FRAMES;
		float v0 = (float) frame / FRAMES;
		float v1 = (float) (frame + 1) / FRAMES;
		int tint = state.permanent ? PERMANENT_TINT : TEMPORARY_TINT;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
		RenderType renderType = RenderTypes.entityTranslucentEmissive(TEXTURE);
		collector.submitCustomGeometry(pose, renderType, (p, vc) -> quad(p, vc, tint, v0, v1));
		pose.popPose();
	}

	private static Component buildLabel(PortalFrameEntity entity) {
		if (!entity.hasDestination()) {
			return Component.literal("Portal Frame").withStyle(ChatFormatting.LIGHT_PURPLE);
		}
		BlockPos p = entity.getDestinationPos();
		String dim = entity.getDestinationDimensionId();
		String shortDim = dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim;
		return Component.literal("→ " + p.getX() + ", " + p.getY() + ", " + p.getZ() + " ")
			.withStyle(ChatFormatting.AQUA)
			.append(Component.literal("(" + shortDim + ")").withStyle(ChatFormatting.DARK_PURPLE));
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vc, int argb, float v0, float v1) {
		float hw = WIDTH / 2.0f;
		float y0 = 0.0f;
		float y1 = HEIGHT;
		int a = (argb >>> 24) & 0xFF;
		int r = (argb >>> 16) & 0xFF;
		int g = (argb >>> 8) & 0xFF;
		int b = argb & 0xFF;

		// Front face (normal +Z)
		vertex(pose, vc, -hw, y0, 0.0f, 0.0f, v1, r, g, b, a, 0.0f, 0.0f, 1.0f);
		vertex(pose, vc, hw, y0, 0.0f, 1.0f, v1, r, g, b, a, 0.0f, 0.0f, 1.0f);
		vertex(pose, vc, hw, y1, 0.0f, 1.0f, v0, r, g, b, a, 0.0f, 0.0f, 1.0f);
		vertex(pose, vc, -hw, y1, 0.0f, 0.0f, v0, r, g, b, a, 0.0f, 0.0f, 1.0f);

		// Back face (normal -Z), reversed winding
		vertex(pose, vc, -hw, y0, 0.0f, 0.0f, v1, r, g, b, a, 0.0f, 0.0f, -1.0f);
		vertex(pose, vc, -hw, y1, 0.0f, 0.0f, v0, r, g, b, a, 0.0f, 0.0f, -1.0f);
		vertex(pose, vc, hw, y1, 0.0f, 1.0f, v0, r, g, b, a, 0.0f, 0.0f, -1.0f);
		vertex(pose, vc, hw, y0, 0.0f, 1.0f, v1, r, g, b, a, 0.0f, 0.0f, -1.0f);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer vc,
			float x, float y, float z, float u, float v,
			int r, int g, int b, int a, float nx, float ny, float nz) {
		vc.addVertex(pose, x, y, z)
			.setColor(r, g, b, a)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(FULL_BRIGHT)
			.setNormal(pose, nx, ny, nz);
	}
}
