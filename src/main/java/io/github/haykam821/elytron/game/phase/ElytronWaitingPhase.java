package io.github.haykam821.elytron.game.phase;

import java.util.Set;

import io.github.haykam821.elytron.game.ElytronConfig;
import io.github.haykam821.elytron.game.map.ElytronMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ElytronWaitingPhase {
	private final GameSpace gameSpace;
	private final ServerLevel level;
	private final ElytronMap map;
	private final ElytronConfig config;

	public ElytronWaitingPhase(GameSpace gameSpace, ServerLevel level, ElytronMap map, ElytronConfig config) {
		this.gameSpace = gameSpace;
		this.level = level;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<ElytronConfig> context) {
		ElytronMap map = new ElytronMap(context.config().getMapConfig());

		RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithLevel(levelConfig, (activity, level) -> {
			ElytronWaitingPhase phase = new ElytronWaitingPhase(activity.getGameSpace(), level, map, context.config());

			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());
			ElytronActivePhase.setRules(activity);

			// Listeners
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private GameResult requestStart() {
		ElytronActivePhase.open(this.gameSpace, this.level, this.map, this.config);
		return GameResult.ok();
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.level, this.map.getWaitingSpawnPos()).thenRunForEach(player -> {
			player.setGameMode(GameType.ADVENTURE);
		});
	}

	private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		// Respawn player at the start
		this.spawnPlayer(player);
		return EventResult.ALLOW;
	}

	private void spawnPlayer(ServerPlayer player) {
		Vec3 spawnPos = this.map.getWaitingSpawnPos();
		player.teleportTo(this.level, spawnPos.x(), spawnPos.y(), spawnPos.z(), Set.of(), 0, 0, true);
	}
}
