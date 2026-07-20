package com.portalbrews;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class PortalBrewsRegistry {
	private PortalBrewsRegistry() {}

	public static final ResourceKey<Item> PORTAL_POTION_KEY = ResourceKey.create(
		Registries.ITEM,
		Identifier.fromNamespaceAndPath(PortalBrews.MOD_ID, "portal_potion")
	);

	public static final Item PORTAL_POTION_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		PORTAL_POTION_KEY.identifier(),
		new PortalPotionItem(new Item.Properties()
			.setId(PORTAL_POTION_KEY)
			.stacksTo(1)
			.rarity(Rarity.RARE))
	);

	public static final ResourceKey<EntityType<?>> PORTAL_POTION_ENTITY_KEY = ResourceKey.create(
		Registries.ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(PortalBrews.MOD_ID, "portal_potion")
	);

	public static final EntityType<PortalPotionEntity> PORTAL_POTION_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		PORTAL_POTION_ENTITY_KEY.identifier(),
		EntityType.Builder.<PortalPotionEntity>of(PortalPotionEntity::new, MobCategory.MISC)
			.sized(0.25f, 0.25f)
			.clientTrackingRange(4)
			.updateInterval(10)
			.build(PORTAL_POTION_ENTITY_KEY)
	);

	public static void init() {
	}
}