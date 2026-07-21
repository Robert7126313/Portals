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

import java.util.Random;

/**
 * Draws the portal frame as a circular, emissive, double-sided disc whose interior
 * shows the destination dimension's <em>sky</em>: an overworld day/night gradient with
 * a moving sun or moon and a twinkling starfield, the nether's crimson murk, or the
 * end's starry void. It's an atmospheric stand-in for a true see-through view (that
 * would need a second-camera world re-render); the destination's time of day is synced
 * from the frame entity so the sky roughly tracks the far side.
 *
 * A solid rim frames it - orange for temporary frames, violet for permanent - and the
 * destination coordinates float above as a name label. A flat white texture is used so
 * vertex colors tint cleanly; layers carry tiny depth offsets so they sort correctly
 * when viewed from either face.
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
	private static final int PERMANENT_RIM = 0xFFB24BFF;  // permanent: steady violet
	private static final int BREAK_COLOR = 0xFFFF2410;    // red-orange for a dying frame
	/** Ticks before expiry at which a temporary frame starts visibly destabilizing. */
	private static final int BREAK_WARN_TICKS = 100;

	// Sky palette (0xE6 alpha so the sky reads as a solid-ish window).
	private static final int OW_DAY_TOP = 0xE64C86E6, OW_DAY_HORIZON = 0xE6BFE0FF;
	private static final int OW_NIGHT_TOP = 0xE604060F, OW_NIGHT_HORIZON = 0xE60E1A34;
	private static final int OW_SUNSET = 0xE6E8722A;
	private static final int NETHER_TOP = 0xE61A0605, NETHER_HORIZON = 0xE67A2408;
	private static final int END_TOP = 0xE60A0716, END_HORIZON = 0xE6241640;
	private static final int UNKNOWN_TOP = 0xE6123232, UNKNOWN_HORIZON = 0xE62F6E6E;
	private static final int NONE_TOP = 0xE0303030, NONE_HORIZON = 0xE0606060;
	private static final int SUN = 0xFFFFEC88, MOON = 0xFFE6ECF5;

	// Placement + per-layer depth offsets (blocks) so translucent layers sort front-to-back.
	private static final float CELE_PLACE = 0.55f;
	private static final float SKY_DEPTH = 0.002f;
	private static final float STAR_DEPTH = 0.006f;
	private static final float CELE_GLOW_DEPTH = 0.010f;
	private static final float CELE_DEPTH = 0.012f;
	private static final float RIM_DEPTH = 0.016f;

	// Fixed star layout inside the disc, generated once.
	private static final int STAR_COUNT = 45;
	private static final float[] STAR_X = new float[STAR_COUNT];
	private static final float[] STAR_Y = new float[STAR_COUNT];
	static {
		Random rng = new Random(0xBEEFCAFEL);
		for (int i = 0; i < STAR_COUNT; i++) {
			double ang = rng.nextDouble() * Math.PI * 2.0;
			double rad = Math.sqrt(rng.nextDouble()) * RADIUS * 0.86;
			STAR_X[i] = (float) (Math.cos(ang) * rad);
			STAR_Y[i] = CENTER_Y + (float) (Math.sin(ang) * rad);
		}
	}

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
		state.destinationTime = entity.getDestinationTime();
		state.instability = entity.isPermanent()
			? 0.0f
			: 1.0f - Mth.clamp(entity.getRemainingTicks() / (float) BREAK_WARN_TICKS, 0.0f, 1.0f);
		state.nameTag = buildLabel(entity);
		state.nameTagAttachment = new Vec3(0.0, CENTER_Y + RADIUS + 0.4, 0.0);
	}

	@Override
	public void submit(PortalFrameRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, pose, collector, camera); // name label (+ shadow, if any)

		Sky sky = Sky.forDestination(state.destinationDimension, state.destinationTime);
		int baseRim = state.permanent ? PERMANENT_RIM : TEMPORARY_RIM;
		int rim = state.instability > 0.0f
			? lerpColor(baseRim, BREAK_COLOR, state.instability * 0.85f)
			: baseRim;
		float alphaMul = flicker(state.instability, state.ageInTicks);
		float age = state.ageInTicks;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
		RenderType renderType = RenderTypes.entityTranslucentEmissive(TEXTURE);
		collector.submitCustomGeometry(pose, renderType, (p, vc) -> {
			gradientDisc(p, vc, sky.top, sky.horizon, alphaMul, SKY_DEPTH);
			if (sky.drawStars && sky.starAlpha > 0.03f) {
				starField(p, vc, alphaMul, age, sky.starAlpha);
			}
			if (sky.drawCelestial) {
				float cx = sky.celX * RADIUS * CELE_PLACE;
				float cy = CENTER_Y + sky.celY * RADIUS * CELE_PLACE;
				int glow = (0x50 << 24) | (sky.celestial & 0xFFFFFF);
				filledCircle(p, vc, cx, cy, sky.celRadius * 1.6f, glow, alphaMul, CELE_GLOW_DEPTH);
				filledCircle(p, vc, cx, cy, sky.celRadius, sky.celestial, alphaMul, CELE_DEPTH);
			}
			rimRing(p, vc, rim, alphaMul);
		});
		pose.popPose();
	}

	/** As instability rises, the frame dims and stutters erratically - a "breaking soon" tell. */
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

	/** The sky look for a destination: gradient, optional stars, optional sun/moon. */
	private static final class Sky {
		int top, horizon;
		boolean drawStars;
		float starAlpha;
		boolean drawCelestial;
		int celestial;
		float celX, celY;   // normalized -1..1 within the disc
		float celRadius;    // block-space radius of the body

		static Sky forDestination(String dim, int time) {
			Sky s = new Sky();
			switch (dim) {
				case "minecraft:overworld" -> {
					double phase = (time - 6000) / 24000.0 * Math.PI * 2.0;
					float sunY = (float) Math.cos(phase); // +1 noon, -1 midnight
					float sunX = (float) Math.sin(phase); // -1 dawn, +1 dusk
					float day = Mth.clamp((sunY + 0.2f) / 0.4f, 0.0f, 1.0f);
					s.top = lerpColor(OW_NIGHT_TOP, OW_DAY_TOP, day);
					s.horizon = lerpColor(OW_NIGHT_HORIZON, OW_DAY_HORIZON, day);
					float sunset = 1.0f - Mth.clamp(Math.abs(sunY) / 0.35f, 0.0f, 1.0f);
					s.horizon = lerpColor(s.horizon, OW_SUNSET, sunset * 0.7f);
					if (sunY > -0.15f) {
						s.drawCelestial = true;
						s.celestial = SUN;
						s.celX = sunX;
						s.celY = sunY;
						s.celRadius = 0.30f;
					} else if (-sunY > -0.15f) {
						s.drawCelestial = true;
						s.celestial = MOON;
						s.celX = -sunX;
						s.celY = -sunY;
						s.celRadius = 0.24f;
					}
					s.drawStars = day < 0.85f;
					s.starAlpha = 1.0f - day;
				}
				case "minecraft:the_nether" -> {
					s.top = NETHER_TOP;
					s.horizon = NETHER_HORIZON;
				}
				case "minecraft:the_end" -> {
					s.top = END_TOP;
					s.horizon = END_HORIZON;
					s.drawStars = true;
					s.starAlpha = 1.0f;
				}
				default -> {
					if (dim.isEmpty()) {
						s.top = NONE_TOP;
						s.horizon = NONE_HORIZON;
					} else {
						s.top = UNKNOWN_TOP;
						s.horizon = UNKNOWN_HORIZON;
					}
				}
			}
			return s;
		}
	}

	private static void starField(PoseStack.Pose pose, VertexConsumer vc, float alphaMul, float age, float starAlpha) {
		for (int i = 0; i < STAR_COUNT; i++) {
			float twinkle = 0.55f + 0.45f * (float) Math.sin(age * 0.15f + i * 1.7f);
			int a = Mth.clamp((int) (255 * twinkle * starAlpha), 0, 255);
			int color = (a << 24) | 0xFFFFFF;
			dot(pose, vc, STAR_X[i], STAR_Y[i], 0.03f, color, alphaMul, STAR_DEPTH);
		}
	}

	/** Filled disc (radius 0..RIM_INNER) with a top-to-bottom color gradient. */
	private static void gradientDisc(PoseStack.Pose pose, VertexConsumer vc, int top, int bottom, float alphaMul, float depth) {
		int center = lerpColor(bottom, top, 0.5f);
		for (int i = 0; i < SEGMENTS; i++) {
			double t0 = (i / (double) SEGMENTS) * Math.PI * 2.0;
			double t1 = ((i + 1) / (double) SEGMENTS) * Math.PI * 2.0;
			float x0 = (float) Math.cos(t0) * RIM_INNER, y0 = CENTER_Y + (float) Math.sin(t0) * RIM_INNER;
			float x1 = (float) Math.cos(t1) * RIM_INNER, y1 = CENTER_Y + (float) Math.sin(t1) * RIM_INNER;
			int c0 = gradient(y0, top, bottom);
			int c1 = gradient(y1, top, bottom);

			vertex(pose, vc, 0.0f, CENTER_Y, depth, center, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x0, y0, depth, c0, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x1, y1, depth, c1, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x1, y1, depth, c1, alphaMul, 0.0f, 0.0f, 1.0f);

			vertex(pose, vc, 0.0f, CENTER_Y, -depth, center, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x1, y1, -depth, c1, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x0, y0, -depth, c0, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x0, y0, -depth, c0, alphaMul, 0.0f, 0.0f, -1.0f);
		}
	}

	/** Small filled circle (sun/moon), double-sided. */
	private static void filledCircle(PoseStack.Pose pose, VertexConsumer vc, float cx, float cy, float r, int color, float alphaMul, float depth) {
		int seg = 18;
		for (int i = 0; i < seg; i++) {
			double a0 = (i / (double) seg) * Math.PI * 2.0;
			double a1 = ((i + 1) / (double) seg) * Math.PI * 2.0;
			float x0 = cx + (float) Math.cos(a0) * r, y0 = cy + (float) Math.sin(a0) * r;
			float x1 = cx + (float) Math.cos(a1) * r, y1 = cy + (float) Math.sin(a1) * r;

			vertex(pose, vc, cx, cy, depth, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x0, y0, depth, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x1, y1, depth, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, x1, y1, depth, color, alphaMul, 0.0f, 0.0f, 1.0f);

			vertex(pose, vc, cx, cy, -depth, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x1, y1, -depth, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x0, y0, -depth, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, x0, y0, -depth, color, alphaMul, 0.0f, 0.0f, -1.0f);
		}
	}

	/** Tiny double-sided quad (a star). */
	private static void dot(PoseStack.Pose pose, VertexConsumer vc, float cx, float cy, float h, int color, float alphaMul, float depth) {
		vertex(pose, vc, cx - h, cy - h, depth, color, alphaMul, 0.0f, 0.0f, 1.0f);
		vertex(pose, vc, cx + h, cy - h, depth, color, alphaMul, 0.0f, 0.0f, 1.0f);
		vertex(pose, vc, cx + h, cy + h, depth, color, alphaMul, 0.0f, 0.0f, 1.0f);
		vertex(pose, vc, cx - h, cy + h, depth, color, alphaMul, 0.0f, 0.0f, 1.0f);

		vertex(pose, vc, cx - h, cy - h, -depth, color, alphaMul, 0.0f, 0.0f, -1.0f);
		vertex(pose, vc, cx - h, cy + h, -depth, color, alphaMul, 0.0f, 0.0f, -1.0f);
		vertex(pose, vc, cx + h, cy + h, -depth, color, alphaMul, 0.0f, 0.0f, -1.0f);
		vertex(pose, vc, cx + h, cy - h, -depth, color, alphaMul, 0.0f, 0.0f, -1.0f);
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

			vertex(pose, vc, ix0, iy0, RIM_DEPTH, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, ox0, oy0, RIM_DEPTH, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, ox1, oy1, RIM_DEPTH, color, alphaMul, 0.0f, 0.0f, 1.0f);
			vertex(pose, vc, ix1, iy1, RIM_DEPTH, color, alphaMul, 0.0f, 0.0f, 1.0f);

			vertex(pose, vc, ix0, iy0, -RIM_DEPTH, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, ix1, iy1, -RIM_DEPTH, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, ox1, oy1, -RIM_DEPTH, color, alphaMul, 0.0f, 0.0f, -1.0f);
			vertex(pose, vc, ox0, oy0, -RIM_DEPTH, color, alphaMul, 0.0f, 0.0f, -1.0f);
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
