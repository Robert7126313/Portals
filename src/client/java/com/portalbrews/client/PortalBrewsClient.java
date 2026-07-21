package com.portalbrews.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.portalbrews.PortalBrewsRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;

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

		// Linking a compass to a lodestone snapshots the current view for that destination.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
				ItemStack held = player.getItemInHand(hand);
				if (held.is(Items.COMPASS) && level.getBlockState(hit.getBlockPos()).is(Blocks.LODESTONE)) {
					String key = PortalSnapshots.keyFor(level.dimension().identifier().toString(), hit.getBlockPos());
					PortalSnapshots.armCapture(key);
				}
			}
			return InteractionResult.PASS;
		});

		// Keybind: replace the held linked compass's portal image with one from disk.
		KeyMapping setImageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.portalbrews.set_snapshot",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			KeyMapping.Category.MISC
		));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			PortalSnapshots.clientTick();
			while (setImageKey.consumeClick()) {
				pickImageForHeldCompass(mc);
			}
		});
	}

	private static void pickImageForHeldCompass(Minecraft mc) {
		if (mc.player == null) return;
		ItemStack held = mc.player.getMainHandItem();
		LodestoneTracker tracker = held.get(DataComponents.LODESTONE_TRACKER);
		if (tracker == null || tracker.target().isEmpty()) {
			mc.player.sendSystemMessage(
				Component.literal("Hold a lodestone compass to set its portal image").withStyle(ChatFormatting.RED));
			return;
		}
		GlobalPos gp = tracker.target().get();
		String key = PortalSnapshots.keyFor(gp.dimension().identifier().toString(), gp.pos());
		String chosen = TinyFileDialogs.tinyfd_openFileDialog(
			"Choose portal image", "", (PointerBuffer) null, (CharSequence) null, false);
		if (chosen != null) {
			boolean ok = PortalSnapshots.importImage(key, Path.of(chosen));
			mc.player.sendSystemMessage(
				Component.literal(ok ? "Portal image set" : "Could not load that image")
					.withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
	}
}
