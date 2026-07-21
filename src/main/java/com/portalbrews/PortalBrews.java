package com.portalbrews;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalBrews implements ModInitializer {
	public static final String MOD_ID = "portalbrews";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PortalBrewsRegistry.init();

		FabricPotionBrewingBuilder.BUILD.register(builder -> {
			// Register our potions as brewing "containers" so they are accepted as the
			// input of a later brew. Without this, hasContainerMix() rejects them and only
			// the first step (splash potion -> Portal Potion) works, because the vanilla
			// splash potion is the only registered container in that chain.
			builder.addContainer(PortalBrewsRegistry.PORTAL_POTION_ITEM);
			builder.addContainer(PortalBrewsRegistry.PORTAL_FRAME_POTION_ITEM);
			builder.addContainer(PortalBrewsRegistry.PERMANENT_PORTAL_FRAME_POTION_ITEM);

			builder.registerItemRecipe(Items.SPLASH_POTION, Ingredient.of(Items.COMPASS), PortalBrewsRegistry.PORTAL_POTION_ITEM);
			builder.registerItemRecipe(PortalBrewsRegistry.PORTAL_POTION_ITEM, Ingredient.of(Items.ENDER_PEARL), PortalBrewsRegistry.PORTAL_FRAME_POTION_ITEM);
			builder.registerItemRecipe(PortalBrewsRegistry.PORTAL_FRAME_POTION_ITEM, Ingredient.of(Items.GHAST_TEAR), PortalBrewsRegistry.PERMANENT_PORTAL_FRAME_POTION_ITEM);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
			PortalCommand.register(dispatcher)
		);

		// Breaking a lodestone tears down every portal frame that points at it.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world instanceof ServerLevel serverLevel && state.is(Blocks.LODESTONE)) {
				PortalFrameEntity.onLodestoneBroken(
					serverLevel.getServer(),
					serverLevel.dimension().identifier().toString(),
					pos
				);
			}
		});

		LOG.info("Portal Brews initialized");
	}
}