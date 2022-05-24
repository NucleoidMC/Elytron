package io.github.haykam821.elytron.game;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public record PlayerEntry(ServerPlayerEntity player, Block trail) {
	public void startGliding(TitleFadeS2CPacket titleFadePacket, TitleS2CPacket titlePacket) {
		this.player.networkHandler.sendPacket(titleFadePacket);
		this.player.networkHandler.sendPacket(titlePacket);

		this.player.startFallFlying();
	}

	public Text getDisplayName() {
		return this.player.getDisplayName();
	}

	public Text getWinText() {
		return new TranslatableText("text.elytron.win", this.getDisplayName()).formatted(Formatting.GOLD);
	}

	// Inventory
	public static ItemStack getElytraStack() {
		ItemStack stack = new ItemStack(Items.ELYTRA);
		stack.addEnchantment(Enchantments.BINDING_CURSE, 1);

		NbtCompound tag = stack.getOrCreateNbt();
		tag.putBoolean("Unbreakable", true);

		return stack;
	}

	public static ItemStack getFireworkRocketStack() {
		return new ItemStack(Items.FIREWORK_ROCKET);
	}

	public static void fillHotbarWithFireworkRockets(ServerPlayerEntity player) {
		for (int slot = 0; slot < 9; slot++) {
			player.getInventory().setStack(slot, PlayerEntry.getFireworkRocketStack());
		}

		player.currentScreenHandler.sendContentUpdates();
		player.playerScreenHandler.onContentChanged(player.getInventory());
	}
}
