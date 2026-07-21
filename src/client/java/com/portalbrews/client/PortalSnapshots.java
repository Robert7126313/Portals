package com.portalbrews.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.portalbrews.PortalBrews;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side store of "postcards" of portal destinations. When a compass is linked
 * to a lodestone the current view is captured and saved to disk keyed by the lodestone's
 * dimension + position; the portal renderer then paints that image onto the disc. The
 * player can also replace it with an image of their own.
 *
 * Everything is local (single-player / whoever captured it); sharing to other players
 * would need networking, which this does not do.
 */
public final class PortalSnapshots {
	private PortalSnapshots() {}

	private static final int SNAP_SIZE = 400;
	private static final Map<String, Identifier> LOADED = new ConcurrentHashMap<>();
	private static final Set<String> ABSENT = ConcurrentHashMap.newKeySet();

	private static volatile String pendingKey = null;
	private static int captureWait = 0;

	/** Stable key for a destination lodestone (safe as both a filename and an id path). */
	public static String keyFor(String dimensionId, BlockPos pos) {
		String safe = dimensionId.replace(':', '_').replace('/', '_');
		return safe + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
	}

	private static Path dir() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("portalbrews").resolve("snapshots");
	}

	private static Path path(String key) {
		return dir().resolve(key + ".png");
	}

	/** Arm a capture for shortly after a compass links to a lodestone. */
	public static void armCapture(String key) {
		pendingKey = key;
		captureWait = 1; // let one more frame render at the destination first
	}

	/** Called each client tick; grabs the composited frame once a capture is due. */
	public static void clientTick() {
		if (pendingKey == null) return;
		if (captureWait > 0) {
			captureWait--;
			return;
		}
		String key = pendingKey;
		pendingKey = null;
		RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
		if (target == null) return;
		Screenshot.takeScreenshot(target, image -> saveCapture(key, image));
	}

	private static void saveCapture(String key, NativeImage src) {
		try {
			writeSquare(key, src);
			PortalBrews.LOG.info("[portalbrews] captured portal snapshot for {}", key);
		} catch (Exception e) {
			PortalBrews.LOG.error("[portalbrews] failed to save portal snapshot", e);
		} finally {
			src.close();
		}
	}

	/** Center-crop to a square, downscale, and write the snapshot png. */
	private static void writeSquare(String key, NativeImage src) throws Exception {
		int w = src.getWidth();
		int h = src.getHeight();
		int side = Math.min(w, h);
		int ox = (w - side) / 2;
		int oy = (h - side) / 2;
		NativeImage out = new NativeImage(NativeImage.Format.RGBA, SNAP_SIZE, SNAP_SIZE, false);
		for (int dy = 0; dy < SNAP_SIZE; dy++) {
			int sy = oy + dy * side / SNAP_SIZE;
			for (int dx = 0; dx < SNAP_SIZE; dx++) {
				int sx = ox + dx * side / SNAP_SIZE;
				out.setPixel(dx, dy, src.getPixel(sx, sy));
			}
		}
		Path p = path(key);
		Files.createDirectories(p.getParent());
		out.writeToFile(p);
		out.close();
		invalidate(key);
	}

	/** Replace a destination's snapshot with an image the player picked from disk. */
	public static boolean importImage(String key, Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			NativeImage src = NativeImage.read(in);
			writeSquare(key, src);
			src.close();
			return true;
		} catch (Exception e) {
			PortalBrews.LOG.error("[portalbrews] failed to import portal image", e);
			return false;
		}
	}

	/** Drop caches for a key so the renderer re-reads it from disk on the next frame. */
	public static void invalidate(String key) {
		ABSENT.remove(key);
		LOADED.remove(key);
	}

	/** Registered texture id for a snapshot, or null if none exists. Lazily loads from disk. */
	public static Identifier textureId(String key) {
		Identifier existing = LOADED.get(key);
		if (existing != null) return existing;
		if (ABSENT.contains(key)) return null;
		Path p = path(key);
		if (!Files.exists(p)) {
			ABSENT.add(key);
			return null;
		}
		try (InputStream in = Files.newInputStream(p)) {
			NativeImage img = NativeImage.read(in);
			DynamicTexture tex = new DynamicTexture(() -> "portalbrews_snapshot_" + key, img);
			Identifier id = Identifier.fromNamespaceAndPath("portalbrews", "snapshot/" + idPath(key));
			Minecraft.getInstance().getTextureManager().register(id, tex);
			LOADED.put(key, id);
			return id;
		} catch (Exception e) {
			PortalBrews.LOG.error("[portalbrews] failed to load portal snapshot {}", key, e);
			ABSENT.add(key);
			return null;
		}
	}

	private static String idPath(String key) {
		return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
	}
}
