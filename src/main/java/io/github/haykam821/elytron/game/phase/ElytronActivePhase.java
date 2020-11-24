package io.github.haykam821.elytron.game.phase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.github.haykam821.elytron.Main;
import io.github.haykam821.elytron.game.ElytronConfig;
import io.github.haykam821.elytron.game.map.ElytronMap;
import io.github.haykam821.elytron.game.map.ElytronMapConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDamageListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.event.UseItemListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

public class ElytronActivePhase {
	private static final int STARTING_INVULNERABILITY_TICKS = 120;
	private static final int ELYTRA_OPEN_TICKS = 40;

	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final ElytronMap map;
	private final ElytronConfig config;
	private final Set<ServerPlayerEntity> players = new HashSet<>();
	private boolean singleplayer;
	private final Map<Block, Long2IntMap> trailPositions = new HashMap<>();
	private boolean opened;
	private int invulnerabilityTicks = STARTING_INVULNERABILITY_TICKS;

	public ElytronActivePhase(GameSpace gameSpace, ElytronMap map, ElytronConfig config) {
		this.world = gameSpace.getWorld();
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static void setRules(GameLogic game) {
		game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.DENY);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
	}

	public static void open(GameSpace gameSpace, ElytronMap map, ElytronConfig config) {
		ElytronActivePhase phase = new ElytronActivePhase(gameSpace, map, config);
		gameSpace.openGame(game -> {
			ElytronActivePhase.setRules(game);

			// Listeners
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerRemoveListener.EVENT, phase::removePlayer);
			game.on(PlayerDamageListener.EVENT, phase::onPlayerDamage);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(UseItemListener.EVENT, phase::onUseItem);
		});
	}

	private ItemStack getElytraStack() {
		ItemStack stack = new ItemStack(Items.ELYTRA);
		stack.addEnchantment(Enchantments.BINDING_CURSE, 1);

		CompoundTag tag = stack.getOrCreateTag();
		tag.putBoolean("Unbreakable", true);

		return stack;
	}

	private ItemStack getFireworkRocketStack() {
		return new ItemStack(Items.FIREWORK_ROCKET);
	}

	private void fillHotbarWithFireworkRockets(ServerPlayerEntity player) {
		for (int slot = 0; slot < 9; slot++) {
			player.equip(slot, this.getFireworkRocketStack());
		}
	}

	private void open() {
		ElytronMapConfig mapConfig = this.config.getMapConfig();
		int spawnRadius = (Math.min(mapConfig.getZ(), mapConfig.getX()) - 10) / 2;

		Vec3d center = this.map.getInnerBox().getCenter();

		int index = 0;
 		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			this.players.add(player);
	
			player.setGameMode(GameMode.ADVENTURE);
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, STARTING_INVULNERABILITY_TICKS - ELYTRA_OPEN_TICKS, 15, true, false));

			player.equipStack(EquipmentSlot.CHEST, this.getElytraStack());
			this.fillHotbarWithFireworkRockets(player);

			double theta = ((double) index++ / this.players.size()) * 2 * Math.PI;
			double x = center.getX() + Math.sin(theta) * spawnRadius;
			double z = center.getZ() + Math.cos(theta) * spawnRadius;

			player.teleport(this.gameSpace.getWorld(), x, this.map.getInnerBox().minY, z, (float) theta - 180, 0);
		}

		this.opened = true;
		this.singleplayer = this.players.size() == 1;
	}

	private void addTrailBlock(Block block, BlockPos pos, int ticks) {
		this.trailPositions.putIfAbsent(block, new Long2IntOpenHashMap());

		Long2IntMap map = this.trailPositions.get(block);
		map.put(pos.asLong(), ticks);
	}

	private void tick() {
		if (this.invulnerabilityTicks > 0) {
			this.invulnerabilityTicks -= 1;
		}
		if (this.invulnerabilityTicks == ELYTRA_OPEN_TICKS) {
			this.gameSpace.getPlayers().sendTitle(new TranslatableText("text.elytron.open_elytra").formatted(Formatting.BLUE), 5, 60, 5);
		}
		
		BlockPos.Mutable trailPos = new BlockPos.Mutable();
		for (Map.Entry<Block, Long2IntMap> blockEntry : this.trailPositions.entrySet()) {
			BlockState state = blockEntry.getKey().getDefaultState();

			ObjectIterator<Long2IntMap.Entry> iterator = Long2IntMaps.fastIterator(blockEntry.getValue());
			while (iterator.hasNext()) {
				Long2IntMap.Entry entry = iterator.next();
				long trailLongPos = entry.getLongKey();
				int ticksLeft = entry.getIntValue();

				if (ticksLeft == 0) {
					trailPos.set(trailLongPos);

					if (this.map.getInnerBox().contains(trailPos.getX(), trailPos.getY(), trailPos.getZ())) {
						this.world.setBlockState(trailPos, state);

						if (!state.isAir() && this.config.getDecay() >= 0) {
							this.addTrailBlock(Blocks.AIR, trailPos, this.config.getDecay());
						}
					}

					iterator.remove();
				} else {
					entry.setValue(ticksLeft - 1);
				}
			}
		}

		Iterator<ServerPlayerEntity> playerIterator = this.players.iterator();
		int index = 0;
		while (playerIterator.hasNext()) {
			ServerPlayerEntity player = playerIterator.next();
			if (!this.map.getInnerBox().expand(0.5).contains(player.getPos())) {
				this.eliminate(player, "text.elytron.eliminated.out_of_bounds", false);
				playerIterator.remove();
			}

			trailPos.set(player.getX(), player.getY(), player.getZ());
			BlockState state = this.world.getBlockState(trailPos);
			if (Main.isTrailBlock(state.getBlock())) {
				this.eliminate(player, "text.elytron.eliminated.fly_into_trail", false);
				playerIterator.remove();
			}

			if (this.invulnerabilityTicks == 0) {
				if (!player.isFallFlying()) {
					this.eliminate(player, "text.elytron.eliminated.elytra_not_opened", false);
					playerIterator.remove();
				}

				for (int y = 0; y < this.config.getHeight(); y++) {
					trailPos.setY(player.getBlockPos().getY() + y);
					this.addTrailBlock(Main.getTrailBlock(index), trailPos, this.config.getDelay());
				}
			}

			index += 1;
		}

		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage());

			this.gameSpace.close();
		}
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			ServerPlayerEntity winner = this.players.iterator().next();
			return new TranslatableText("text.elytron.win", winner.getDisplayName()).formatted(Formatting.GOLD);
		}
		return new TranslatableText("text.elytron.win.none").formatted(Formatting.GOLD);
	}

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	private void addPlayer(ServerPlayerEntity player) {
		if (!this.players.contains(player)) {
			this.setSpectator(player);
		} else if (this.opened) {
			this.removePlayer(player);
		}
	}

	private void removePlayer(ServerPlayerEntity player) {
		this.eliminate(player, true);
	}

	private void eliminate(ServerPlayerEntity eliminatedPlayer, String reason, boolean remove) {
		Text message = new TranslatableText(reason, eliminatedPlayer.getDisplayName()).formatted(Formatting.RED);
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			player.sendMessage(message, false);
		}

		if (remove) {
			this.players.remove(eliminatedPlayer);
		}
		this.setSpectator(eliminatedPlayer);
	}

	private void eliminate(ServerPlayerEntity eliminatedPlayer, boolean remove) {
		this.eliminate(eliminatedPlayer, "text.elytron.eliminated", remove);
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		if (source == DamageSource.FLY_INTO_WALL) {
			if (this.map.getInnerInnerBox().contains(player.getPos())) {
				this.eliminate(player, "text.elytron.eliminated.fly_into_trail", true);
			} else {
				this.eliminate(player, "text.elytron.eliminated.fly_into_wall", true);
			}
		} else if (source == DamageSource.FALL) {
			this.eliminate(player, "text.elytron.eliminated.fall", true);
		}
		return ActionResult.FAIL;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.eliminate(player, true);
		return ActionResult.SUCCESS;
	}

	private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
		this.fillHotbarWithFireworkRockets(player);

		ItemStack handStack = player.getStackInHand(hand);
		if (handStack.getItem() == Items.FIREWORK_ROCKET && player.isFallFlying()) {
			handStack.increment(1);
		}
		
		return TypedActionResult.pass(handStack);
	}
}
