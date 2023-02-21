package io.github.haykam821.elytron.game.phase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.github.haykam821.elytron.Main;
import io.github.haykam821.elytron.game.ElytronConfig;
import io.github.haykam821.elytron.game.PlayerEntry;
import io.github.haykam821.elytron.game.map.ElytronMap;
import io.github.haykam821.elytron.game.map.ElytronMapConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ElytronActivePhase {
	private static final int STARTING_INVULNERABILITY_TICKS = 120;
	private static final int ELYTRA_OPEN_TICKS = 40;
	private static final int INTERPOLATION_STEPS = 3;

	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final ElytronMap map;
	private final ElytronConfig config;
	private final Set<PlayerEntry> players = new HashSet<>();
	private boolean singleplayer;
	private final Map<Block, Long2IntMap> trailPositions = new HashMap<>();
	private int invulnerabilityTicks = STARTING_INVULNERABILITY_TICKS;

	public ElytronActivePhase(GameSpace gameSpace, ServerWorld world, ElytronMap map, ElytronConfig config) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, ElytronMap map, ElytronConfig config) {
		ElytronActivePhase phase = new ElytronActivePhase(gameSpace, world, map, config);
		gameSpace.setActivity(activity -> {
			ElytronActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PlayerDamageEvent.EVENT, phase::onPlayerDamage);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(ItemUseEvent.EVENT, phase::onUseItem);
		});
	}

	private void enable() {
		ElytronMapConfig mapConfig = this.config.getMapConfig();
		int spawnRadius = (Math.min(mapConfig.getZ(), mapConfig.getX()) - 10) / 2;

		Vec3d center = this.map.getInnerBox().getCenter();

		int index = 0;
		int total = this.gameSpace.getPlayers().size();

 		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			this.players.add(new PlayerEntry(player, Main.getTrailBlock(index)));
	
			player.changeGameMode(GameMode.ADVENTURE);
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, STARTING_INVULNERABILITY_TICKS - ELYTRA_OPEN_TICKS, 15, true, false));

			player.equipStack(EquipmentSlot.CHEST, PlayerEntry.getElytraStack());
			PlayerEntry.fillHotbarWithFireworkRockets(player);

			double theta = ((double) index++ / total) * 2 * Math.PI;
			float yaw = (float) theta * MathHelper.DEGREES_PER_RADIAN + 90;

			double x = center.getX() + Math.cos(theta) * spawnRadius;
			double z = center.getZ() + Math.sin(theta) * spawnRadius;

			player.teleport(this.world, x, this.map.getInnerBox().minY, z, yaw, 0);
		}

		this.singleplayer = this.players.size() == 1;
	}

	private void addTrailBlock(Block block, BlockPos pos, int ticks, Map<Block, Long2IntMap> trailPositions) {
		trailPositions.putIfAbsent(block, new Long2IntOpenHashMap());

		Long2IntMap map = trailPositions.get(block);
		map.put(pos.asLong(), ticks);
	}

	private void addTrailBlocks(PlayerEntry player, BlockPos.Mutable pos, int ticks, int height, Map<Block, Long2IntMap> trailPositions) {
		Block trail = player.getTrail();

		Vec3d start = player.getPreviousPos();
		Vec3d end = player.getPos();

		double relativeX = end.getX() - start.getX();
		double relativeY = end.getY() - start.getY();
		double relativeZ = end.getZ() - start.getZ();

		for (int step = 1; step <= INTERPOLATION_STEPS; step++) {
			double progress = step / (double) INTERPOLATION_STEPS;

			pos.setX((int) (start.getX() + relativeX * progress));
			pos.setY((int) (start.getY() + relativeY * progress));
			pos.setZ((int) (start.getZ() + relativeZ * progress));

			for (int y = 0; y < height; y++) {
				this.addTrailBlock(trail, pos, ticks, trailPositions);
				pos.move(Direction.UP);
			}
		}
	}

	private void tick() {
		if (this.invulnerabilityTicks > 0) {
			this.invulnerabilityTicks -= 1;
		}
		if (this.invulnerabilityTicks == ELYTRA_OPEN_TICKS) {
			TitleFadeS2CPacket titleFadePacket = new TitleFadeS2CPacket(5, 60, 5);
			TitleS2CPacket titlePacket = new TitleS2CPacket(Text.translatable("text.elytron.open_elytra").formatted(Formatting.BLUE));

			for (PlayerEntry player : this.players) {
				player.startGliding(titleFadePacket, titlePacket);
			}
		}
		
		BlockPos.Mutable trailPos = new BlockPos.Mutable();

		Map<Block, Long2IntMap> temporaryTrailPositions = new HashMap<>();;
		Iterator<Map.Entry<Block, Long2IntMap>> blockEntryIterator = this.trailPositions.entrySet().iterator();
		while (blockEntryIterator.hasNext()) {
			Map.Entry<Block, Long2IntMap> blockEntry = blockEntryIterator.next();
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
							this.addTrailBlock(Blocks.AIR, trailPos, this.config.getDecay(), temporaryTrailPositions);
						}
					}

					iterator.remove();
				} else {
					entry.setValue(ticksLeft - 1);
				}
			}
		}
		this.trailPositions.putAll(temporaryTrailPositions);

		Iterator<PlayerEntry> playerIterator = this.players.iterator();
		while (playerIterator.hasNext()) {
			PlayerEntry entry = playerIterator.next();
			ServerPlayerEntity player = entry.getPlayer();

			if (!this.map.getInnerBox().expand(0.5).contains(entry.getPos())) {
				this.eliminate(entry, "text.elytron.eliminated.out_of_bounds", false);
				playerIterator.remove();
			}

			trailPos.set(player.getX(), player.getY(), player.getZ());
			BlockState state = this.world.getBlockState(trailPos);
			if (Main.isTrailBlock(state.getBlock())) {
				this.eliminate(entry, "text.elytron.eliminated.fly_into_trail", false);
				playerIterator.remove();
			}

			if (this.invulnerabilityTicks == 0) {
				if (!player.isFallFlying()) {
					this.eliminate(entry, "text.elytron.eliminated.elytra_not_opened", false);
					playerIterator.remove();
				}

				this.addTrailBlocks(entry, trailPos, this.config.getDelay(), this.config.getHeight(), this.trailPositions);
			}

			entry.updatePreviousPos();
		}

		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage());

			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			return this.players.iterator().next().getWinText();
		}
		return Text.translatable("text.elytron.win.none").formatted(Formatting.GOLD);
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getWaitingSpawnPos()).and(() -> {
			this.setSpectator(offer.player());
		});
	}

	private void removePlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);

		if (entry != null) {
			this.eliminate(entry, true);
		}
	}

	private void eliminate(PlayerEntry eliminatedPlayer, String reason, boolean remove) {
		if (!this.players.contains(eliminatedPlayer)) return;

		Text message = Text.translatable(reason, eliminatedPlayer.getDisplayName()).formatted(Formatting.RED);
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			player.sendMessage(message, false);
		}

		if (remove) {
			this.players.remove(eliminatedPlayer);
		}
		this.setSpectator(eliminatedPlayer.getPlayer());
	}

	private void eliminate(PlayerEntry eliminatedPlayer, boolean remove) {
		this.eliminate(eliminatedPlayer, "text.elytron.eliminated", remove);
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		PlayerEntry entry = this.getPlayerEntry(player);

		if (entry != null) {
			if (source == DamageSource.FLY_INTO_WALL) {
				if (this.map.getInnerInnerBox().contains(player.getPos())) {
					this.eliminate(entry, "text.elytron.eliminated.fly_into_trail", true);
				} else {
					this.eliminate(entry, "text.elytron.eliminated.fly_into_wall", true);
				}
			} else if (source == DamageSource.FALL) {
				this.eliminate(entry, "text.elytron.eliminated.fall", true);
			}
		}

		return ActionResult.FAIL;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		PlayerEntry entry = this.getPlayerEntry(player);

		if (entry != null) {
			this.eliminate(entry, true);
		}

		return ActionResult.SUCCESS;
	}

	private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
		PlayerEntry.fillHotbarWithFireworkRockets(player);

		ItemStack handStack = player.getStackInHand(hand);
		if (handStack.getItem() == Items.FIREWORK_ROCKET && player.isFallFlying()) {
			handStack.increment(1);
		}
		
		return TypedActionResult.pass(handStack);
	}

	private PlayerEntry getPlayerEntry(ServerPlayerEntity player) {
		for (PlayerEntry entry : this.players) {
			if (player == entry.getPlayer()) {
				return entry;
			}
		}

		return null;
	}
}
