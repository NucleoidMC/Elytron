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
import net.minecraft.util.math.Vec3d;

public class PlayerEntry {
	private final ServerPlayerEntity player;
	private final Block trail;

	private Vec3d previousPos;

	public PlayerEntry(ServerPlayerEntity player, Block trail) {
		this.player = player;
		this.trail = trail;

		this.updatePreviousPos();
	}

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

	// Getters
	public ServerPlayerEntity getPlayer() {
		return this.player;
	}

	public Block getTrail() {
		return this.trail;
	}

	// Position
	public Vec3d getPos() {
		return this.player.getPos();
	}

	public Vec3d getPreviousPos() {
		return this.previousPos;
	}

	public void updatePreviousPos() {
		this.previousPos = this.getPos();
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
