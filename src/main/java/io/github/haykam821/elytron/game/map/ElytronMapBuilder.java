package io.github.haykam821.elytron.game.map;

import java.util.Random;

import io.github.haykam821.elytron.game.ElytronConfig;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

public record ElytronMapBuilder(ElytronConfig config) {

	public ElytronMap create() {
		MapTemplate template = MapTemplate.createEmpty();
		ElytronMapConfig mapConfig = this.config.mapConfig();

		BlockBounds bounds = BlockBounds.of(BlockPos.ORIGIN, new BlockPos(mapConfig.x(), mapConfig.y(), mapConfig.z()));
		this.build(bounds, template, mapConfig);

		return new ElytronMap(template, bounds);
	}

	private BlockState getBlockState(BlockPos pos, BlockBounds bounds, ElytronMapConfig mapConfig, Random random) {
		int layer = pos.getY() - bounds.min().getY();
		if (layer == 0) return mapConfig.floorProvider().getBlockState(random, pos);
		if (layer == bounds.max().getY()) return mapConfig.ceilingProvider().getBlockState(random, pos);

		if (pos.getX() == bounds.min().getX() || pos.getX() == bounds.max().getX() || pos.getZ() == bounds.min().getZ() || pos.getZ() == bounds.max().getZ()) {
			return mapConfig.wallProvider().getBlockState(random, pos);
		}
		return null;
	}

	public void build(BlockBounds bounds, MapTemplate template, ElytronMapConfig mapConfig) {
		Random random = new Random();
		for (BlockPos pos : bounds) {
			BlockState state = this.getBlockState(pos, bounds, mapConfig, random);
			if (state != null) {
				template.setBlockState(pos, state);
			}
		}
	}
}