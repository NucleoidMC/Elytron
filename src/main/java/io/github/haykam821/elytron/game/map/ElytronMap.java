package io.github.haykam821.elytron.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Box;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class ElytronMap {
	private final MapTemplate template;
	private final Box innerBox;
	private final Box innerInnerBox;

	public ElytronMap(MapTemplate template, BlockBounds bounds) {
		this.template = template;
		this.innerBox = bounds.toBox().expand(-1, -1, -1);
		this.innerInnerBox = this.innerBox.expand(-1, -1, -1);
	}

	public Box getInnerBox() {
		return this.innerBox;
	}

	public Box getInnerInnerBox() {
		return this.innerInnerBox;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}