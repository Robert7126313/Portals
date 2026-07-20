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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the portal frame as a circular, emissive, double-sided disc:
 *
 *  - the interior is a placeholder "view" of the destination, tinted by the
 *    destination dimension (overworld sky-blue, nether red, end purple). This is
 *    a stand-in until a real see-through portal view (e.g. an Immersive Portals
 *    API connection) can be wired in;
 *  - a solid orange/gold rim frames it, Dr. Strange style, complemented by the
 *    spark particles the entity orbits around the edge;
 *  - the destination coordinates float above it as a name label.
 *
 * A flat white texture is used so the vertex colors tint cleanly.
 */
public class PortalFrameRenderer extends EntityRenderer<PortalFrameEntity, PortalFrameRenderState> {
	private static final Identifier TEXTURE =
		Identifier.fromNamespaceAndPath("portalbrews", "textures/item/portal_view.png");
	private static final float RADIUS = 1.5f;
	private static final float RIM_INNER = 1.34f;
	private static final float CENTER_Y = 1.5f;
	private static final int SEGMENTS = 48;
	private static final int FULL_BRIGHT = 0x00F000F0;
	private static final int TEMPORARY_RIM = 0xFFFF6A10; // temporary: fiery orange
	private static final int PERMANENT_RIM = 0xFF33E6D8;  // permanent: steady aqua
	private static final int BREAK_COLOR = 0xFFFF2410;    // red-orange for a dying frame
	/** Ticks before expiry at which a temporary frame starts visibly destabilizing. */
	private static final int BREAK_WARN_TICKS = 100;

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
		state.destinationDimension = entity.hasDestination() ? entity.getDestinationDimensionId() : "";
		state.instability = entity.isPermanent()
			? 0.0f
			: 1.0f - Mth.clamp(entity.getRemainingTicks() / (float) BREAK_WARN_TICKS, 0.0f, 1.0f);
		state.nameTag = buildLabel(entity);
		state.nameTagAttachment = new Vec3(0.0, CENTER_Y + RADIUS + 0.4, 0.0);
	}

	@Override
	public void submit(PortalFrameRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, pose, collector, camera); // name label (+ shadow, if any)

		int[] view = dimensionColors(state.destinationDimension);
		int baseRim = state.permanent ? PERMANENT_RIM : TEMPORARY_RIM;
		int rim = state.instability > 0.0f
			? lerpColor(baseRim, BREAK_COLOR, state.instability * 0.85f)
			: baseRim;
		float alphaMul = flicker(state.instability, state.ageInTicks);

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
		RenderType renderType = RenderTypes.entityTranslucentEmissive(TEXTURE);
		collector.submitCustomGeometry(pose, renderType, (p, vc) -> {
			windowDisc(p, vc, view[0], view[1], alphaMul);
			rimRing(p, vc, rim, alphaMul);
		});
		pose.popPose();
	}

	/** As instability rises, the frame dims and stutters erratically — a "breaking soon" tell. */
	private static float flicker(float instability, float ageInTicks) {
		if (instability <= 0.0f) {
			return 1.0f;
		}
		float wobble = (float) (Math.sin(ageInTicks * 1.7) * Math.sin(ageInTicks * 0.9)); // -1..1, erratic
		float drop = instability * (0.4f + 0.4f * (0.5f + 0.5f * wobble));
		return Mth.clamp(1.0f - drop, 0.15f, 1.0f);
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

	/** ARGB {top, bottom} for the destination-view placeholder, keyed by dimension. */
	private static int[] dimensionColors(String dim) {
		return switch (dim) {
			case "minecraft:the_nether" -> new int[]{0xD8551111, 0xD8FF6A1A};
			case "minecraft:the_end" -> new int[]{0xD8140A28, 0xD85A3A88};
			case "minecraft:overworld" -> new int[]{0xD85AA0FF, 0xD8B8E0A0};
			default -> dim.isEmpty()
				? new int[]{0xC0444444, 0xC0888888}       // no destination
				: new int[]{0xD82A6E6E, 0xD87FD4D4};      // some other/modded dimension
		};
	}

	/** Filled disc (radius 0..RIM_INNER) with a top-to-bottom color gradient. */
	private static void windowDisc(PoseStack.Pose pose, VertexConsumer vc, int top, int bottom, float alphaMul) {
		int center = lerpColor(bottom, top, 0.5f);
		for (int i = 0; i < SEGMENTS; i++) {
			double t0 = (i / (double) SEGMENTS) * Math.PI * 2.0;
			double t1 = ((i + 1) / (double) SEGMENTS) * Math.PI * 2.0;
			float x0 = (float) Math.cos(t0) * RIM_INNER, y0 = CENTER_Y + (float) Math.sin(t0) * RIM_INNER;
			float x1 = (float) Math.cos(t1) * RIM_INNER, y1 = CENTER_Y + (float) Math.sin(t1) * RIM_INNER;
			int c0 = gradient(y0, top, bottom);
			int c1 = gradient(y1, top, bottom);

			// front (normal +Z), degenerate quad = triangle
			vertex(pose, vc, 0.0f, CENTER_Y, 0.0f, center, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x0, y0, 0.0f, c0, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x1, y1, 0.0f, c1, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x1, y1, 0.0f, c1, alphaMul, 0.0f, 0.0f, 1.0f);
			// back (normal -Z), reversed
			vertex(pose, vc, 0.0f, CENTER_Y, 0.0f, center, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x1, y1, 0.0f, c1, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x0, y0, 0.0f, c0, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x0, y0, 0.0f, c0, alphaMul, 0.0f, 0.0f, -1.0f);
		}
	}

	/** Solid annulus (radius RIM_INNER..RADIUS) forming the glowing frame. */
	private static void rimRing(PoseStack.Pose pose, VertexConsumer vc, int color, float alphaMul) {
		for (int i = 0; i < SEGMENTS; i++) {
			double t0 = (i / (double) SEGMENTS) * Math.PI * 2.0;
			double t1 = ((i + 1) / (double) SEGMENTS) * Math.PI * 2.0;
			float ci0 = (float) Math.cos(t0), si0 = (float) Math.sin(t0);
			float ci1 = (float) Math.cos(t1), si1 = (float) Math.sin(t1);

			float ix0 = ci0 * RIM_INNER, iy0 = CENTER_Y + si0 * RIM_INNER;
			float ox0 = ci0 * RADIUS, oy0 = CENTER_Y + si0 * RADIUS;
			float ix1 = ci1 * RIM_INNER, iy1 = CENTER_Y + si1 * RIM_INNER;
			float ox1 = ci1 * RADIUS, oy1 = CENTER_Y + si1 * RADIUS;

			// front (normal +Z)
			vertex(pose, vc, ix0, iy0, 0.0f, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, ox0, oy0, 0.0f, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, ox1, oy1, 0.0f, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, ix1, iy1, 0.0f, color, alphaMul, 0.0f, 0.0f, 1.0f);
			// back (normal -Z), reversed
			vertex(pose, vc, ix0, iy0, 0.0f, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, ix1, iy1, 0.0f, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, ox1, oy1, 0.0f, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, ox0, oy0, 0.0f, color, alphaMul, 0.0f, 0.0f, -1.0f);
		}
	}

	private static int gradient(float y, int top, int bottom) {
		float t = Mth.clamp((y - (CENTER_Y - RADIUS)) / (2.0f * RADIUS), 0.0f, 1.0f);
		return lerpColor(bottom, top, t);
	}

	private static int lerpColor(int c0, int c1, float t) {
		int a = Mth.lerpInt(t, (c0 >>> 24) & 0xFF, (c1 >>> 24) & 0xFF);
		int r = Mth.lerpInt(t, (c0 >>> 16) & 0xFF, (c1 >>> 16) & 0xFF);
		int g = Mth.lerpInt(t, (c0 >>> 8) & 0xFF, (c1 >>> 8) & 0xFF);
		int b = Mth.lerpInt(t, c0 & 0xFF, c1 & 0xFF);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer vc,
			float x, float y, float z, int argb, float alphaMul, float nx, float ny, float nz) {
		int alpha = Mth.clamp((int) (((argb >>> 24) & 0xFF) * alphaMul), 0, 255);
		vc.addVertex(pose, x, y, z)
			.setColor((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, alpha)
			.setUv(0.5f, 0.5f)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(FULL_BRIGHT)
			.setNormal(pose, nx, ny, nz);
	}
}
