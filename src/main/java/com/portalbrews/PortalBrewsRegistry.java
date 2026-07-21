package com.portalbrews;

import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.UUID;

public final class PortalBrewsRegistry {
	private PortalBrewsRegistry() {}

	public static final int PORTAL_POTION_COLOR = 0xD100FF;
	public static final int PORTAL_FRAME_POTION_COLOR = 0x00E5FF;
	public static final int PERMANENT_PORTAL_FRAME_POTION_COLOR = 0xFFD700;

	public static final DataComponentType<UUID> LODESTONE_ID = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(PortalBrews.MOD_ID, "lodestone_id"),
		DataComponentType.<UUID>builder()
			.persistent(UUIDUtil.CODEC)
			.networkSynchronized(UUIDUtil.STREAM_CODEC)
			.cacheEncoding()
			.build()
	);

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

	public static final ResourceKey<Item> PORTAL_FRAME_POTION_KEY = ResourceKey.create(
		Registries.ITEM,
		Identifier.fromNamespaceAndPath(PortalBrews.MOD_ID, "portal_frame_potion")
	);

	public static final Item PORTAL_FRAME_POTION_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		PORTAL_FRAME_POTION_KEY.identifier(),
		new PortalPotionItem(new Item.Properties()
			.setId(PORTAL_FRAME_POTION_KEY)
			.stacksTo(1)
			.rarity(Rarity.RARE))
	);

	public static final ResourceKey<Item> PERMANENT_PORTAL_FRAME_POTION_KEY = ResourceKey.create(
		Registries.ITEM,
		Identifier.fromNamespaceAndPath(PortalBrews.MOD_ID, "permanent_portal_frame_potion")
	);

	public static final Item PERMANENT_PORTAL_FRAME_POTION_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		PERMANENT_PORTAL_FRAME_POTION_KEY.identifier(),
		new PortalPotionItem(new Item.Properties()
			.setId(PERMANENT_PORTAL_FRAME_POTION_KEY)
			.stacksTo(1)
			.rarity(Rarity.EPIC))
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

	public static final ResourceKey<EntityType<?>> PORTAL_FRAME_ENTITY_KEY = ResourceKey.create(
		Registries.ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(PortalBrews.MOD_ID, "portal_frame")
	);

	public static final EntityType<PortalFrameEntity> PORTAL_FRAME_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		PORTAL_FRAME_ENTITY_KEY.identifier(),
		EntityType.Builder.<PortalFrameEntity>of(PortalFrameEntity::new, MobCategory.MISC)
			.sized(3.0f, 3.0f)
			.clientTrackingRange(8)
			.updateInterval(20)
			.build(PORTAL_FRAME_ENTITY_KEY)
	);

	public static void init() {
	}
}