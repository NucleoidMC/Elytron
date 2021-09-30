package io.github.haykam821.elytron.game.phase;

import io.github.haykam821.elytron.game.ElytronConfig;
import io.github.haykam821.elytron.game.map.ElytronMap;
import io.github.haykam821.elytron.game.map.ElytronMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ElytronWaitingPhase {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final ElytronMap map;
	private final ElytronConfig config;

	public ElytronWaitingPhase(GameSpace gameSpace, ServerWorld world, ElytronMap map, ElytronConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<ElytronConfig> context) {
		ElytronMapBuilder mapBuilder = new ElytronMapBuilder(context.config());
		ElytronMap map = mapBuilder.create();

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			ElytronWaitingPhase phase = new ElytronWaitingPhase(activity.getGameSpace(), world, map, context.config());

			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());
			ElytronActivePhase.setRules(activity);

			// Listeners
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private GameResult requestStart() {
		ElytronActivePhase.open(this.gameSpace, this.world, this.map, this.config);
		return GameResult.ok();
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getWaitingSpawnPos()).and(() -> {
			offer.player().changeGameMode(GameMode.ADVENTURE);
		});
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		this.spawnPlayer(player);
		return ActionResult.SUCCESS;
	}

	private void spawnPlayer(ServerPlayerEntity player) {
		Vec3d spawnPos = this.map.getWaitingSpawnPos();
		player.teleport(this.world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
	}
}
