package com.portalbrews;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;
import java.util.Optional;

public final class PortalCommand {
	private PortalCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("portal").executes(ctx -> {
			ServerPlayer player = ctx.getSource().getPlayerOrException();
			ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
			book.set(DataComponents.WRITTEN_BOOK_CONTENT, buildManual());
			player.getInventory().placeItemBackInInventory(book);
			return 1;
		}));
	}

	private static WrittenBookContent buildManual() {
		return new WrittenBookContent(
			Filterable.passThrough("Portal Brews Manual"),
			"Portal Brews",
			0,
			List.of(
				page(
					title("Portal Brews"),
					"\n\nBrew a splash potion that teleports whoever it splashes to a location stored in a lodestone compass.\n\nCross-dimension travel is supported.\n\nTurn the page for the recipe."
				),
				page(
					title("1. Link a compass"),
					"\n\nPlace a ", keyword("lodestone"), " at your destination.\n\nRight-click it with a ", keyword("compass"), " to bind that block's coordinates and dimension into the compass."
				),
				page(
					title("2. Make splash water"),
					"\n\nFill a ", keyword("bottle"), " at water.\n\nBrew ", keyword("gunpowder"), " into the water bottle in a brewing stand to convert it into a ", keyword("splash water bottle"), "."
				),
				page(
					title("3. Brew the potion"),
					"\n\nPut the splash water bottle in the brewing stand.\n\nPut your linked ", keyword("lodestone compass"), " in the ingredient slot.\n\nAdd blaze powder if empty.\n\nOut comes a ", keyword("Portal Potion"), " storing the target."
				),
				page(
					title("4. Throw it"),
					"\n\nRight-click to throw, just like a splash potion.\n\nAnything inside the splash radius (~4 blocks) is teleported.\n\nThrow at your feet to teleport yourself. Throw at a mob to banish it."
				),
				page(
					title("Notes"),
					"\n\n- A compass not linked to a lodestone brews into a ", keyword("dead"), " potion that does nothing.\n\n- Destination is one block above the lodestone.\n\n- Set lodestones on floors, not in walls."
				)
			),
			true
		);
	}

	private static Filterable<Component> page(Object... parts) {
		net.minecraft.network.chat.MutableComponent page = Component.empty();
		for (Object part : parts) {
			if (part instanceof Component c) {
				page.append(c);
			} else {
				page.append(Component.literal(part.toString()));
			}
		}
		return Filterable.passThrough(page);
	}

	private static Component title(String text) {
		return Component.literal(text).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
	}

	private static Component keyword(String text) {
		return Component.literal(text).withStyle(ChatFormatting.DARK_AQUA);
	}
}