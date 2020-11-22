package io.github.haykam821.elytron.game.phase;

import io.github.haykam821.elytron.game.ElytronConfig;
import io.github.haykam821.elytron.game.map.ElytronMap;
import io.github.haykam821.elytron.game.map.ElytronMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.world.bubble.BubbleWorldConfig;

import java.util.concurrent.CompletableFuture;

public class ElytronWaitingPhase {
	private final GameWorld gameWorld;
	private final ElytronMap map;
	private final ElytronConfig config;

	public ElytronWaitingPhase(GameWorld gameWorld, ElytronMap map, ElytronConfig config) {
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static CompletableFuture<GameWorld> open(GameOpenContext<ElytronConfig> context) {
		ElytronMapBuilder mapBuilder = new ElytronMapBuilder(context.getConfig());

		return mapBuilder.create().thenCompose(map -> {
			BubbleWorldConfig worldConfig = new BubbleWorldConfig()
					.setGenerator(map.createGenerator(context.getServer())).setDefaultGameMode(GameMode.ADVENTURE);
			return context.openWorld(worldConfig).thenApply(gameWorld -> {
				ElytronWaitingPhase phase = new ElytronWaitingPhase(gameWorld, map, context.getConfig());

				return GameWaitingLobby.open(gameWorld, context.getConfig().getPlayerConfig(), game -> {
					ElytronActivePhase.setRules(game);

					// Listeners
					game.on(PlayerAddListener.EVENT, phase::addPlayer);
					game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
					game.on(RequestStartListener.EVENT, phase::requestStart);
				});
			});
		});
	}

	private StartResult requestStart() {
		ElytronActivePhase.open(this.gameWorld, this.map, this.config);
		return StartResult.OK;
	}

	private void addPlayer(ServerPlayerEntity player) {
		this.spawnPlayer(player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		this.spawnPlayer(player);
		return ActionResult.SUCCESS;
	}

	private void spawnPlayer(ServerPlayerEntity player) {
		Vec3d center = this.map.getInnerBox().getCenter();
		player.teleport(this.gameWorld.getWorld(), center.getX(), this.map.getInnerBox().minY, center.getZ(), 0, 0);
	}
}
