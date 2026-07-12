package io.github.haykam821.elytron.game.map;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;

public class ElytronMap {
	private final AABB innerBox;
	private final AABB innerInnerBox;
	private final Vec3 waitingSpawnPos;
	private final ElytronMapConfig config;

	public ElytronMap(ElytronMapConfig config) {
		this.config = config;
		BlockBounds bounds = BlockBounds.of(BlockPos.ZERO, new BlockPos(config.getX(), config.getY(), config.getZ()));
		this.innerBox = bounds.asBox().inflate(-1, -1, -1);
		this.innerInnerBox = this.innerBox.inflate(-1, -1, -1);

		Vec3 center = this.innerBox.getCenter();
		this.waitingSpawnPos = new Vec3(center.x(), this.innerBox.minY, center.z());
	}

	public AABB getInnerBox() {
		return this.innerBox;
	}

	public AABB getInnerInnerBox() {
		return this.innerInnerBox;
	}

	public Vec3 getWaitingSpawnPos() {
		return this.waitingSpawnPos;
	}

	public void teleportToWaitingSpawn(ServerPlayer player) {
		Vec3 pos = this.getWaitingSpawnPos();
		player.teleportTo(player.level(), pos.x(), pos.y(), pos.z(), Set.of(), 0, 0, true);
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new ElytronMapGenerator(this.config, server);
	}
}