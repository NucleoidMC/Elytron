package io.github.haykam821.elytron.game;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;

public class PlayerEntry {
	private final ServerPlayer player;
	private final Block trail;

	private Vec3 previousPos;

	public PlayerEntry(ServerPlayer player, Block trail) {
		this.player = player;
		this.trail = trail;

		this.updatePreviousPos();
	}

	public void startGliding(ClientboundSetTitlesAnimationPacket titleFadePacket, ClientboundSetTitleTextPacket titlePacket) {
		this.player.connection.send(titleFadePacket);
		this.player.connection.send(titlePacket);

		this.player.startFallFlying();
	}

	public Component getDisplayName() {
		return this.player.getDisplayName();
	}

	public Component getWinText() {
		return Component.translatable("text.elytron.win", this.getDisplayName()).withStyle(ChatFormatting.GOLD);
	}

	// Getters
	public ServerPlayer getPlayer() {
		return this.player;
	}

	public Block getTrail() {
		return this.trail;
	}

	// Position
	public Vec3 getPos() {
		return this.player.position();
	}

	public Vec3 getPreviousPos() {
		return this.previousPos;
	}

	public void updatePreviousPos() {
		this.previousPos = this.getPos();
	}

	// Inventory
	public static ItemStack getElytraStack(HolderLookup.Provider registries) {
		return ItemStackBuilder.of(Items.ELYTRA)
			.addEnchantment(registries, Enchantments.BINDING_CURSE, 1)
			.setUnbreakable()
			.build();
	}

	public static ItemStack getFireworkRocketStack() {
		return new ItemStack(Items.FIREWORK_ROCKET);
	}

	public static void fillHotbarWithFireworkRockets(ServerPlayer player) {
		for (int slot = 0; slot < 9; slot++) {
			player.getInventory().setItem(slot, PlayerEntry.getFireworkRocketStack());
		}

		player.containerMenu.broadcastChanges();
		player.inventoryMenu.slotsChanged(player.getInventory());
	}
}
