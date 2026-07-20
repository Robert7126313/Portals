package com.portalbrews;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class PortalPotionItem extends ThrowablePotionItem {
	public PortalPotionItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(getDescriptionId());
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
		LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
		if (tracker == null || tracker.target().isEmpty()) {
			lines.accept(Component.literal("No destination").withStyle(ChatFormatting.RED));
			return;
		}
		GlobalPos gp = tracker.target().get();
		String path = gp.dimension().identifier().toString();
		lines.accept(Component.literal("Destination: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(gp.pos().getX() + ", " + gp.pos().getY() + ", " + gp.pos().getZ())
				.withStyle(ChatFormatting.AQUA)));
		lines.accept(Component.literal("Dimension: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(path).withStyle(ChatFormatting.AQUA)));
	}

	@Override
	protected AbstractThrownPotion createPotion(ServerLevel level, LivingEntity owner, ItemStack stack) {
		PortalPotionEntity e = new PortalPotionEntity(level, owner, stack);
		return e;
	}

	@Override
	protected AbstractThrownPotion createPotion(Level level, Position pos, ItemStack stack) {
		return new PortalPotionEntity(level, pos.x(), pos.y(), pos.z(), stack);
	}
}