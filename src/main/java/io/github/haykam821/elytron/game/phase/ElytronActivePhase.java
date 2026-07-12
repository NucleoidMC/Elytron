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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ElytronActivePhase {
	private static final int STARTING_INVULNERABILITY_TICKS = 120;
	private static final int ELYTRA_OPEN_TICKS = 40;
	private static final int INTERPOLATION_STEPS = 3;

	private final ServerLevel level;
	private final GameSpace gameSpace;
	private final ElytronMap map;
	private final ElytronConfig config;
	private final Set<PlayerEntry> players = new HashSet<>();
	private boolean singleplayer;
	private final Map<Block, Long2IntMap> trailPositions = new HashMap<>();
	private int invulnerabilityTicks = STARTING_INVULNERABILITY_TICKS;
	private int ticksUntilClose = -1;

	public ElytronActivePhase(GameSpace gameSpace, ServerLevel level, ElytronMap map, ElytronConfig config) {
		this.level = level;
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

	public static void open(GameSpace gameSpace, ServerLevel level, ElytronMap map, ElytronConfig config) {
		ElytronActivePhase phase = new ElytronActivePhase(gameSpace, level, map, config);
		gameSpace.setActivity(activity -> {
			ElytronActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PlayerDamageEvent.EVENT, phase::onPlayerDamage);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(ItemUseEvent.EVENT, phase::onUseItem);
		});
	}

	private void enable() {
		PlayerSet participants = this.gameSpace.getPlayers().participants();
		HolderLookup.Provider registries = this.level.registryAccess();

		ElytronMapConfig mapConfig = this.config.getMapConfig();
		int spawnRadius = (Math.min(mapConfig.getZ(), mapConfig.getX()) - 10) / 2;

		Vec3 center = this.map.getInnerBox().getCenter();

		int index = 0;
		int total = participants.size();

 		for (ServerPlayer player : participants) {
			this.players.add(new PlayerEntry(player, Main.getTrailBlock(index)));
	
			player.setGameMode(GameType.ADVENTURE);
			player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, STARTING_INVULNERABILITY_TICKS - ELYTRA_OPEN_TICKS, 15, true, false));

			player.setItemSlot(EquipmentSlot.CHEST, PlayerEntry.getElytraStack(registries));
			PlayerEntry.fillHotbarWithFireworkRockets(player);

			double theta = ((double) index++ / total) * 2 * Math.PI;
			float yaw = (float) theta * Mth.RAD_TO_DEG + 90;

			double x = center.x() + Math.cos(theta) * spawnRadius;
			double z = center.z() + Math.sin(theta) * spawnRadius;

			player.teleportTo(this.level, x, this.map.getInnerBox().minY, z, Set.of(), yaw, 0, true);
		}

		this.singleplayer = this.players.size() == 1;

		for (ServerPlayer player : this.gameSpace.getPlayers().spectators()) {
			this.map.teleportToWaitingSpawn(player);
			this.setSpectator(player);
		}
	}

	private void addTrailBlock(Block block, BlockPos pos, int ticks, Map<Block, Long2IntMap> trailPositions) {
		trailPositions.putIfAbsent(block, new Long2IntOpenHashMap());

		Long2IntMap map = trailPositions.get(block);
		map.put(pos.asLong(), ticks);
	}

	private void addTrailBlocks(PlayerEntry player, BlockPos.MutableBlockPos pos, int ticks, int height, Map<Block, Long2IntMap> trailPositions) {
		Block trail = player.getTrail();

		Vec3 start = player.getPreviousPos();
		Vec3 end = player.getPos();

		double relativeX = end.x() - start.x();
		double relativeY = end.y() - start.y();
		double relativeZ = end.z() - start.z();

		for (int step = 1; step <= INTERPOLATION_STEPS; step++) {
			double progress = step / (double) INTERPOLATION_STEPS;

			pos.setX((int) (start.x() + relativeX * progress));
			pos.setY((int) (start.y() + relativeY * progress));
			pos.setZ((int) (start.z() + relativeZ * progress));

			for (int y = 0; y < height; y++) {
				this.addTrailBlock(trail, pos, ticks, trailPositions);
				pos.move(Direction.UP);
			}
		}
	}

	private void tick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		if (this.invulnerabilityTicks > 0) {
			this.invulnerabilityTicks -= 1;
		}
		if (this.invulnerabilityTicks == ELYTRA_OPEN_TICKS) {
			ClientboundSetTitlesAnimationPacket titleFadePacket = new ClientboundSetTitlesAnimationPacket(5, 60, 5);
			ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(Component.translatable("text.elytron.open_elytra").withStyle(ChatFormatting.BLUE));

			for (PlayerEntry player : this.players) {
				player.startGliding(titleFadePacket, titlePacket);
			}
		}
		
		BlockPos.MutableBlockPos trailPos = new BlockPos.MutableBlockPos();

		Map<Block, Long2IntMap> temporaryTrailPositions = new HashMap<>();;
		Iterator<Map.Entry<Block, Long2IntMap>> blockEntryIterator = this.trailPositions.entrySet().iterator();
		while (blockEntryIterator.hasNext()) {
			Map.Entry<Block, Long2IntMap> blockEntry = blockEntryIterator.next();
			BlockState state = blockEntry.getKey().defaultBlockState();

			ObjectIterator<Long2IntMap.Entry> iterator = Long2IntMaps.fastIterator(blockEntry.getValue());
			while (iterator.hasNext()) {
				Long2IntMap.Entry entry = iterator.next();
				long trailLongPos = entry.getLongKey();
				int ticksLeft = entry.getIntValue();

				if (ticksLeft == 0) {
					trailPos.set(trailLongPos);

					if (this.map.getInnerBox().contains(trailPos.getX(), trailPos.getY(), trailPos.getZ())) {
						this.level.setBlockAndUpdate(trailPos, state);

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
			ServerPlayer player = entry.getPlayer();

			if (!this.map.getInnerBox().inflate(0.5).contains(entry.getPos())) {
				this.eliminate(entry, "text.elytron.eliminated.out_of_bounds", false);
				playerIterator.remove();
			}

			trailPos.set(player.getX(), player.getY(), player.getZ());
			BlockState state = this.level.getBlockState(trailPos);
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

			this.ticksUntilClose = this.config.getTicksUntilClose().sample(this.level.getRandom());
		}
	}

	private Component getEndingMessage() {
		if (this.players.size() == 1) {
			return this.players.iterator().next().getWinText();
		}
		return Component.translatable("text.elytron.win.none").withStyle(ChatFormatting.GOLD);
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	private void setSpectator(ServerPlayer player) {
		player.setGameMode(GameType.SPECTATOR);
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.level, this.map.getWaitingSpawnPos()).thenRunForEach(player -> {
			this.setSpectator(player);
		});
	}

	private void removePlayer(ServerPlayer player) {
		PlayerEntry entry = this.getPlayerEntry(player);

		if (entry != null) {
			this.eliminate(entry, true);
		}
	}

	private void eliminate(PlayerEntry eliminatedPlayer, String reason, boolean remove) {
		if (this.isGameEnding()) return;
		if (!this.players.contains(eliminatedPlayer)) return;

		Component message = Component.translatable(reason, eliminatedPlayer.getDisplayName()).withStyle(ChatFormatting.RED);
		for (ServerPlayer player : this.gameSpace.getPlayers()) {
			player.sendSystemMessage(message, false);
		}

		if (remove) {
			this.players.remove(eliminatedPlayer);
		}
		this.setSpectator(eliminatedPlayer.getPlayer());
	}

	private void eliminate(PlayerEntry eliminatedPlayer, boolean remove) {
		this.eliminate(eliminatedPlayer, "text.elytron.eliminated", remove);
	}

	private EventResult onPlayerDamage(ServerPlayer player, DamageSource source, float amount) {
		PlayerEntry entry = this.getPlayerEntry(player);

		if (entry != null) {
			if (source.is(DamageTypes.FLY_INTO_WALL)) {
				if (this.map.getInnerInnerBox().contains(player.position())) {
					this.eliminate(entry, "text.elytron.eliminated.fly_into_trail", true);
				} else {
					this.eliminate(entry, "text.elytron.eliminated.fly_into_wall", true);
				}
			} else if (source.is(DamageTypeTags.IS_FALL)) {
				this.eliminate(entry, "text.elytron.eliminated.fall", true);
			}
		}

		return EventResult.DENY;
	}

	private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		PlayerEntry entry = this.getPlayerEntry(player);

		if (entry != null) {
			this.eliminate(entry, true);
		}

		return EventResult.ALLOW;
	}

	private InteractionResult onUseItem(ServerPlayer player, InteractionHand hand) {
		PlayerEntry.fillHotbarWithFireworkRockets(player);

		ItemStack handStack = player.getItemInHand(hand);
		if (handStack.getItem() == Items.FIREWORK_ROCKET && player.isFallFlying()) {
			handStack.grow(1);
		}
		
		return InteractionResult.PASS;
	}

	private PlayerEntry getPlayerEntry(ServerPlayer player) {
		for (PlayerEntry entry : this.players) {
			if (player == entry.getPlayer()) {
				return entry;
			}
		}

		return null;
	}
}
